package helpers

import org.joda.time._
import org.joda.time.format._
import anorm._

object DateTimeUtils {
  implicit class TimeString(s: String) {
    def isValidDuration: Boolean = s.matches( """^(\d+[hH])?(\d+[mM])?$""") && !s.trim.isEmpty

    def toDuration: Duration = {
      val pattern = """^(?:(\d+)[hH])?(?:(\d+)[mM])?$""".r
      s match {
        case pattern(h, m) if h != null && m != null => new Period(h.toInt, m.toInt, 0, 0).toStandardDuration
        case pattern(h, _) if h != null              => new Period(h.toInt, 0, 0, 0).toStandardDuration
        case pattern(_, m) if              m != null => new Period(0, m.toInt, 0, 0).toStandardDuration
        case _ => throw new IllegalArgumentException("duration string given doesn't follow the format nn[Hh]nn[mM]")
      }
    }
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
