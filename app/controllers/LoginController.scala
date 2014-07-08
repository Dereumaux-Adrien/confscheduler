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
import play.api.libs.Crypto

object LoginController extends Controller {
  val rememberMeCookieName     = "ConfSched_REMEMBERME"
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
    Redirect(routes.ConferenceController.listUpcomingConfs()).withNewSession.discardingCookies(removeRememberMe).flashing(("success", "Logout successful"))
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.login(formWithErrors)(request, Guest))),
      user           => doLogin(user.get)
    )
  }

  def doLogin(user: User) = {
    Future {
      val sessionName     = "UID"
      val userId          = Crypto.generateToken

      Cache.set(userId, user)

      if(user.rememberMeToken.isEmpty) {
        Redirect(routes.ConferenceController.listUpcomingConfs()).withSession((sessionName, userId)).flashing(("success", "Logged in as " + user.firstName + " " + user.lastName))
      } else {
        val cToken = user.rememberMeToken
        val rememberMeCookie = Cookie(rememberMeCookieName, cToken, rememberMeCookieLifetime, rememberMeCookiePath, rememberMeCookieDomain, secureCookie, rememberMeCookieHttpOnly)

        Redirect(routes.ConferenceController.listUpcomingConfs()).withSession((sessionName, userId)).withCookies(rememberMeCookie).flashing(("success", "Logged in as " + user.firstName + " " + user.lastName))
      }
    }
  }
}
