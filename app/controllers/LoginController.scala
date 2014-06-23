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
  val rememberMeCookieName     = MySecurity.Authentication.rememberMeCookieName
  val rememberMeCookieLifetime = Some(3600 * 48)
  val rememberMeCookiePath     = "/"
  val rememberMeCookieDomain   = None
  val rememberMeCookieHttpOnly = true
  val secureCookie             = play.api.Play.isProd(play.api.Play.current)

  val loginForm = Form {
    mapping("email" -> email, "password" -> text, "rememberme" -> boolean)(User.authenticate)(_.map(u => (u.email, "", false)))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm)(request, Guest))
  }

  def logout = Action { implicit request =>
    val removeRememberMe = DiscardingCookie(rememberMeCookieName, rememberMeCookiePath, rememberMeCookieDomain, secureCookie)
    Redirect(routes.Application.index()).withNewSession.discardingCookies(removeRememberMe)
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors)(request, Guest))),
      user           => doLogin(user.get)
    )
  }

  def doLogin(user: User) = {
    Future {
      val sessionName     = "UID"
      val userId          = MySecurity.SecurityHelper.UIDGenerator

      // We don't need to check if the UID is already defined for some other user in cache, since the
      // UID is a 40 chars random string, making the probabilty of collision under 1/10^70.
      Cache.set(userId, user)

      if(user.rememberMeToken.isEmpty) {
        Redirect(routes.ConferenceController.listConfs()).withSession((sessionName, userId))
      } else {
        val cToken = user.rememberMeToken
        val rememeberMeCookie = Cookie(rememberMeCookieName, cToken, rememberMeCookieLifetime, rememberMeCookiePath, rememberMeCookieDomain, secureCookie, rememberMeCookieHttpOnly)

        Redirect(routes.ConferenceController.listConfs()).withSession((sessionName, userId)).withCookies(rememeberMeCookie)
      }
    }
  }
}
