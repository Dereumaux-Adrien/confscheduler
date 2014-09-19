package controllers

import java.io.File

import MySecurity.Authentication._
import MySecurity.Authorization._
import models.{Administrator, Lab}
import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.Crypto
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LabController extends Controller {
  case class SimpleLab(acronym: String, name: String)

  val labForm = Form {
    mapping(
      "acronym" -> nonEmptyText(1, 100),
      "name" -> nonEmptyText(1, 254))(SimpleLab.apply)(SimpleLab.unapply)
  }

  def newLab: Action[AnyContent] = AuthorizedWith(_.role == Administrator) { implicit request => Future {
      Ok(views.html.labViews.newLab(labForm)(request, request.user.get.role))
  }}

  def create = MyAuthenticated(parse.multipartFormData) { implicit request => {
    request.user.map(_.role) match {
      case Some(Administrator) => createLab
      case _                   => Redirect(routes.ConferenceController.listUpcomingConfs()).flashing(("error", "You do not have the rights to create a new lab."))
    }
  }}

  def successfullAddition(labName: String) = Redirect(routes.LabController.list()).flashing(("success", "Successfully created new lab: " + labName))

  def AddNewLabWithLogo(form: Form[SimpleLab], logo: FilePart[TemporaryFile])(implicit request: Request[Any]) = {
    labForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.labViews.newLab(formWithErrors)(request, Administrator)),
      newLab         => {
        val logoId = Crypto.generateToken
        logo.ref.moveTo(new File(Play.configuration.getString("application.imageSavePath").getOrElse(System.getenv("HOME") + "/.confscheduler/logos/") + logoId))
        val lab   = Lab.fromSimpleLab(newLab, Some(logoId)).get
        val LabId = lab.save().get
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
    val logo = request.body.file("logo")
    val isImage = logo.flatMap(_.contentType).exists(t => t.equals("image/jpeg") || t.equals("image/png"))
    val sizeUnder4Mb = logo.map(_.ref.file.length()).exists(_ < 4*1024*1024) //We only accept file up to 4Mb in size
    val form = labForm.bindFromRequest

    logo match {
      case Some(f) if isImage && sizeUnder4Mb => AddNewLabWithLogo(form, f)
      case Some(f)                            => LogoInvalid(form)
      case None                               => AddNewLabWithoutLogo(form)
    }
  }

  def list = AuthorizedWith(_.role == Administrator) { implicit request => Future {
    Ok(views.html.labViews.list(Lab.listAll)(request, request.user.get.role))
  }}

  def delete(id: Long) = AuthorizedWith(_.role == Administrator) { implicit request => Future {
    Lab.findById(id).fold(
      BadRequest(views.html.labViews.list(Lab.listAll)(request, request.user.get.role)).flashing(("error", "You tried to delete a non-existing lab"))
    )(lab => {
      lab.destroy()
      Redirect(routes.LabController.list()).flashing(("success", "Lab successfully deleted"))
    })
  }}
}
