package controllers

import MySecurity.Authentication._
import MySecurity.Authorization._
import controllers.ConferenceController._
import models.{Administrator, Lab, Moderator, User}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Controller, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object UserController extends Controller {
  case class SimpleUser(firstName: String, lastName: String, email: String, labId: Long, password: String, repeatPassword: String, newUserRole: String)

  val userForm = Form {
    mapping(
      "firstName" -> nonEmptyText(1, 100),
      "lastName" -> nonEmptyText(1, 3000),
      "email" -> email,
      "labId" -> longNumber.verifying(Lab.findById(_).isDefined),
      "password" -> nonEmptyText(8, 100),
      "repeatPassword" -> nonEmptyText(8, 100),
      "newUserRole" -> text.verifying(User.loggedUserRoleList.contains(_)))(SimpleUser.apply)(SimpleUser.unapply)
    .verifying("Password in the repeat field does not match", user => user.password == user.repeatPassword)
  }

  def newUser(role: Option[String]) = ForcedAuthentication { implicit request =>
    Future {
      request.user.get.role match {
        case Administrator | Moderator => Ok(views.html.userViews.newUser(userForm, request.user.get)(request, request.user.get.role))
        case _                         => Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You do not have the rights to add users"))
      }
    }
  }

  def create = ForcedAuthentication { implicit request =>
    Future {
      userForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.userViews.newUser(formWithErrors, request.user.get)(request, authenticatedUserRole.get)),
        newUser        => request.user.get.role match {
          case Administrator                                     => createUser(newUser)
          case Moderator if newUser.newUserRole == "Contributor" && newUser.labId == request.user.get.lab.id => createUser(newUser)
          case Moderator if newUser.newUserRole != "Contributor" || newUser.labId != request.user.get.lab.id =>
            Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You do not have the rights to add non-contributors users, or users in another lab"))
          case _                                                 =>
            Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You do not have the rights to add users"))
        }
      )
    }
  }

  def list(filter: Option[String]) = AuthorizedWith(_.role == Administrator) {implicit request =>
    Future {
      if(filter.isDefined) {
        Ok(views.html.userViews.list(User.filteredWith(filter.get))(request, Administrator))
      } else {
        Ok(views.html.userViews.list(User.listAll)(request, Administrator))
      }
    }
  }

  def delete(id: Long) = AuthorizedWith(_.role == Administrator) {implicit request =>
    Future {
      User.findById(id).map(_.destroy)
      Redirect(routes.UserController.list(None))
    }
  }

  def createUser(newUser: SimpleUser): Result = {
    User.fromSimpleUser(newUser).get.save match {
      case Some(u) => Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("success", "User " + u.firstName + " " + u.lastName + " successfully created."))
      case None    =>
        Logger.error("Failure to save a new user, please check that the DB has been set correctly.")
        Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("success", "There was an error during User creation. Please try again later"))
    }
  }
}
