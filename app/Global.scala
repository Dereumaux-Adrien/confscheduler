import database.Cleaner
import java.io.File

import akka.actor.Props
import email._
import database._
import helpers.DateTimeUtils
import models._
import org.joda.time.{DateTimeConstants, DateTime}
import play.api.Play.current
import play.api._
import play.libs.Akka
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._

object Global extends GlobalSettings{
  def scheduleMailing() = {
    val delayBeforeNextMonday = DateTimeUtils.findNextDayOfWeek(DateTimeConstants.MONDAY)
    val delayBeforeNextMonth  = DateTimeUtils.findNextMonth(1)
    val delayBeforeNextYear = DateTimeUtils.findNextYear(1)

    val mailer = Akka.system.actorOf(Props[MailingListActor])
    Akka.system().scheduler.schedule(delayBeforeNextMonday, 7 days, mailer, Mailing(Weekly))
    Akka.system().scheduler.scheduleOnce(delayBeforeNextMonth, mailer, Mailing(Monthly))

    val cleaner = Akka.system.actorOf(Props[Cleaner])
    Akka.system().scheduler.scheduleOnce(delayBeforeNextYear, cleaner, TenYearPeriod)
  }

  override def onStart(app: Application) {
    createLogoDirectory()

    if(current.mode == Mode.Dev) {
      //testMailer
    }

    if(current.mode == Mode.Dev && (User.count == 0 || Conference.count == 0 || Speaker.count == 0 || Lab.count == 0 || Location.count == 0)){
      initDB()
    }

    if(current.mode == Mode.Test || Play.application.configuration.getBoolean("heroku.test").getOrElse(false)) {
      initDB(silent = true)
    } else {
      scheduleMailing()
    }
  }

  def testMailer() = {
    Logger.info("Starting the mailer")
    val mailer = Akka.system.actorOf(Props[Mailer])
    val user = User.findByEmail("rosa@gmail.com").get
    mailer ! SendMail(user.email, "Test subject",
      views.html.email.weeklyConfList.render(Conference.findVisibleByLabBetween(user.lab, DateTime.now, DateTime.now.plusDays(7)), "week").body)
  }

  def createLogoDirectory() = {
    val logoSaveDir = new File(Play.configuration.getString("application.imageSavePath").getOrElse(System.getenv("HOME") + "/.confscheduler/logos/"))
    if (!logoSaveDir.exists() && !logoSaveDir.mkdirs()) {
      throw new Error("Couldn't create/access the directory set to save logos: " + logoSaveDir.toURI)
    }
  }

  def initDB(silent: Boolean = false): Unit = {
    if(!silent) Logger.info("Cleaning DB...")
    User.destroyAll()
    Conference.destroyAll()
    Location.destroyAll()
    Speaker.destroyAll()
    Lab.destroyAll()
    if(!silent) Logger.info("... done.")

    if(!silent) Logger.info("Starting to seed DB...")
    Speaker.seedDB()
    Lab.seedDB()
    Location.seedDB()
    Conference.seedDB()
    User.seedDB()
    if(!silent) Logger.info("... done")
  }
}
