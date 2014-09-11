package controllers

import MySecurity.Authentication._
import MySecurity.Authorization._
import controllers.ConferenceController._
import models.{Administrator, Lab, Moderator, User}
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
        case _                         => Redirect(routes.Application.index()) //TODO: Display a meaningful error message when someone tries to access this page without the rights
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
          case _                                                 => Redirect(routes.Application.index()) //TODO: Actually display an error message here
        }
      )
    }
  }

  def list = AuthorizedWith(_.role == Administrator) {implicit request =>
    Future {
      Ok(views.html.userViews.list(User.listAll)(request, Administrator))
    }
  }

  def delete(id: Long) = AuthorizedWith(_.role == Administrator) {implicit request =>
    Future {
      User.findById(id).map(_.destroy)
      Redirect(routes.UserController.list())
    }
  }

  def createUser(newUser: SimpleUser): Result = {
    User.fromSimpleUser(newUser).get.save //TODO: Actually handle errors during User creation
    Redirect(routes.Application.index())
  }
}
