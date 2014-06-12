package controllers

import play.api.mvc.Controller
import jp.t2v.lab.play2.auth.OptionalAuthElement
import com.github.nscala_time.time.Imports._

/**
 * Created by thomas on 12/06/14.
 */
object ConferenceViewController extends Controller with OptionalAuthElement with AuthConfigImpl {
  def listConfs = StackAction { implicit request =>
    Ok(views.html.index(models.Conference.findAll.sortBy(_.startDate))(request, loggedIn.isDefined))
  }

  def calendar = StackAction { implicit request =>
    Ok(views.html.calendar(request, loggedIn.isDefined))
  }

  def viewConf(id: Long) = StackAction { implicit request =>
    models.Conference.find(id) match {
      case Some(c) => Ok(views.html.conf(c)(request, loggedIn.isDefined))
      case None    => NotFound
    }
  }
}
