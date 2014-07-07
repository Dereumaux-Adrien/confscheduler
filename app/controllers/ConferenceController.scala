package controllers

import com.github.nscala_time.time.Imports._
import scala.Some
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Controller, Result}

import models._
import helpers.DateTimeUtils
import DateTimeUtils.TimeString

import org.joda.time.DateTime
import MySecurity.Authentication.{MyAuthenticatedRequest, MyAuthenticated, ForcedAuthentication}
import MySecurity.Authorization.{AuthorizedWith, AuthorizedRequest}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ConferenceController extends Controller {
  case class SimpleConference(title: String, abstr: String, speakerId: Long, date: DateTime, length: String, organizerId: Long)

  val conferenceForm = Form {
    mapping(
      "title" -> nonEmptyText(1, 100),
      "abstract" -> nonEmptyText(1, 3000),
      "speaker" -> longNumber.verifying(Speaker.findById(_).isDefined),
      "Date" -> jodaDate,
      "length" -> text.verifying(_.isValidDuration),
      "organizer" -> longNumber.verifying(Lab.findById(_).isDefined))(SimpleConference.apply)(SimpleConference.unapply)
  }

  def authenticatedUserRole(implicit request: MyAuthenticatedRequest[AnyContent]): Option[UserRole] = request.user.map(_.role)
  def authorizedUserRole(implicit request: AuthorizedRequest[AnyContent]): UserRole = request.user.map(_.role).get
  def authorizedUser(implicit request: AuthorizedRequest[AnyContent]): User = request.user.get

  def listConfs = MyAuthenticated { implicit request =>
    Ok(views.html.confViews.index(models.Conference.findAccepted.filter(_.isInFuture).sortBy(_.startDate))(request, authenticatedUserRole.getOrElse(Guest)))
  }

  def calendar = MyAuthenticated { implicit request =>
    Ok(views.html.confViews.calendar(request, authenticatedUserRole.getOrElse(Guest)))
  }

  def viewConf(id: Long) = MyAuthenticated { implicit request =>
    models.Conference.find(id) match {
      case Some(c) => Ok(views.html.confViews.conf(c)(request, authenticatedUserRole.getOrElse(Guest)))
      case None    => NotFound
    }
  }

  def addConf() = ForcedAuthentication { implicit request =>
    Future(Ok(views.html.confViews.addConf(conferenceForm)(request, authenticatedUserRole.get)))
  }

  def create() = ForcedAuthentication { implicit request =>
    Future {
      conferenceForm.bindFromRequest.fold(
        formWithErrors => {formWithErrors.errors.foreach(println); BadRequest(views.html.confViews.addConf(formWithErrors)(request, authenticatedUserRole.get))},
        conf           => createConfWithUser(conf, request.user.get)
      )
    }
  }

  def accept(id: Long) = AuthorizedWith(_.canAllowConf(id)) { implicit request => Future {
    Conference.find(id).fold(
      Redirect(routes.ConferenceController.allowList()).flashing(("error", "You tried to allow an unknown conference"))
    )(
      c => {
        val accepted = c.asAccepted
        Redirect(routes.ConferenceController.viewConf(accepted.save.get.id)).flashing(("success", "Conference " + accepted.title + " successfully accepted"))
      }
    )
  }}

  def refuse(id: Long) = AuthorizedWith(_.canAllowConfs) { implicit request => Future {
    Conference.find(id).fold(
      Redirect(routes.ConferenceController.allowList()).flashing(("error", "You tried to refuse an unknown conference"))
    )(
      c => {
        c.destroy
        Redirect(routes.ConferenceController.listConfs()).flashing(("success", "Conference successfully refused"))
      }
    )
  }}

  def allowList = AuthorizedWith(_.canAllowConfs) { implicit request => Future {
    Ok(views.html.confViews.allowConfList(Conference.findConfsAllowableBy(authorizedUser))(request, authorizedUserRole))
  }}

  private def createConfWithUser(conf: SimpleConference, user: User): Result = {
    user.role match {
      case Administrator | Moderator =>
        val newId = Conference.saveNew(conf)
        Redirect(routes.ConferenceController.viewConf(newId))
      case _                         => NotImplemented

    }
  }
}
