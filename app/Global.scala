import components.ComponentRegistry
import play.GlobalSettings

class Global extends GlobalSettings {
  override def getControllerInstance[A](controllerClass: Class[A]): A =
   ComponentRegistry.getController(controllerClass)
}
