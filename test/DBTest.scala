import models.{Moderator, Lab, User}
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test.WithApplication

@RunWith(classOf[JUnitRunner])
class DBTest extends Specification {
  "User" should {
    "be savable if valid" in new WithApplication() {
      val validUser = User(-1, "Thomas", "Pthe", "sdfsqdfs@fqsdfsq.fr", Lab.listAll(0), "123456789", Moderator, "")
      val userCount = User.count

      validUser.save

      User.count mustEqual userCount+1
    }
  }
}
