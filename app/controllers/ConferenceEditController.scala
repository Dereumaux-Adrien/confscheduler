package controllers

import play.api.mvc._
import jp.t2v.lab.play2.auth.AuthElement
import play.api.data.Form
import play.api.data.Forms._
import models._
import play.api.mvc.Result
import java.util.Date

object ConferenceEditController extends Controller with AuthElement with AuthConfigImpl {
  val conferenceForm = Form {
    mapping("name" -> text, "abstract" -> text, "speaker" -> longNumber, "Date" -> date, "length" -> text)(SimpleConference.apply)(SimpleConference.unapply)
  }

  case class SimpleConference(title: String, abstr: String, speakerId: Long, date: Date, length: String)

  def addConf() = StackAction(AuthorityKey -> Contributor) { implicit request =>
      Ok(views.html.confViews.addConf(conferenceForm)(request, logged = true))
  }

  def create() = StackAction(AuthorityKey -> Contributor) { implicit request =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.confViews.addConf(conferenceForm)(request, logged = true)),
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