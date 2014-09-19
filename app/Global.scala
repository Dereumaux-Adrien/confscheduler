import java.io.File

import akka.actor.Props
import email.{SendMail, Mailer}
import models._
import org.joda.time.DateTime
import play.api.Play.current
import play.api._
import play.libs.Akka

object Global extends GlobalSettings{
  override def onStart(app: Application) {
    val logoSaveDir = new File(Play.configuration.getString("application.imageSavePath").getOrElse(System.getenv("HOME") + "/.confscheduler/logos/"))
    if(!logoSaveDir.exists() && !logoSaveDir.mkdirs()) {
      throw new Error("Couldn't create/access the directory set to save logos: " + logoSaveDir.toURI)
    }

    if(current.mode == Mode.Dev) {
      Logger.info("Starting the mailer")
      val mailer = Akka.system.actorOf(Props[Mailer])
      val user = User.findByEmail("rosa@gmail.com").get
      mailer ! SendMail(user.email, "Test subject",
        views.html.email.weeklyConfList.render(Conference.findVisibleByLabBetween(user.lab, DateTime.now, DateTime.now.plusDays(7)), "week").body)
    }

    if(current.mode == Mode.Dev && (User.count == 0 || Conference.count == 0 || Speaker.count == 0 || Lab.count == 0 || Location.count == 0)){
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
    Location.destroyAll()
    Speaker.destroyAll()
    Lab.destroyAll()
    Logger.info("... done.")

    Logger.info("Starting to seed DB...")
    Speaker.seedDB()
    Lab.seedDB()
    Location.seedDB()
    Conference.seedDB()
    User.seedDB()
    Logger.info("... done")
  }
}
