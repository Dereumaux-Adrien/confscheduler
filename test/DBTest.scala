import models._
import org.joda.time.{Duration, DateTime}
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.Play
import play.api.test.{FakeApplication, WithApplication}

@RunWith(classOf[JUnitRunner])
class DBTest extends Specification {
  var fake: FakeApplication = _

  step {fake = FakeApplication()}
  step {Play.start(fake)}

  "DB" should {
    "be able to save valid Users" in {
      val validUser = User(-1, "Thomas", "Pthe", "sdfsqdfs@fqsdfsq.fr", Lab.listAll(0), "123456789", Moderator, "")
      val userCount = User.count

      validUser.save

      User.count mustEqual userCount + 1
      User.listAll.find(_.email == "sdfsqdfs@fqsdfsq.fr") must beSome
    }

    "be able to save valid Labs" in {
      val validLab = Lab(-1, "ERF", "Erectus Raging Friends", None)
      val labCount = Lab.count

      validLab.save()

      Lab.count mustEqual labCount + 1
      Lab.listAll.find(_.name == "Erectus Raging Friends") must beSome
    }

    "be able to save valid Locations" in {
      val validLoc = Location(-1, "test", Some("test"), "testaswell", "qdfsfg", "qfqsfgsdfg", 4, "qegeg")
      val locCount = Location.count

      validLoc.save

      Location.count mustEqual locCount + 1
      Location.listAll.find(_.buildingName == Some("test")) must beSome
    }

    "be able to save valid Speakers" in {
      val validSpeaker = Speaker(-1, "test", "testName", "Dr.", "Blabloup", "Blipblop", "wdfsfg@qsfgdfg.gt")
      val speakerCount = Speaker.count

      validSpeaker.save

      Speaker.count mustEqual speakerCount + 1
      Speaker.listAll.find(_.email == "wdfsfg@qsfgdfg.gt") must beSome
    }

    "be able to save valid Conferences" in {
      val validConf = Conference(-1, "qsdfqsdf", "sd<f<sdf", Speaker.listAll(0), DateTime.now().plusDays(5), new Duration(5*1000*3600),
        Lab.listAll(0), Location.listAll(0), true, None, true)
      val confCount = Conference.count

      validConf.save

      Conference.count mustEqual confCount + 1
      Conference.listAll.find(_.title == "qsdfqsdf") must beSome
    }
  }
  step {Play.stop()}
}
