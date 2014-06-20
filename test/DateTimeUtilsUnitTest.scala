import helpers.DateTimeUtils
import DateTimeUtils.TimeString
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class DateTimeUtilsUnitTest extends Specification {
  "DateTimeUtils" should {
    "be able to convert date time string to a Duration" in {
      "2h".toDuration.getStandardHours must be equalTo 2
      "2m".toDuration.getStandardMinutes must be equalTo 2

      val duration = "4h8m".toDuration
      duration.getStandardHours must be equalTo 4
      duration.getStandardMinutes - duration.getStandardHours * 60 must be equalTo 8
    }
  }
}
