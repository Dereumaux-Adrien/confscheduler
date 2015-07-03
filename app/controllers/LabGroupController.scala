package controllers

import models._
import play.api.data.Form
import play.api.data.Forms._
import MySecurity.Authorization._
import scala.concurrent.Future
import MySecurity.Authentication._
import scala.Some
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.Some
import play.api.mvc.Result
import MySecurity.Authentication.MyAuthenticatedRequest

/**
 * Created by adrien on 5/18/15.
 */
object LabGroupController extends Controller {
  case class SimpleLabGroup(name: String)

  val labGroupForm = Form {
    mapping(
      "name" -> nonEmptyText(1, 254)
    )(SimpleLabGroup.apply)(SimpleLabGroup.unapply)
  }

  def authenticatedUserRole(implicit request: MyAuthenticatedRequest[AnyContent]): Option[UserRole] = request.user.map(_.role)

  def create = MyAuthenticated(parse.multipartFormData) { implicit request => {
    request.user.map(_.role) match {
      case Some(Administrator) => addNewLabGroup
      case _                   => Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You do not have the rights to create a new labGroup."))
    }
  }}

  def addNewLabGroup()(implicit request: Request[Any]) = {
    labGroupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.labGroupViews.newLabGroup(formWithErrors)(request, Administrator)),
      newLabGroup         => {
        val labGroup = LabGroup.fromSimpleLabGroup(newLabGroup).get
        val id = labGroup.save().get
        successfullAddition(labGroup.name)
      }
    )
  }

  def newLabGroup: Action[AnyContent] = AuthorizedWith(_.role == Administrator) { implicit request => Future {
    Ok(views.html.labGroupViews.newLabGroup(labGroupForm)(request, request.user.get.role))
  }}

  def successfullAddition(labGroupName: String) = Redirect(routes.LabGroupController.list(None)).flashing(("success", "Successfully created new labGroup: " + labGroupName))

  def modify(id: Long) = ForcedAuthentication {
    implicit request => {
      val group = LabGroup.findById(id)
      if(group.isDefined){
        if(request.user.get.role == Administrator){
          Future(Ok(views.html.labGroupViews.modifyLabGroup(labGroupForm, group.get)(request, authenticatedUserRole.get)))
        }else{
          Future(Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You don't have the rights to modify groups")))
        }
      }else{
        Future(Redirect(routes.ConferenceController.listUpcomingConfs(None)).flashing(("error", "You tried to modify an unknown group")))
      }
    }
  }

  def reCreate(groupId: Long) = ForcedAuthentication { implicit request =>
    Future {
      val form= labGroupForm.bindFromRequest()
      form.fold(
        formWithErrors => BadRequest(views.html.labGroupViews.modifyLabGroup(formWithErrors, LabGroup.findById(groupId).get)(request, authenticatedUserRole.get)),
        group           => reCreateLabGroup(LabGroup.findById(groupId).get, group, request.user.get)
      )
    }
  }

  private def reCreateLabGroup(oldGroup: LabGroup, group: SimpleLabGroup, user: User): Result = {
    if(user.role == Administrator){
      LabGroup.modifyFromSimpleLabGroup(oldGroup, group)
      oldGroup.save
      Redirect(routes.LabGroupController.list(None))
    }else{
      Redirect(routes.ConferenceController.listConfs(None))
    }
  }

  def list(filter: Option[String]) = AuthorizedWith(_.role == Administrator) { implicit request => Future {
    if(filter.isDefined) {
      Ok(views.html.labGroupViews.list(LabGroup.filteredWith(filter.get))(request, request.user.get.role))
    } else {
      Ok(views.html.labGroupViews.list(LabGroup.listAll)(request, request.user.get.role))
    }
  }}

  def delete(id: Long) = AuthorizedWith(_.role == Administrator) { implicit request => Future {
    LabGroup.findById(id).fold(
      BadRequest(views.html.labGroupViews.list(LabGroup.listAll)(request, request.user.get.role)).flashing(("error", "You tried to delete a non-existing labGroup"))
    )(labGroup => {
      labGroup.destroy()
      Redirect(routes.LabGroupController.list(None)).flashing(("success", "LabGroup successfully deleted"))
    })
  }}

  def listLabToAdd(id: Long, filter: Option[String]) = AuthorizedWith(_.role == Administrator) { implicit request => Future {
    if(filter.isDefined) {
      Ok(views.html.labGroupViews.listLab(LabGroup.findById(id).get, Lab.findLabToAddToLabGroup(LabGroup.findById(id).get.id,filter.get), true)(request, request.user.get.role))
    } else {
      Ok(views.html.labGroupViews.listLab(LabGroup.findById(id).get, Lab.findLabToAddToLabGroup(LabGroup.findById(id).get.id), true)(request, request.user.get.role))
    }
  }}

  def listLabOfGroup(id: Long, filter: Option[String]) = AuthorizedWith(_.role == Administrator) { implicit request => Future {
    if(filter.isDefined) {
      Ok(views.html.labGroupViews.listLab(LabGroup.findById(id).get, Lab.findLabToRemoveFromLabGroup(LabGroup.findById(id).get.id,filter.get), false)(request, request.user.get.role))
    } else {
      Ok(views.html.labGroupViews.listLab(LabGroup.findById(id).get, Lab.findLabToRemoveFromLabGroup(LabGroup.findById(id).get.id), false)(request, request.user.get.role))
    }
  }}

  def addLab(idLabGroup: Long, idLab: Long)= AuthorizedWith(_.role == Administrator) { implicit request => Future {
    LabGroup.findById(idLabGroup).fold(
      BadRequest(views.html.labGroupViews.list(LabGroup.listAll)(request, request.user.get.role)).flashing(("error", "You tried to add a Lab into a non-existing labGroup"))
    )(labGroup => {
      Lab.findById(idLab).fold(
        BadRequest(views.html.labGroupViews.list(LabGroup.listAll)(request, request.user.get.role)).flashing(("error", "You tried to add a Lab non-existing into a labGroup"))
      )(labGroup => {
        LabGroup.addLabToGroup(idLabGroup, idLab)
        successfullLabAddition(LabGroup.findById(idLabGroup).get, Lab.findById(idLab).get.name)
      })
    })
  }}

  def removeLab(idLabGroup: Long, idLab: Long)= AuthorizedWith(_.role == Administrator) { implicit request => Future {
    LabGroup.findById(idLabGroup).fold(
      BadRequest(views.html.labGroupViews.list(LabGroup.listAll)(request, request.user.get.role)).flashing(("error", "You tried to add a Lab into a non-existing labGroup"))
    )(labGroup => {
      Lab.findById(idLab).fold(
        BadRequest(views.html.labGroupViews.list(LabGroup.listAll)(request, request.user.get.role)).flashing(("error", "You tried to add a Lab non-existing into a labGroup"))
      )(labGroup => {
        LabGroup.removeLabFromGroup(idLabGroup, idLab)
        successfullLabRemoval(LabGroup.findById(idLabGroup).get, Lab.findById(idLab).get.name)
      })
    })
  }}

  def successfullLabAddition(labGroup: LabGroup, labName: String) = Redirect(routes.LabGroupController.listLabToAdd(labGroup.id, None)).flashing(("success", "Successfully added lab: " + labName + " to Group: " + labGroup.name))

  def successfullLabRemoval(labGroup: LabGroup, labName: String) = Redirect(routes.LabGroupController.listLabOfGroup(labGroup.id, None)).flashing(("success", "Successfully removed lab: " + labName + " from Group: " + labGroup.name))

}