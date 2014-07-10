import models.{User, Speaker, Conference, Lab}
import play.api.Play.current
import play.api._

object Global extends GlobalSettings{
  override def onStart(app: Application) {
    if(current.mode == Mode.Dev && (User.count == 0 || Conference.count == 0 || Speaker.count == 0 || Lab.count == 0)){
      initDB()
    }

    if(current.mode == Mode.Test || Play.application.configuration.getBoolean("heroku.test").getOrElse(false)) {
      initDB()
    }
  }

  def initDB(): Unit = {
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
  }
}
