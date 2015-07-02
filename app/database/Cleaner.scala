package database

import akka.actor.{Props, Actor}
import models.{Location, Speaker, Conference}
import helpers.DateTimeUtils
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by adrien on 5/11/15.
 */

sealed trait CleanFrequency
case object TenYearPeriod extends CleanFrequency

case class Clean(freq : CleanFrequency)

class Cleaner extends Actor {
  override def receive = {
    case Clean(tenYearPeriod) => {
      Conference.destroyAfterTenYearPeriod();
      Speaker.destroyUnused()
      Location.destroyUnused()

      Akka.system.scheduler.scheduleOnce(DateTimeUtils.findNextYear(1), Akka.system.actorOf(Props[Cleaner]), TenYearPeriod)
    }
  }
}
