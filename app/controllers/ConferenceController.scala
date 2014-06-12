package controllers

import play.api.mvc._
import com.github.nscala_time.time.Imports._
import jp.t2v.lab.play2.auth.AuthElement
import models.Contributor

object ConferenceController extends Controller with AuthElement with AuthConfigImpl {
  def addConf = StackAction(AuthorityKey -> Contributor) { request =>
    Ok(views.html.addConf(request))
  }

  def viewConf(id: Long) = Action { request =>
    models.Conference.find(id) match {
        case Some(c) => Ok(views.html.conf(c)(request))
        case None    => NotFound
    }
  }

  def listConfs = Action { request =>
    Ok(views.html.index(models.Conference.findAll.sortBy(_.startDate))(request))
  }

  def calendar = Action { request => 
    Ok(views.html.calendar(request))
  }
}