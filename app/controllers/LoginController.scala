package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import models.LoggedUser

import jp.t2v.lab.play2.auth.LoginLogout
import scala.concurrent.Future

object LoginController extends Controller with LoginLogout with AuthConfigImpl {
  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(LoggedUser.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm)(request, logged = false))
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded.map(_.flashing(
      "success" -> "You've been logged out"
    ))
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors)(request, logged = false))),
      user => gotoLoginSucceeded(user.get.id)
    )
  }
}
