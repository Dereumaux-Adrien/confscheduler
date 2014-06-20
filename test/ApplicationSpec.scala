import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the upcoming conferences page" in new WithApplication{
      val conflist = route(FakeRequest(GET, "/conf")).get

      status(conflist) must equalTo(OK)
      contentType(conflist) must beSome.which(_ == "text/html")
      contentAsString(conflist) must contain ("Upcoming Conferences")
    }

    "render the calendar page" in new WithApplication{
      val calendar = route(FakeRequest(GET, "/calendar")).get

      status(calendar) must equalTo(OK)
      contentType(calendar) must beSome.which(_ == "text/html")
      contentAsString(calendar) must contain ("Public Conferences Calendar")
      contentAsString(calendar) must contain ("""<div id="calendar">""")
    }

    "render the login page" in new WithApplication{
      val login = route(FakeRequest(GET, "/login")).get

      status(login) must equalTo(OK)
      contentType(login) must beSome.which(_ == "text/html")
      contentAsString(login) must contain ("Login")
    }
  }
}
