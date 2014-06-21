import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import controllers.routes

import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication}

@RunWith(classOf[JUnitRunner])
class LoginSpec extends Specification {
  val validUsername = ("email", "rosa@gmail.com")
  val validPassword = ("password", "123456789")

  val invalidUsername = ("email", "liuqgrfuqqezrhferfqrf@gmail.com")
  val invalidPassword = ("password", "sidygfuhqszliyhfzsflidrghqlifq")

  "The login controller" should {
    "allow a valid user to authenticate" in new WithApplication {
      val login = route(FakeRequest(POST, "/login").withFormUrlEncodedBody(validUsername, validPassword)).get
      status(login) must equalTo(SEE_OTHER)
      cookies(login).get("PLAY2AUTH_SESS_ID") must beSome
    }

    "redirect to the conf list page after login" in new WithApplication {
      val login = route(FakeRequest(POST, "/login").withFormUrlEncodedBody(validUsername, validPassword)).get
      headers(login) must havePair("Location" -> routes.ConferenceOpthAuthController.listConfs().toString())
    }

    "prevent invalid user from logging in" in new WithApplication {
      val login = route(FakeRequest(POST, "/login").withFormUrlEncodedBody(invalidUsername, invalidPassword)).get
      status(login) must equalTo(BAD_REQUEST)
    }

    "prevent valid user with invalid password from logging in" in new WithApplication {
      val login = route(FakeRequest(POST, "/login").withFormUrlEncodedBody(validUsername, invalidPassword)).get
      status(login) must equalTo(BAD_REQUEST)
    }
  }
}
