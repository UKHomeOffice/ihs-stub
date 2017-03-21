package controllers

import java.io.{BufferedInputStream, File, FileInputStream, FileOutputStream}
import java.security.KeyStore
import java.security.KeyStore.{PasswordProtection, SecretKeyEntry}
import javax.crypto.spec.SecretKeySpec

import components.Configuration
import sun.misc.BASE64Decoder

class SymmetricKeyLoader(val keyStoreLocation: String = Configuration.ihsConfig.keyStoreLocation, val keyStorePassword: String = Configuration.ihsConfig.keyStorePassword) {

  def loadIntoKeyStore(keyName: String, key: String): Unit = {

    val symmetricKey = new SecretKeySpec(new BASE64Decoder().decodeBuffer(key), 0, 32, "AES")
    keyStore.setEntry(keyName, new SecretKeyEntry(symmetricKey), new PasswordProtection(keyStorePassword.toCharArray))
    keyStore.store(new FileOutputStream(keyStoreLocation), keyStorePassword.toCharArray)
  }

  private val keyStore =
    closeAfter(new BufferedInputStream(new FileInputStream(new File(getKeyStoreAbsolute)))) {
      resource =>
        val keyStore = KeyStore.getInstance("jceks")
        keyStore.load(resource, keyStorePassword.toCharArray)
        keyStore
    }

  private def getKeyStoreAbsolute =
    if (keyStoreLocation.startsWith("/"))
      keyStoreLocation
    else {
      val path = getClass.getResource("").getPath
      val base = path.take(path.indexOf("ihsstub") + 7)
      base + "/" + keyStoreLocation
    }

  private def closeAfter[A <: {def close() : Unit}, B](resource: A)(f: A => B): B =
    try {
      f(resource)
    } finally {
      resource.close()
    }
}