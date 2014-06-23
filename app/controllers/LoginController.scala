package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import models.{Guest, UserRole, User}

import jp.t2v.lab.play2.auth.LoginLogout
import scala.concurrent.Future
import jp.t2v.lab.play2.stackc.RequestWithAttributes

object LoginController extends Controller {
  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(User.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm)(request, Guest))
  }

  def logout = Action { implicit request =>
    Redirect(routes.Application.index()).withNewSession
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors)(request, Guest))),
      user           => doLogin(user.get)
    )
  }

  def doLogin(user: User) = {
    val cookieName     = "UID"
    val userId         = user.email

    Future {
      Redirect(routes.Application.index()).withSession((cookieName, userId))
    }
  }
}
