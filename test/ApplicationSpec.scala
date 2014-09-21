import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.Play
import play.api.mvc.Session

import play.api.test._
import play.api.test.Helpers._

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

  var fake: FakeApplication = _

  step {fake = FakeApplication()}
  step {Play.start(fake)}

  "Application" should {
    "send 404 on a bad request" in {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the upcoming conferences page" in {
      val conflist = route(FakeRequest(GET, "/conf/upcoming")).get

      status(conflist) must equalTo(OK)
      contentType(conflist) must beSome.which(_ == "text/html")
    }

    "render the conference list page" in {
      val conflist = route(FakeRequest(GET, "/conf/all")).get

      status(conflist) must equalTo(OK)
      contentType(conflist) must beSome.which(_ == "text/html")
    }

    "render the calendar page" in {
      val calendar = route(FakeRequest(GET, "/calendar")).get

      status(calendar) must equalTo(OK)
      contentType(calendar) must beSome.which(_ == "text/html")
      contentAsString(calendar) must contain ("Calendar")
      contentAsString(calendar) must contain ("""<div id="calendar">""")
    }

    "render the login page" in {
      val login = route(FakeRequest(GET, "/login")).get

      status(login) must equalTo(OK)
      contentType(login) must beSome.which(_ == "text/html")
      contentAsString(login) must contain("Login")
    }

    "render the new lab page to an admin" in {
      val newLab = route(FakeRequest(GET, "/lab/new").withSession(getAdminSession: _*)).get

      status(newLab) must equalTo(OK)
      contentType(newLab) must beSome.which(_ == "text/html")
      contentAsString(newLab) must contain("Acronym")
      contentAsString(newLab) must contain("Name")
    }

    "not render the new lab page to a moderator" in {
      val newLab = route(FakeRequest(GET, "/lab/new").withSession(getModeratorSession: _*)).get

      status(newLab) must equalTo(SEE_OTHER)
      contentAsString(newLab) must not contain "Acronym"
      contentAsString(newLab) must not contain "Name"
    }

    "render the lab list page to an admin" in {
      val newLab = route(FakeRequest(GET, "/lab/all").withSession(getAdminSession: _*)).get

      status(newLab) must equalTo(OK)
      contentType(newLab) must beSome.which(_ == "text/html")
    }
  }
  step {Play.stop()}
}
