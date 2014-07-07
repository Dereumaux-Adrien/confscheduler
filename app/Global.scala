import models.{Speaker, Conference, Lab}
import play.api.Play.current
import play.api._

object Global extends GlobalSettings{
  override def onStart(app: Application) {
    if(current.mode == Mode.Dev) {
        Logger.info("Cleaning DB...")
        Conference.destroyAll()
        Speaker.destroyAll()
        Lab.destroyAll()
        Logger.info("... done.")

        Logger.info("Starting to seed DB...")
        Speaker.seedDB
        Lab.seedDB
        Conference.seedDB
        Logger.info("... done")

        Conference.listAll.foreach(c => Logger.info(c.toString))
    }
  }
}
