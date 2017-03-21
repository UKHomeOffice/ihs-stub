package components

import com.typesafe.config.{ConfigFactory, Config}
import controllers.IhsController
import play.api.mvc.Controller
import services.{IhsRequestTokenParser, IhsResponseTokenGenerator, KeyRegistry}

object Configuration {

  val host = conf.getString("host")
  val dcjSuccessfulUrl = conf.getString("dcj.successful.url")
  val dcjFailureUrl = conf.getString("dcj.failure.url")
  val ihsConfig = IhsConfig(
    symmetricKey = conf.getString("ihs.symmetric.key"),
    symmetricKeyPassword = conf.getString("ihs.symmetric.password"),
    ihsPrivateKey = conf.getString("ihs.private.key"),
    ihsPrivateKeyPassword = conf.getString("ihs.private.password"),
    dcjPrivateKey = Some(conf.getString("ihs.private.key")).filter(!_.isEmpty),
    dcjPrivateKeyPassword = Some(conf.getString("ihs.private.password")).filter(!_.isEmpty),
    dcjPublicKey = conf.getString("dcj.public.key"),
    keyStoreLocation = conf.getString("ihs.keystore.location"),
    keyStorePassword = conf.getString("ihs.keystore.password")
  )

  private lazy val conf: Config = ConfigFactory.load()
}

case class IhsConfig(keyStoreLocation: String,
                     keyStorePassword: String,
                     symmetricKey: String,
                     symmetricKeyPassword: String,
                     dcjPrivateKey: Option[String],
                     dcjPrivateKeyPassword: Option[String],
                     dcjPublicKey: String,
                     ihsPrivateKey: String,
                     ihsPrivateKeyPassword: String)


object ComponentRegistry {

  import components.Configuration._

  val keyRegistry = new KeyRegistry(Configuration.ihsConfig)
  val responseTokenGenerator = new IhsResponseTokenGenerator(keyRegistry)
  val requestTokenParser = new IhsRequestTokenParser(keyRegistry)

  private val controllers = Map[Class[_], Controller](
    classOf[IhsController] -> new IhsController(responseTokenGenerator, requestTokenParser)
  )
  def getController[A](controllerClass: Class[A]): A = controllers(controllerClass).asInstanceOf[A]
}
