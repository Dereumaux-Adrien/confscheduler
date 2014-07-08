import models.{User, Speaker, Conference, Lab}
import play.api.Play.current
import play.api._

object Global extends GlobalSettings{
  override def onStart(app: Application) {
    if(current.mode == Mode.Dev || current.mode == Mode.Test) {
        Logger.info("Cleaning DB...")
        User.destroyAll()
        Conference.destroyAll()
        Speaker.destroyAll()
        Lab.destroyAll()
        Logger.info("... done.")

        Logger.info("Starting to seed DB...")
        Speaker.seedDB()
        Lab.seedDB()
        Conference.seedDB()
        User.seedDB()
        Logger.info("... done")

        Conference.listAll.foreach(c => Logger.info(c.toString))
    }
  }
}
