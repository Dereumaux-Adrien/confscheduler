package controllers

import play.api.mvc._
import com.github.nscala_time.time.Imports._
import jp.t2v.lab.play2.auth.OptionalAuthElement

object ConferenceController extends Controller with OptionalAuthElement with AuthConfigImpl {
  def addConf() = StackAction { implicit request =>
    if (loggedIn.isDefined) {
      Ok(views.html.addConf(request, logged = true))
    } else {
      Redirect(routes.LoginController.login())
    }
  }

  def viewConf(id: Long) = StackAction { implicit request =>
    models.Conference.find(id) match {
        case Some(c) => Ok(views.html.conf(c)(request, loggedIn.isDefined))
        case None    => NotFound
    }
  }

  def listConfs = StackAction { implicit request =>
    Ok(views.html.index(models.Conference.findAll.sortBy(_.startDate))(request, loggedIn.isDefined))
  }

  def calendar = StackAction { implicit request =>
    Ok(views.html.calendar(request, loggedIn.isDefined))
  }
}