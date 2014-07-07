import models.Speaker
import play.api.Play.current
import play.api._

object Global extends GlobalSettings{
  override def onStart(app: Application) {
    if(current.mode == Mode.Dev) {
      Speaker.destroyAll
      Speaker.feedDB
      Speaker.listAll.foreach(println)
    }
  }
}
