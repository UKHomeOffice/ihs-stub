package services

import java.io.{File, FileInputStream, BufferedInputStream}
import java.security.{UnrecoverableKeyException, KeyStore}
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.util.UUID
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKey}

import com.nimbusds.jose.crypto.{RSASSASigner, RSASSAVerifier}
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{ReadOnlyJWTClaimsSet, JWTClaimsSet, SignedJWT}
import components.IhsConfig
import model.{StandardClaimSet, CustomClaimSet, IhsRequestData}

import scala.collection.JavaConversions._
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization._
import org.apache.commons.codec.binary.Base64._
import org.apache.commons.codec.digest.DigestUtils
import java.security.cert.Certificate

class IhsRequestTokenParser(keyRegistry: KeyRegistry) {

  implicit def json4sFormats: Formats = DefaultFormats

  def parse(token: String): IhsRequestData = {
    val claimsSet = verify(toJWT(token))
    val encodedIV = claimsSet.getCustomClaim("ihs:iv").asInstanceOf[String]
    val encodedEncryptedJson = claimsSet.getCustomClaim("ihs:vpd").asInstanceOf[String]
    val iv = decodeBase64(encodedIV)
    val encryptedJson = decodeBase64(encodedEncryptedJson)
    read[IhsRequestData](decrypt(iv, encryptedJson))
  }

  private def verify(token: SignedJWT): ReadOnlyJWTClaimsSet = {
    if (!token.verify(new RSASSAVerifier(keyRegistry.dcjPublicKey))) {
      throw new KeyVerificationException
    }
    if (token.getHeader.getKeyID != toBase64(DigestUtils.getSha1Digest.digest(keyRegistry.dcjPublicCertificate.getEncoded))) {
      throw new KeyVerificationException
    }
    token.getJWTClaimsSet
  }

  def toJWT(jwtString: String): SignedJWT = {
    SignedJWT.parse(jwtString)
  }

  private def decrypt(iv: Array[Byte], message: Array[Byte]): String = {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, keyRegistry.symmetricKey, new IvParameterSpec(iv))
    new String(cipher.doFinal(message), "UTF-8")
  }

  private def toBase64(toEncode: Array[Byte]) = new String(encodeBase64(toEncode, false, true), "UTF-8")
}

class IhsResponseTokenGenerator(keyRegistry: KeyRegistry) {

  def generate(visaApplicationNumber: String, ihsReferenceNumber: String): String =
    sign(build(visaApplicationNumber, ihsReferenceNumber))

  private def sign(token: SignedJWT): String = {
    token.sign(new RSASSASigner(keyRegistry.ihsPrivateKey))
    token.serialize()
  }

  private def build(visaApplicationNumber: String, ihsReferenceNumber: String): SignedJWT =
    JWTBuilder(
      kid = toBase64(DigestUtils.getSha1Digest.digest(keyRegistry.dcjPublicCertificate.getEncoded)),
      standardClaimSet = StandardClaimSet("IHS", visaApplicationNumber),
      customClaimSet = CustomClaimSet(Map(
        "ihs:ref" -> ihsReferenceNumber,
        "ihs:vai" -> visaApplicationNumber
    )))

  private def toBase64(toEncode: Array[Byte]) = new String(encodeBase64(toEncode, false, true), "UTF-8")
}

object JWTBuilder {

  def apply(kid: String, standardClaimSet: StandardClaimSet, customClaimSet: CustomClaimSet): SignedJWT = {
    val claimsSet = buildClaimsSet(standardClaimSet, customClaimSet)
    new SignedJWT(new JWSHeader(JWSAlgorithm.RS256, null, null, null, null, null, null, null, null, null, kid, null, null), claimsSet)
  }

  private def buildClaimsSet(standardClaimSet: StandardClaimSet, customClaimSet: CustomClaimSet) = {
    val claimsSet = new JWTClaimsSet()
    claimsSet.setIssuer(standardClaimSet.iss)
    claimsSet.setSubject(standardClaimSet.sub)
    claimsSet.setExpirationTime(standardClaimSet.exp)
    claimsSet.setNotBeforeTime(standardClaimSet.nbf)
    claimsSet.setIssueTime(standardClaimSet.iat)
    claimsSet.setJWTID(generateJTI)
    claimsSet.setCustomClaims(customClaimSet.claims)
    claimsSet
  }

  // TODO reduced to 15 characters due to IHS restrictions - remove once fixed on IHS
  private def generateJTI = UUID.randomUUID().toString.replace("-", "").take(15)
}

class KeyRegistry(config: IhsConfig) {

  private def getKeyStoreAbsolute =
    if (config.keyStoreLocation.startsWith("/"))
      config.keyStoreLocation
    else {
      val path = getClass.getResource("").getPath
      val base = path.take(path.indexOf("ihs-stub") + 8)
      base + "/" + config.keyStoreLocation
    }

  val keyStore =
    closeAfter(new BufferedInputStream(new FileInputStream(new File(getKeyStoreAbsolute)))) {
      resource =>
        val keyStore = KeyStore.getInstance("jceks")
        keyStore.load(resource, config.keyStorePassword.toCharArray)
        keyStore
    }

  val symmetricKey: SecretKey =
    new SecretKeySpec(getKey(config.symmetricKey, config.symmetricKeyPassword).getEncoded, "AES")

  val ihsPrivateKey: RSAPrivateKey =
    getKey(config.ihsPrivateKey, config.ihsPrivateKeyPassword).asInstanceOf[RSAPrivateKey]

  val dcjPrivateKey: Option[RSAPrivateKey] =
    config.dcjPrivateKey.map(key => getKey(key, config.dcjPrivateKeyPassword.get).asInstanceOf[RSAPrivateKey])

  val dcjPublicCertificate: Certificate =
    keyStore.getCertificate(config.dcjPublicKey)

  val dcjPublicKey: RSAPublicKey =
    dcjPublicCertificate.getPublicKey.asInstanceOf[RSAPublicKey]

  private def getKey(name: String, password: String) = {
    try {
      val key = keyStore.getKey(name, password.toCharArray)
      if (key == null)
        throw new KeyNotFoundException(name)
      key
    } catch {
      case _: UnrecoverableKeyException => throw new InvalidKeyPasswordException(name)
    }
  }

  private def closeAfter[A <: {def close() : Unit}, B](resource: A)(f: A => B): B =
    try {
      f(resource)
    } finally {
      resource.close()
    }
}

class InvalidKeyPasswordException(keyName: String) extends RuntimeException(s"Password for key [$keyName] is invalid")
class KeyNotFoundException(keyName: String) extends RuntimeException(s"Key [$keyName] not found in the key store")
class KeyVerificationException extends RuntimeException("Could not verify token")
