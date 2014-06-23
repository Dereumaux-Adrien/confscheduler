package MySecurity

import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.Result

import models.User
import scala.concurrent.Future
import controllers.routes
import play.api.cache.Cache
import play.api.Play.current

object Authentication {
  val unsuccessfulAuthRoute = routes.LoginController.login()

  case class MyAuthenticatedRequest[A](user: Option[User], request: Request[A]) extends WrappedRequest[A](request)

  object MyAuthenticated extends ActionBuilder[MyAuthenticatedRequest] with ActionTransformer[Request, MyAuthenticatedRequest]{
    def transform[A](request: Request[A]) = Future.successful {
      MyAuthenticatedRequest[A](request.session.get("UID").flatMap(Cache.getAs[User]), request)
    }
  }

  object ForceAuth extends ActionFilter[MyAuthenticatedRequest] {
    def filter[A](request: MyAuthenticatedRequest[A]) = Future.successful {
      if(request.user.isDefined) None else Some(Redirect(unsuccessfulAuthRoute))
    }
  }

  def ForcedAuthentication(block: (MyAuthenticatedRequest[AnyContent]) => Future[Result]) =
    (MyAuthenticated andThen ForceAuth).async(request => block(request))
}

