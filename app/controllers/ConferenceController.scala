package controllers

import com.github.nscala_time.time.Imports._
import scala.Some
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Controller, Result}

import models._
import helpers.DateTimeUtils
import DateTimeUtils.TimeString

import org.joda.time.DateTime
import MySecurity.Authentication.{MyAuthenticatedRequest, MyAuthenticated}
import MySecurity.Authorization._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ConferenceController extends Controller {
  case class SimpleConference(title: String, abstr: String, speakerId: Long, date: DateTime, length: String)
  val conferenceForm = Form {
    mapping(
      "title" -> nonEmptyText(1, 100),
      "abstract" -> nonEmptyText(1, 3000),
      "speaker" -> longNumber.verifying(Speaker.findById(_).isDefined),
      "Date" -> jodaDate,
      "length" -> text.verifying(_.isValidDuration))(SimpleConference.apply)(SimpleConference.unapply)
  }

  def userRole(implicit request: MyAuthenticatedRequest[AnyContent]): UserRole = request.user.flatMap(u => Some(u.role)).getOrElse(Guest)
  def authorizedUserRole(implicit request: AuthorizedRequest[AnyContent]): UserRole = request.user.get.role

  def listConfs = MyAuthenticated { implicit request =>
    Ok(views.html.confViews.index(models.Conference.findAll.filter(_.isInFuture).sortBy(_.startDate))(request, userRole))
  }

  def calendar = MyAuthenticated { implicit request =>
    Ok(views.html.confViews.calendar(request, userRole))
  }

  def viewConf(id: Long) = MyAuthenticated { implicit request =>
    models.Conference.find(id) match {
      case Some(c) => Ok(views.html.confViews.conf(c)(request, userRole))
      case None    => NotFound
    }
  }

  def addConf() = AuthorizedWith(_ => true) { implicit request =>
    Future(Ok(views.html.confViews.addConf(conferenceForm)(request, authorizedUserRole)))
  }

  def create() = AuthorizedWith(_ => true) { implicit request =>
    Future {
      conferenceForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.confViews.addConf(formWithErrors)(request, authorizedUserRole)),
        conf           => createConfWithUser(conf, request.user.get)
      )
    }
  }

  private def createConfWithUser(conf: SimpleConference, user: User): Result = {
    user.role match {
      case Administrator | Moderator =>
        val newId = Conference.save(conf)
        Redirect(routes.ConferenceController.viewConf(newId))
      case _                         => NotImplemented

    }
  }
}
