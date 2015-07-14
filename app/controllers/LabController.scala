package controllers

import java.io.File

import MySecurity.Authentication._
import MySecurity.Authorization._
import logo.Logo
import models.{User, UserRole, Administrator, Lab}
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
  case class SimpleLab(acronym: String, name: String, email: String)

  val labForm = Form {
    mapping(
      "acronym" -> nonEmptyText(1, 100),
      "name" -> nonEmptyText(1, 254),
      "email" -> email
    )(SimpleLab.apply)(SimpleLab.unapply)
  }

  def authenticatedUserRole(implicit request: MyAuthenticatedRequest[AnyContent]): Option[UserRole] = request.user.map(_.role)

  def logo(id: Long) = ForcedAuthentication{implicit request => Future {
      Lab.findById(id).flatMap(_.logoId) match {
        case Some(logoId) => Ok.sendFile(Logo.find(logoId))
        case _            => Redirect(routes.LabController.list(None)).flashing(("error", "Couldn't find the logo for this lab"))
      }
  }}

  def newLab: Action[AnyContent] = AuthorizedWith(_.role == Administrator) { implicit request => Future {
      Ok(views.html.labViews.newLab(labForm)(request, request.user.get.role))
  }}

  def modify(id: Long) = ForcedAuthentication {
    implicit request => {
      val lab = Lab.findById(id)
      if(lab.isDefined){
        if(request.user.get.role == Administrator){
          Future(Ok(views.html.labViews.modifyLab(labForm, lab.get)(request, authenticatedUserRole.get)))
        }else{
          Future(Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You don't have the rights to modify a lab")))
        }
      }else{
        Future(Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You tried to modify an unknown lab")))
      }
    }
  }

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

  def reCreate(labId: Long) = MyAuthenticated(parse.multipartFormData) { implicit request => {
      request.user.map(_.role) match {
        case Some(Administrator) => {
          val form = labForm.bindFromRequest
          request.body.file("logo") match {
            case None => {
              form.fold(
                formWithErrors => BadRequest(views.html.labViews.modifyLab(formWithErrors, Lab.findById(labId).get)(request, Administrator)),
                lab           => {
                  val user = request.user.get
                  if(user.role == Administrator){
                    val oldLab =Lab.findById(labId).get
                    Lab.modifyFromSimpleLab(oldLab, lab, None)
                    oldLab.save

                    Redirect(routes.LabController.list(None))
                  }else{
                    Redirect(routes.ConferenceController.listUpcomingConfs(None))
                  }
                }
              )
            }
            case Some(tempFile) => Logo(tempFile) match {
              case Some(smallLogo) => {
                val form= labForm.bindFromRequest()
                val logo = smallLogo
                form.fold(
                  formWithErrors => BadRequest(views.html.labViews.modifyLab(formWithErrors, Lab.findById(labId).get)(request, Administrator)),
                  lab           => {
                    val user = request.user.get
                    if(user.role == Administrator){
                      val oldLab =Lab.findById(labId).get
                      logo.save
                      Lab.modifyFromSimpleLab(oldLab, lab, Some(logo.logoId))
                      oldLab.save

                      Redirect(routes.LabController.list(None))
                    }else{
                      Redirect(routes.ConferenceController.listUpcomingConfs(None))
                    }
                  }
                )
              }
              case _               => LogoInvalid(form)
            }
          }
        }
        case _                   => Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You do not have the rights to modify a lab."))
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
      val labHasBeenDestroyed = lab.destroy()
      if(labHasBeenDestroyed) {
        Redirect(routes.LabController.list(None)).flashing(("success", "Lab successfully deleted"))
      } else {
        Redirect(routes.LabController.list(None)).flashing(("error", "This lab is still referenced by some seminars or users, please delete them first"))
      }
    })
  }}
}
