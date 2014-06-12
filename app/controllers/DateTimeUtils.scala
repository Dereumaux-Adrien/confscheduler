package controllers

import org.joda.time.{Period, Duration}

object DateTimeUtils {
  def isValidDuration(duration: String): Boolean  = duration.matches("""^(\d+[hH])?(\d+[mM])?$""") && !duration.trim.isEmpty

  def strToDuration(duration: String)  : Duration = {
    val pattern = """^(\d+[hH])?(\d+[mM])?$""".r
    duration match {
      case pattern(h, m) => new Period(h.toInt, m.toInt, 0, 0).toStandardDuration
      case pattern(h, _) => new Period(h.toInt, 0, 0, 0).toStandardDuration
      case pattern(_, m) => new Period(0, m.toInt, 0, 0).toStandardDuration
      case _             => throw new IllegalArgumentException("duration string given doesn't follow the format nn[Hh]nn[mM]")
    }
  }
}
