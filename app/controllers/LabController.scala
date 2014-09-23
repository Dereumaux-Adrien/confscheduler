package controllers

import java.io.File

import MySecurity.Authentication._
import MySecurity.Authorization._
import logo.Logo
import models.{Administrator, Lab}
import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object LabController extends Controller {
  case class SimpleLab(acronym: String, name: String)

  val labForm = Form {
    mapping(
      "acronym" -> nonEmptyText(1, 100),
      "name" -> nonEmptyText(1, 254))(SimpleLab.apply)(SimpleLab.unapply)
  }

  def logo(id: Long) = ForcedAuthentication{implicit request => Future {
      Lab.findById(id).flatMap(_.logoId) match {
        case Some(logoId) => Ok.sendFile(Logo.find(logoId))
        case _            => Redirect(routes.LabController.list(None)).flashing(("error", "Couldn't find the logo for this lab"))
      }
  }}

  def newLab: Action[AnyContent] = AuthorizedWith(_.role == Administrator) { implicit request => Future {
      Ok(views.html.labViews.newLab(labForm)(request, request.user.get.role))
  }}

  def create = MyAuthenticated(parse.multipartFormData) { implicit request => {
    request.user.map(_.role) match {
      case Some(Administrator) => createLab
      case _                   => Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You do not have the rights to create a new lab."))
    }
  }}

  def successfullAddition(labName: String) = Redirect(routes.LabController.list(None)).flashing(("success", "Successfully created new lab: " + labName))

  def AddNewLabWithLogo(form: Form[SimpleLab], logo: Logo)(implicit request: Request[Any]) = {
    labForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.labViews.newLab(formWithErrors)(request, Administrator)),
      newLab         => {
        val saving = logo.save()
        val lab   = Lab.fromSimpleLab(newLab, Some(logo.logoId)).get
        val LabId = lab.save().get
        Await.result(saving, 1 second)
        successfullAddition(lab.name)
      }
    )
  }

  def LogoInvalid(form: Form[SimpleLab])(implicit request: Request[Any]) = {
    val formWithErrors = labForm.bindFromRequest.withError("fileError", "The logo must be a JPEG or a PNG under 4Mb in size")
    BadRequest(views.html.labViews.newLab(formWithErrors)(request, Administrator))
  }

  def AddNewLabWithoutLogo(form: Form[SimpleLab])(implicit request: Request[Any]) = {
    labForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.labViews.newLab(formWithErrors)(request, Administrator)),
      newLab         => {
        val lab = Lab.fromSimpleLab(newLab, None).get
        val id = lab.save().get
        successfullAddition(lab.name)
      }
    )
  }

  def createLab(implicit request: MyAuthenticatedRequest[MultipartFormData[TemporaryFile]]) = {
    val form = labForm.bindFromRequest
    request.body.file("logo") match {
      case None => AddNewLabWithoutLogo(form)
      case Some(tempFile) => Logo(tempFile) match {
        case Some(smallLogo) => AddNewLabWithLogo(form, smallLogo)
        case _               => LogoInvalid(form)
      }
    }
  }

  def list(filter: Option[String]) = AuthorizedWith(_.role == Administrator) { implicit request => Future {
    if(filter.isDefined) {
      Ok(views.html.labViews.list(Lab.filteredWith(filter.get))(request, request.user.get.role))
    } else {
      Ok(views.html.labViews.list(Lab.listAll)(request, request.user.get.role))
    }
  }}

  def delete(id: Long) = AuthorizedWith(_.role == Administrator) { implicit request => Future {
    Lab.findById(id).fold(
      BadRequest(views.html.labViews.list(Lab.listAll)(request, request.user.get.role)).flashing(("error", "You tried to delete a non-existing lab"))
    )(lab => {
      lab.destroy()
      Redirect(routes.LabController.list(None)).flashing(("success", "Lab successfully deleted"))
    })
  }}
}
