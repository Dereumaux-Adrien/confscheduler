package controllers

import MySecurity.Authentication._
import MySecurity.Authorization._
import controllers.ConferenceController._
import models._
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

  def list(filter: Option[String]) = AuthorizedWith(u => u.role == Administrator | u.role == Moderator) {implicit request => Future {
    (filter.isDefined, authorizedUser.role) match {
      case (true, Administrator) => Ok(views.html.userViews.list(User.filteredWith(filter.get))(request, Administrator))
      case (false, Administrator) => Ok(views.html.userViews.list(User.listAll)(request, Administrator))
        // In the following case, we're ok with doing the filtering here instead of the DB as the nb of contributors/lab is low, and it saves writing yet another method
      case (true, Moderator) =>
        Ok(views.html.userViews.list(User.findContributorsInLab(authorizedUser.lab)
          .filter(u => u.firstName.toLowerCase.contains(filter.get.toLowerCase) | u.lastName.toLowerCase.contains(filter.get.toLowerCase)))
          (request, Moderator))
      case (false, Moderator) => Ok(views.html.userViews.list(User.findContributorsInLab(authorizedUser.lab))(request, Moderator))
    }
  }}

  def delete(id: Long) = AuthorizedWith(u => u.role == Administrator | u.role == Moderator) {implicit request => Future {
    def destroyUser = {
      val toDestroy = User.findById(id)
      if(User.isLastAdmin(toDestroy)){
        toDestroy match {
          case Some(u) => u.destroy; Redirect(routes.UserController.list(None)).flashing(("success", "User" + u.firstName + " " + u.lastName + " deleted"))
          case None    =>
            Logger.error("Tried to delete user " + id + " which doesn't exist")
            Redirect(routes.UserController.list(None)).flashing(("error", "User with id " + id + " doesn't exist"))
        }
      }else{
        Logger.error("Tried to delete user " + id + " which is the last administrator")
        Redirect(routes.UserController.list(None)).flashing(("error", "User with id " + id + " is the last administrator and can't be deleted"))
      }
    }
    authorizedUser.role match {
      case Administrator => destroyUser
      case Moderator if User.findById(id).exists(u => u.lab.id == authorizedUser.lab.id && u.role == Contributor) => destroyUser
      case _            => Redirect(routes.UserController.list(None)).flashing(("error", "You do not have the rights to delete this user"))
    }
  }}

  def createUser(newUser: SimpleUser): Result = {
    User.fromSimpleUser(newUser).get.save match {
      case Some(u) => Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("success", "User " + u.firstName + " " + u.lastName + " successfully created."))
      case None    =>
        Logger.error("Failure to save a new user, please check that the DB has been set correctly.")
        Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("success", "There was an error during User creation. Please try again later"))
    }
  }
}
