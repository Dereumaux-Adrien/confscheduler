package email

import akka.actor.{Props, Actor}
import helpers.DateTimeUtils
import models.{Lab, Conference, User}
import org.joda.time.DateTime
import play.api.libs.concurrent.Akka

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

sealed trait EmailFrequency
case object Weekly extends EmailFrequency
case object Monthly extends EmailFrequency

case class Mailing(freq: EmailFrequency)

class MailingListActor extends Actor {
  val mailer = Akka.system.actorOf(Props[Mailer])

  override def receive = {
    case Mailing(Weekly) => Lab.listAll.foreach(lab => mailer ! SendMail(lab.email, "[Bio-Hebdo] Weekly Conferences Reminder",
        views.html.email.weeklyConfList.render(Conference.findVisibleByLabBetween(lab, DateTime.now, DateTime.now.plusDays(7)), "week").body)
    )
    case Mailing(Monthly) => {
      Lab.listAll.foreach(lab => mailer ! SendMail(lab.email, "[Bio-Conf] Monthly Conferences Reminder",
        views.html.email.weeklyConfList.render(Conference.findVisibleByLabBetween(lab, DateTime.now, DateTime.now.plusMonths(1)), "month").body)
      )
      Akka.system.scheduler.scheduleOnce(DateTimeUtils.findNextMonth(1), Akka.system.actorOf(Props[MailingListActor]), Mailing(Monthly))
    }
  }
}
