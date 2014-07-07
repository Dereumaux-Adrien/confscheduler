import models.{Lab, Speaker}
import play.api.Play.current
import play.api._

object Global extends GlobalSettings{
  override def onStart(app: Application) {
    if(current.mode == Mode.Dev) {
      Speaker.destroyAll()
      Speaker.seedDB

      Lab.destroyAll()
      Lab.seedDB
    }
  }
}
