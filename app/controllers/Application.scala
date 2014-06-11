package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index(models.Conference.findAll))
  }

  def addConf = Action {
    Ok(views.html.addConf())
  }

  def viewConf(id: Long) = Action {
    models.Conference.find(id) match {
        case Some(c) => Ok(views.html.conf(c))
        case None    => NotFound
    }
  }
}