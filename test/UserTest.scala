import helpers.DateTimeUtils
import DateTimeUtils.TimeString
import models.{Moderator, User}
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class UserTest extends Specification {
  "User" should {
    "be able to save a new User" in {
      val newUser = User(12356, "T", "P", "badam@gmail.com", "987654321", Moderator, "")
      val userNb  = User.count

      newUser.save

      User.count must beEqualTo (userNb+1)
      User.findById(12356) must beSome
      User.findById(12356).get.firstName must beEqualTo ("T")
      User.findById(12356).get.lastName must beEqualTo ("P")
      User.findById(12356).get.email must beEqualTo ("badam@gmail.com")
      User.findById(12356).get.hashedPass must beEqualTo ("987654321")
      User.findById(12356).get.role must be (models.Moderator)

      newUser.destroy
    }
  }
}
