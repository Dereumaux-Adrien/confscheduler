package controllers

import jp.t2v.lab.play2.auth.AuthConfig
import models._

import scala.reflect.{ClassTag, classTag}
import scala.concurrent.{Future, ExecutionContext}

import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.Result

trait AuthConfigImpl extends AuthConfig {
  type Id        = Long
  type User      = models.User
  type Authority = UserRole

  val idTag: ClassTag[Id] = classTag[Id]

  val sessionTimeoutInSeconds: Int = 3600

  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = Future{User.findById(id)}

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Redirect(routes.ConferenceOpthAuthController.listConfs()))

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Redirect(routes.ConferenceOpthAuthController.listConfs()))

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Redirect(routes.LoginController.login()))

  def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Forbidden("no permission"))

  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    (user.role, authority) match {
        case (Administrator, _)         => true
        case (Moderator, Moderator)     => true
        case (Moderator, Contributor)   => true
        case (Contributor, Contributor) => true
        case _                          => false
    }
  }

  override lazy val cookieSecureOption: Boolean = play.api.Play.isProd(play.api.Play.current)
}
