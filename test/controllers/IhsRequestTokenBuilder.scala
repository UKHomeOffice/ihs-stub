package controllers

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

import com.nimbusds.jwt.SignedJWT
import model.{CustomClaimSet, StandardClaimSet, IhsRequestData}
import org.apache.commons.codec.binary.Base64._
import org.apache.commons.codec.digest.DigestUtils
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, Formats}
import services.{JWTBuilder, KeyRegistry}

class IhsRequestTokenBuilder(keyRegistry: KeyRegistry) {

  implicit def json4sFormats: Formats = DefaultFormats

  def build(ihsRequestData: IhsRequestData): SignedJWT = {
    val iv = buildInitialisationVector
    JWTBuilder(
      toBase64(DigestUtils.getSha1Digest.digest(keyRegistry.dcjPublicCertificate.getEncoded)),
      StandardClaimSet("DCJ", ihsRequestData.ApplicationData.VisaApplicationNumber),
      CustomClaimSet(Map(
        "ihs:vpd" -> toBase64(encrypt(write(ihsRequestData), iv, keyRegistry)),
        "ihs:iv" -> toBase64(iv),
        "ihs:act" -> "Apply"
      )))
  }

  private def encrypt(message: String, iv: Array[Byte], keyRegister: KeyRegistry) = {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keyRegister.symmetricKey, new IvParameterSpec(iv))
    cipher.doFinal(message.getBytes("UTF-8"))
  }

  private def buildInitialisationVector = {
    val ivBytes = new Array[Byte](16)
    SecureRandom.getInstance("SHA1PRNG").nextBytes(ivBytes)
    ivBytes
  }

  private def toBase64(toEncode: Array[Byte]) = new String(encodeBase64(toEncode, false, true), "UTF-8")
}
