package helpers

import org.joda.time._
import org.joda.time.format._
import anorm._

import scala.concurrent.duration._

object DateTimeUtils {
  implicit class TimeString(s: String) {
    def isValidDuration: Boolean = s.matches( """^(\d+[hH])?(\d+[mM])?$""") && !s.trim.isEmpty

    def toDuration: org.joda.time.Duration = {
      val pattern = """^(?:(\d+)[hH])?(?:(\d+)[mM])?$""".r
      s match {
        case pattern(h, m) if h != null && m != null => new Period(h.toInt, m.toInt, 0, 0).toStandardDuration
        case pattern(h, _) if h != null              => new Period(h.toInt, 0, 0, 0).toStandardDuration
        case pattern(_, m) if              m != null => new Period(0, m.toInt, 0, 0).toStandardDuration
        case _ => throw new IllegalArgumentException("duration string given doesn't follow the format nn[Hh]nn[mM]")
      }
    }
  }

  def findNextYear(sendOn: Int): FiniteDuration = {
    var count = 1
    var currentDay = DateTime.now().plusDays(1)
    while (currentDay.getDayOfYear != sendOn) {
      count += 1
      currentDay = currentDay.plusDays(1)
    }

    count.days - DateTime.now().getMillisOfDay.milliseconds
  }

  def findNextMonth(sendOn: Int): FiniteDuration = {
    var count = 1
    var currentDay = DateTime.now().plusDays(1)
    while (currentDay.getDayOfMonth != sendOn) {
      count += 1
      currentDay = currentDay.plusDays(1)
    }

    count.days - DateTime.now().getMillisOfDay.milliseconds
  }

  def findNextDayOfWeek(day: Int): FiniteDuration = {
    var count: Int = 1
    var currentDay = DateTime.now().plusDays(1)
    while (currentDay.getDayOfWeek != day) {
      count += 1
      currentDay = currentDay.plusDays(1)
    }

    count.days - DateTime.now().getMillisOfDay.milliseconds
  }
}

object AnormExtension {
  val dateFormatGeneration: DateTimeFormatter = ISODateTimeFormat.dateTime()

  implicit def rowToDateTime: Column[DateTime] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case ts: java.sql.Timestamp => Right(new DateTime(ts.getTime))
      case d: java.sql.Date => Right(new DateTime(d.getTime))
      case str: java.lang.String => Right(dateFormatGeneration.parseDateTime(str))
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass) )
    }
  }

  implicit val dateTimeToStatement = new ToStatement[DateTime] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: DateTime): Unit = {
      s.setTimestamp(index, new java.sql.Timestamp(aValue.withMillisOfSecond(0).getMillis) )
    }
  }

}
