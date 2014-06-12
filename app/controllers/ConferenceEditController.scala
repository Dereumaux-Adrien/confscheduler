package controllers

import play.api.mvc._
import jp.t2v.lab.play2.auth.AuthElement
import play.api.data.Form
import play.api.data.Forms._
import models.{User, Contributor, Conference}

object ConferenceEditController extends Controller with AuthElement with AuthConfigImpl {
  val conferenceForm = Form {
    mapping("name" -> text, "abstract" -> text, "speaker" -> longNumber, "Date" -> date, "length" -> text)(Conference.create)(Conference.toPublicTuple)
  }

  def addConf() = StackAction(AuthorityKey -> Contributor) { implicit request =>
      Ok(views.html.addConf(conferenceForm)(request, logged = true))
  }

  def create() = StackAction(AuthorityKey -> Contributor) { implicit request =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.addConf(conferenceForm)(request, logged = true)),
      conf           => createConfWithUser(conf, loggedIn)
    )
  }

  private def createConfWithUser(conf: Conference, user: User): Result = {
    Redirect(routes.ConferenceViewController.listConfs())
  }
}