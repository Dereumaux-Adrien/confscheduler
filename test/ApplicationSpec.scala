import models._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.{Mode, mvc, Play}
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

  def testAccess(url: String, role: UserRole, access: Boolean) = role.toString + " " + {if(access) "can" else "can't"} + " access " + url >> {
    val result = role match {
      case Administrator => route(FakeRequest(GET, url).withSession(getAdminSession: _*)).get
      case Moderator     => route(FakeRequest(GET, url).withSession(getModeratorSession: _*)).get
      case Contributor   => route(FakeRequest(GET, url).withSession(getContributorSession: _*)).get
      case _             => route(FakeRequest(GET, url)).get
    }
    isAccessible(result) must beEqualTo(access)
  }

  def adminOnlyAccess(url: String) = {
    testAccess(url, Administrator, access = true)
    testAccess(url, Moderator, access = false)
    testAccess(url, Contributor, access = false)
    testAccess(url, Guest, access = false)
  }

  var fake: FakeApplication = _

  step {fake = FakeApplication()}
  step {Play.start(fake)}

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
      adminOnlyAccess("/lab/new")
    }

    "render the lab list page to only an admin" in {
      adminOnlyAccess("/lab/all")
    }

    "render the user list page to only an admin" in {
      adminOnlyAccess("/user/all")
    }
  }
  step {Play.stop()}
}
