import javax.xml.transform.OutputKeys

import models._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.{Logger, mvc, Play}
import scala.language.experimental.macros

import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.Future

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {
  def getSession(email: String, pass: String) = {
    val login = route(FakeRequest(POST, "/login").withFormUrlEncodedBody(("email", email), ("password", pass))).get
    session(login).data.map{case (k, v) => (k, v)}.toList
  }

  def getAdminSession = getSession("rosa@gmail.com", "123456789")
  def getModeratorSession = getSession("jimmy@gmail.com", "987654321")
  def getContributorSession = getSession("tom@gmail.com", "123456789")

  def isAccessible(page: Future[mvc.Result]) = status(page) == OK && contentType(page).contains("text/html")

  def testAccess(url: String, role: UserRole, access: Boolean, method: String) = role.toString + " " + {if(access) "can" else "can't"} + " access " + url >> {
    val result = role match {
      case Administrator => route(FakeRequest(method, url).withSession(getAdminSession: _*)).get
      case Moderator     => route(FakeRequest(method, url).withSession(getModeratorSession: _*)).get
      case Contributor   => route(FakeRequest(method, url).withSession(getContributorSession: _*)).get
      case _             => route(FakeRequest(method, url)).get
    }
    isAccessible(result) must beEqualTo(access)
  }

  def adminOnlyAccess(url: String, method: String) = {
    testAccess(url, Administrator, access = true, method)
    testAccess(url, Moderator, access = false, method)
    testAccess(url, Contributor, access = false, method)
    testAccess(url, Guest, access = false, method)
  }

  def modOrBetterAccess(url: String, method: String) = {
    testAccess(url, Administrator, access = true, method)
    testAccess(url, Moderator, access = true, method)
    testAccess(url, Contributor, access = false, method)
    testAccess(url, Guest, access = false, method)
  }

  def loggedAccess(url: String, method: String) = {
    testAccess(url, Administrator, access = true, method)
    testAccess(url, Moderator, access = true, method)
    testAccess(url, Contributor, access = true, method)
    testAccess(url, Guest, access = false, method)
  }

  var fake: FakeApplication = _

  step {fake = FakeApplication()}
  step {Play.start(fake)}

  "API" should {
    "Provide a JSON list of confs" in {
      val startDate = DateTime.now().toString(ISODateTimeFormat.date())
      val endDate = DateTime.now().plusDays(7).toString(ISODateTimeFormat.date())
      val url = "/api/v1/conf/all?start=" + startDate + "&end=" + endDate
      val JSONListConfs = route(FakeRequest(GET, url)).get
      status(JSONListConfs) mustEqual OK and(contentType(JSONListConfs).contains("application/json") must beTrue)
    }
  }

  "Application" should {
    "send 404 on a bad request" in {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the upcoming conferences page" in {
      val upcomingConfs = route(FakeRequest(GET, "/conf/upcoming")).get
      isAccessible(upcomingConfs) must beTrue
    }

    "render the conference list page" in {
      val confList = route(FakeRequest(GET, "/conf/all")).get
      isAccessible(confList) must beTrue
    }

    "render the calendar page" in {
      val calendar = route(FakeRequest(GET, "/calendar")).get
      isAccessible(calendar) must beTrue
      contentAsString(calendar) must contain ("""<div id="calendar">""")
    }

    "render the login page" in {
      val login = route(FakeRequest(GET, "/login")).get
      isAccessible(login) must beTrue
    }

    "render the new lab page to only an admin" in {
      adminOnlyAccess("/lab/new", GET)
    }

    "render the lab list page to only an admin" in {
      adminOnlyAccess("/lab/all", GET)
    }

    "render the user list page to only an admin" in {
      adminOnlyAccess("/user/all", GET)
    }

    "render the user add page to an admin or moderator" in {
      modOrBetterAccess("/user/new", GET)
    }

    "render the conference add page to a logged user" in {
      loggedAccess("/conf/new", GET)
    }

    "render the conference accept page to an admin or moderator" in {
      modOrBetterAccess("/conf/allow", GET)
    }
  }
  step {Play.stop()}
}
