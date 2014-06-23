package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import models.{Guest, User}

import scala.concurrent.Future
import play.api.cache.Cache
import play.api.Play.current

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
    val userId         = MySecurity.SecurityHelper.UIDGenerator

    // We don't need to check if the UID is already defined for some other user in cache, since the
    // UID is a 40 chars random string, making the probabilty of collision under 1/10^70.
    Cache.set(userId, user)

    Future {
      Redirect(routes.ConferenceController.listConfs()).withSession((cookieName, userId))
    }
  }
}
