package controllers

import jp.t2v.lab.play2.auth.OptionalAuthElement
import com.github.nscala_time.time.Imports._
import scala.Some
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Result

import jp.t2v.lab.play2.auth.AuthElement

import models._
import helpers.DateTimeUtils
import DateTimeUtils.TimeString

import org.joda.time.DateTime
import jp.t2v.lab.play2.stackc.RequestWithAttributes


object ConferenceOpthAuthController extends Controller with OptionalAuthElement with AuthConfigImpl {
  def userRole(implicit request: RequestWithAttributes[AnyContent]): UserRole = if(loggedIn.isDefined) loggedIn.get.role else Guest

  def listConfs = StackAction { implicit request =>
    Ok(views.html.confViews.index(models.Conference.findAll.filter(_.isInFuture).sortBy(_.startDate))(request, userRole))
  }

  def calendar = StackAction { implicit request =>
    Ok(views.html.confViews.calendar(request, userRole))
  }

  def viewConf(id: Long) = StackAction { implicit request =>
    models.Conference.find(id) match {
      case Some(c) => Ok(views.html.confViews.conf(c)(request, userRole))
      case None    => NotFound
    }
  }
}

object ConferenceMandatoryAuthController extends Controller with AuthElement with AuthConfigImpl {
  val conferenceForm = Form {
    mapping(
      "title" -> nonEmptyText(1, 100),
      "abstract" -> nonEmptyText(1, 3000),
      "speaker" -> longNumber.verifying(Speaker.findById(_).isDefined),
      "Date" -> jodaDate,
      "length" -> text.verifying(_.isValidDuration))(SimpleConference.apply)(SimpleConference.unapply)
  }

  case class SimpleConference(title: String, abstr: String, speakerId: Long, date: DateTime, length: String)

  def addConf() = StackAction(AuthorityKey -> Contributor) { implicit request =>
    Ok(views.html.confViews.addConf(conferenceForm)(request, loggedIn.role))
  }

  def create() = StackAction(AuthorityKey -> Contributor) { implicit request =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => {println(formWithErrors.errors); BadRequest(views.html.confViews.addConf(formWithErrors)(request, loggedIn.role))},
      conf           => createConfWithUser(conf, loggedIn)
    )
  }

  private def createConfWithUser(conf: SimpleConference, user: User): Result = {
    user.role match {
      case Administrator | Moderator =>
        val newId = Conference.save(conf)
        Redirect(routes.ConferenceOpthAuthController.viewConf(newId))
      case _                         => NotImplemented

    }
  }
}
