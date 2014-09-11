package controllers

import MySecurity.Authentication.{ForcedAuthentication, MyAuthenticated, MyAuthenticatedRequest}
import MySecurity.Authorization.{AuthorizedRequest, AuthorizedWith}
import com.github.nscala_time.time.Imports
import com.github.nscala_time.time.Imports._
import models._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.data.{FormError, Form}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContent, Controller, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ConferenceController extends Controller {
  val isoFormatter = ISODateTimeFormat.date()
  case class SimpleConference(title: String, abstr: String,
                              date: DateTime, timezoneOffset: Int, time:Duration, length: Duration,
                              organizerId: Long, speaker: SimpleSpeaker, location: SimpleLocation,
                              priv: Boolean)
  case class SimpleSpeaker(speakerId: Long, speakerTitle: Option[String], firstName: Option[String], lastName: Option[String],
                           email: Option[String], team: Option[String], organisation: Option[String])
  case class SimpleLocation(locationId: Long,  instituteName :  Option[String], buildingName : Option[String], roomDesignation: Option[String],
                            floor : Option[String], streetName : Option[String], streetNb : Option[Int], city : Option[String])
  case class ConferenceEvent(title: String, start: String, end: String, url: String, backgroundColor: String)

  class timeFormatter extends Formatter[Duration] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Imports.Duration] = {
      val parsed = data.get(key)
        .map(s => s.split(":").toList)
        .filter(_.size == 2)
        .map(_.map(_.toInt))

      parsed match {
        case Some(h::m::_) => Right(new Duration(h*3600*1000 + m*60*1000))
        case _             => Left(List(FormError(key, "Error while parsing time")))
      }
    }

    override def unbind(key: String, value: Imports.Duration): Map[String, String] = {
      Map((key, value.getStandardHours + ":" + value.getStandardMinutes))
    }
  }

  val conferenceForm = Form {
    mapping(
      "title"          -> nonEmptyText(1, 100),
      "abstract"       -> nonEmptyText(1, 3000),
      "date"           -> jodaDate("yy-MM-dd"),
      "timezoneOffset" -> number,
      "time"           -> of[Duration](new timeFormatter),
      "length"         -> of[Duration](new timeFormatter),
      "organizer"      -> longNumber.verifying(Lab.findById(_).isDefined),
      "speaker"        -> mapping (
          "id"           -> longNumber.verifying(id => Speaker.findById(id).isDefined || id == -1),
          "speakerTitle" -> optional(text),
          "firstName"    -> optional(nonEmptyText(1, 254)),
          "lastName"     -> optional(nonEmptyText(1, 254)),
          "email"        -> optional(email),
          "team"         -> optional(nonEmptyText(1, 254)),
          "organisation" -> optional(nonEmptyText(1, 254))
      )(SimpleSpeaker.apply)(SimpleSpeaker.unapply).verifying("Please check that all fields in the new speaker form have been filled",
          c => c.speakerId != -1 
            || List(c.speakerTitle, c.firstName, c.lastName, c.email, c.team, c.organisation).forall(_.isDefined)
       ),
      "location" -> mapping(
        "id"              -> longNumber.verifying(id => Location.findById(id).isDefined || id == -1),
        "instituteName"   -> optional(nonEmptyText(1, 250)),
        "buildingName"    -> optional(text),
        "roomDesignation" -> optional(nonEmptyText(1, 250)),
        "floor"           -> optional(nonEmptyText(1, 250)),
        "streetName"      -> optional(nonEmptyText(1, 250)),
        "streetNb"        -> optional(number),
        "city"            -> optional(nonEmptyText(1,250))
      )(SimpleLocation.apply)(SimpleLocation.unapply).verifying("Please check that all fields in the new location form have been filled (only the building name is optional)",
        l => l.locationId != -1 
        || List(l.instituteName, l.roomDesignation, l.floor, l.streetName, l.streetNb, l.city).forall(_.isDefined)),
      "private" -> boolean)(SimpleConference.apply)(SimpleConference.unapply)
  }

  implicit val conferenceEventWrites: Writes[ConferenceEvent] = (
    (JsPath \ "title").write[String] and
    (JsPath \ "start").write[String] and
    (JsPath \ "end").write[String] and
    (JsPath \ "url").write[String] and
    (JsPath \ "backgroundColor").write[String]
  )(unlift(ConferenceEvent.unapply))

  def authenticatedUserRole(implicit request: MyAuthenticatedRequest[AnyContent]): Option[UserRole] = request.user.map(_.role)
  def authenticatedUser(implicit request: MyAuthenticatedRequest[AnyContent]): Option[User] = request.user
  def authorizedUserRole(implicit request: AuthorizedRequest[AnyContent]): UserRole = request.user.map(_.role).get
  def authorizedUser(implicit request: AuthorizedRequest[AnyContent]): User = request.user.get

  def listUpcomingConfs = MyAuthenticated { implicit request =>
    val confWithEditRights =
      Conference.findAccepted(authenticatedUser).filter(_.isInFuture).sortBy(_.startDate)
        .map(c => (c, request.user.exists(_.canEdit(c.id))))

    Ok(views.html.confViews.list(confWithEditRights)(request, authenticatedUserRole.getOrElse(Guest)))
  }

  def listConfs = MyAuthenticated { implicit request =>
    val confWithEditRights =
      Conference.findAccepted(authenticatedUser).sortBy(_.startDate).reverse
        .map(c => (c, request.user.exists(_.canEdit(c.id))))

    Ok(views.html.confViews.list(confWithEditRights)(request, authenticatedUserRole.getOrElse(Guest)))
  }

  def listConfEvents(start: String, end: String) = MyAuthenticated { implicit request =>
    Ok(Json.toJson(Conference.between(isoFormatter.parseDateTime(start), isoFormatter.parseDateTime(end))
                             .filter(_.isAccessibleBy(authenticatedUser))
                             .map(_.toConfEvent)))
  }

  def calendar = MyAuthenticated { implicit request =>
    Ok(views.html.confViews.calendar(request, authenticatedUserRole.getOrElse(Guest)))
  }

  def viewConf(id: Long) = MyAuthenticated { implicit request =>
    models.Conference.findById(id) match {
      case Some(c) if c.isAccessibleBy(authenticatedUser) => Ok(views.html.confViews.conf(c, request.user.exists(_.canEdit(id)))(request, authenticatedUserRole.getOrElse(Guest)))
      case _    => NotFound
    }
  }

  def addConf() = ForcedAuthentication { implicit request =>
    Future(Ok(views.html.confViews.addConf(conferenceForm, Lab.listVisible(request.user.get))(request, authenticatedUserRole.get)))
  }

  def create() = ForcedAuthentication { implicit request =>
    Future {
      conferenceForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.confViews.addConf(formWithErrors, Lab.listVisible(request.user.get))(request, authenticatedUserRole.get)),
        conf           => createConfWithUser(conf, request.user.get)
      )
    }
  }

  def accept(id: Long) = AuthorizedWith(_.canAllowConf(id)) { implicit request => Future {
    Conference.findById(id).fold(
      Redirect(routes.ConferenceController.allowList()).flashing(("error", "You tried to allow an unknown conference"))
    )(
      c => {
        val accepted = c.asAccepted
        Redirect(routes.ConferenceController.viewConf(accepted.save.get.id)).flashing(("success", "Conference " + accepted.title + " successfully accepted"))
      }
    )
  }}

  def refuse(id: Long) = AuthorizedWith(_.canAllowConfs) { implicit request => Future {
    Conference.findById(id).fold(
      Redirect(routes.ConferenceController.allowList()).flashing(("error", "You tried to refuse an unknown conference"))
    )(
      c => {
        c.destroy
        Redirect(routes.ConferenceController.listUpcomingConfs()).flashing(("success", "Conference successfully refused"))
      }
    )
  }}

  def delete(id: Long) = AuthorizedWith(_.canAllowConfs) { implicit request => Future {
    Conference.findById(id).fold(
      Redirect(routes.ConferenceController.allowList()).flashing(("error", "You tried to delete an unknown conference"))
    )(
        c => {
          c.destroy
          Redirect(routes.ConferenceController.listUpcomingConfs()).flashing(("success", "Conference successfully deleted"))
        }
      )
  }}

  def allowList = AuthorizedWith(_.canAllowConfs) { implicit request => Future {
    Ok(views.html.confViews.allowConfList(Conference.findConfsAllowableBy(authorizedUser))(request, authorizedUserRole))
  }}

  private def createConfWithUser(conf: SimpleConference, user: User): Result = {
    user.role match {
      case Administrator | Moderator =>
        val newId = Conference.fromSimpleConference(conf).asAccepted.save.get.id
        Redirect(routes.ConferenceController.viewConf(newId))
      case _                         => NotImplemented

    }
  }
}
