package MySecurity

import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.Result

import models.User
import scala.concurrent.Future
import controllers.{LoginController, routes}
import play.api.cache.Cache
import play.api.Play.current

object Authentication {
  val rememberMeCookieName  = LoginController.rememberMeCookieName
  val unsuccessfulAuthRoute = routes.LoginController.login()

  case class MyAuthenticatedRequest[A](user: Option[User], request: Request[A]) extends WrappedRequest[A](request)

  object MyAuthenticated extends ActionBuilder[MyAuthenticatedRequest] with ActionTransformer[Request, MyAuthenticatedRequest]{
    def transform[A](request: Request[A]) = Future.successful {
      val maybeUser = authBySession(request) orElse authByRememberMe(request)
      MyAuthenticatedRequest[A](maybeUser, request)
    }
  }

  object ForceAuth extends ActionFilter[MyAuthenticatedRequest] {
    def filter[A](request: MyAuthenticatedRequest[A]) = Future.successful {
      if(request.method == "POST") {
        if(request.user.isDefined) None else Some(Forbidden)
      } else {
        if(request.user.isDefined) None else Some(Redirect(unsuccessfulAuthRoute))
      }
    }
  }

  def ForcedAuthentication(block: (MyAuthenticatedRequest[AnyContent]) => Future[Result]) =
    (MyAuthenticated andThen ForceAuth).async(request => block(request))

  def authBySession(request: Request[Any]): Option[User] = request.session.get("UID").flatMap(Cache.getAs[User])

  def authByRememberMe(request: Request[Any]): Option[User] = User.findByRememberMe(request.cookies.get(rememberMeCookieName))
}

