package controllers

import play.api.mvc.{Action, Controller}
import play.api.data._
import play.api.data.Forms._

import models.LoggedUser

object LoginController extends Controller{
  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(LoggedUser.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }
}
