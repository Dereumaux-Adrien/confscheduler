package controllers

import MySecurity.Authentication._
import models.Guest
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Redirect(routes.ConferenceController.listUpcomingConfs(None))
  }

  def legals = MyAuthenticated { implicit request =>
    Ok(views.html.legals(request, request.user.map(_.role).getOrElse(Guest)))
  }
}