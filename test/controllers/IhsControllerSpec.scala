package controllers

import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.SignedJWT
import components.IhsConfig
import model.{ApplicationData, IhsRequestData, LeadApplicant}
import org.scalatest.{Matchers, WordSpec}
import play.api.test.Helpers._
import play.api.test._
import services.{IhsRequestTokenParser, IhsResponseTokenGenerator, KeyRegistry}

import scala.concurrent.duration._

class IhsControllerSpec extends WordSpec with Matchers {

  val keyRegistry = new KeyRegistry(IhsConfig(
    keyStoreLocation = "ops/keystore/unitTestKeystore.jks",
    keyStorePassword = "password",
    symmetricKey = "ihs-symmetric-key",
    symmetricKeyPassword = "password",
    dcjPrivateKey = Some("dcj-key"),
    dcjPrivateKeyPassword = Some("password"),
    dcjPublicKey = "dcj-key",
    ihsPrivateKey = "ihs-private-key",
    ihsPrivateKeyPassword = "password"
  ))

  val responseTokenGenerator = new IhsResponseTokenGenerator(keyRegistry)
  val requestTokenParser = new IhsRequestTokenParser(keyRegistry)
  def newController = new IhsController(responseTokenGenerator, requestTokenParser)

  "complete" should {

    val leadApplicant = LeadApplicant(Some("Mr"), Some("John"), Some("McEnroe"), None, Some("US"), Some("T2 General"), Some("P123124"), Some("1950-01-04"),None)
    val ihsRequestData = IhsRequestData(ApplicationData("VAN1", true, Some("Beijing VAC"), Some("a@a.com"), leadApplicant, None, List()))
    val token = sign(new IhsRequestTokenBuilder(keyRegistry).build(ihsRequestData))


    "render ihs form with decrypted fields populated" in new WithApplication {
      val request = FakeRequest().withFormUrlEncodedBody(("visa_token", token))
      val controller = newController
      status(controller.complete()(request))(1 seconds) should equal (OK)
      contentAsString(controller.complete()(request))(1 seconds) should include("ihs_token")
    }

    "redirect to dcjFailureUrl when no form fields entered" in new WithApplication {
      val request = FakeRequest()
      val controller = newController
      status(controller.complete()(request))(1 seconds) should equal (FOUND)
      redirectLocation(controller.complete()(request))(1 seconds) should be(Some("failureURL"))
    }

    "redirect to dcjFailureUrl when invalid form field passed" in new WithApplication {
      val request = FakeRequest().withFormUrlEncodedBody(("invalid_token_name", "any val"))
      val controller = newController
      status(controller.complete()(request))(1 seconds) should equal (FOUND)
      redirectLocation(controller.complete()(request))(1 seconds) should be(Some("failureURL"))
    }

  }

  private def sign(token: SignedJWT): String = {
    token.sign(new RSASSASigner(keyRegistry.dcjPrivateKey.get))
    token.serialize()
  }

}
