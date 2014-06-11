package controllers

import play.api._
import play.api.mvc._
import com.github.nscala_time.time.Imports._

abstract class CalendarPeriod
case class Weekly() extends CalendarPeriod
case class Monthly() extends CalendarPeriod


object ConferenceController extends Controller {
  def addConf = Action {
    Ok(views.html.addConf())
  }

  def viewConf(id: Long) = Action {
    models.Conference.find(id) match {
        case Some(c) => Ok(views.html.conf(c))
        case None    => NotFound
    }
  }

  def listConfs = Action {
    Ok(views.html.index(models.Conference.findAll.sortBy(_.startDate)))
  }

  def calendar(period: CalendarPeriod) = Action {
    Ok(views.html.weeklyCalendar())
  }

  def weeklyCalendar = calendar(Weekly())

  def monthlyCalendar = calendar(Monthly())
}