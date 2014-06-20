package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Result

import jp.t2v.lab.play2.auth.AuthElement

import models._
import helpers.DateTimeUtils
import DateTimeUtils.TimeString

import org.joda.time.DateTime


object ConferenceEditController extends Controller with AuthElement with AuthConfigImpl {
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
      Ok(views.html.confViews.addConf(conferenceForm)(request, logged = true))
  }

  def create() = StackAction(AuthorityKey -> Contributor) { implicit request =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => {println(formWithErrors.errors); BadRequest(views.html.confViews.addConf(formWithErrors)(request, logged = true))},
      conf           => createConfWithUser(conf, loggedIn)
    )
  }

  private def createConfWithUser(conf: SimpleConference, user: User): Result = {
    user.role match {
      case Administrator | Moderator =>
        val newId = Conference.save(conf)
        Redirect(routes.ConferenceViewController.viewConf(newId))
      case _                         => NotImplemented

    }
  }
}