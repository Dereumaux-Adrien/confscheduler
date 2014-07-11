package controllers

import MySecurity.Authentication._
import controllers.ConferenceController._
import models.{Administrator, Lab}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Controller, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LabController extends Controller {
  case class SimpleLab(acronym: String, name: String)

  val labForm = Form {
    mapping(
      "acronym" -> nonEmptyText(1, 100),
      "name" -> nonEmptyText(1, 254))(SimpleLab.apply)(SimpleLab.unapply)
  }

  def newLab = ForcedAuthentication { implicit request => Future {
    request.user.get.role match {
      case Administrator => Ok(views.html.labViews.newLab(labForm)(request, request.user.get.role))
      case _             => Redirect(routes.Application.index()) //TODO: Display a meaningful error message when someone tries to access this page without the rights
    }
  }}

  def create = ForcedAuthentication { implicit request => Future {
    labForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.labViews.newLab(formWithErrors)(request, authenticatedUserRole.get)),
      newLab         => request.user.get.role match {
        case Administrator                                     => createLab(newLab)
        case _                                                 => Redirect(routes.Application.index()) //TODO: Actually display an error message here
      }
    )
  }}

  def createLab(newLab: SimpleLab): Result = {
    Lab.fromSimpleLab(newLab).get.save //TODO: Actually handle errors during User creation
    Redirect(routes.Application.index())
  }
}
