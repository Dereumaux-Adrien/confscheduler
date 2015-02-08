package controllers

import models.Conference
import play.api.mvc._

object RSSFeedController extends Controller {
  def index = Action { implicit request =>
    val publicConferences = Conference.findAccepted(None)
    Ok(views.xml.rss.rssIndex(publicConferences))
  }
}
