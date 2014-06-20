package controllers

import org.joda.time.{Period, Duration}

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
