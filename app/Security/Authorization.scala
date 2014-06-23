package Security

import controllers.routes
import scala.concurrent.Future
import play.api.mvc.Results._
import models.User
import Security.Authentication.MyAuthenticated
import Security.Authentication.MyAuthenticatedRequest
import Security.Authentication.ForceAuth
import play.api.mvc._
import scala.Some

object Authorization {
  val unsuccessfulAuthorizationRoute = routes.Application.index()

  case class AuthorizedRequest[A](f: User => Boolean, request: MyAuthenticatedRequest[A]) extends WrappedRequest[A](request) {
    def user = request.user
  }

  def Authorize(f: User => Boolean) = new ActionRefiner[MyAuthenticatedRequest, AuthorizedRequest] {
    def refine[A](input: MyAuthenticatedRequest[A]) = Future.successful {
      Right(AuthorizedRequest(f, input))
    }
  }

  object Authorized extends ActionFilter[AuthorizedRequest] {
    def filter[A](request: AuthorizedRequest[A]) = Future.successful {
      if(request.user.isDefined && request.f(request.user.get)) {
        None
      } else {
        Some(Redirect(unsuccessfulAuthorizationRoute))
      }
    }
  }

  def AuthorizedWith(f: User => Boolean)(block: AuthorizedRequest[AnyContent] => Future[Result]) =
    (MyAuthenticated andThen ForceAuth andThen Authorize(f) andThen Authorized).async(request => block(request))
}
