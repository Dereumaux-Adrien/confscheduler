package models
import com.github.nscala_time.time.Imports._
import java.util.Date
import controllers.DateTimeUtils

case class Conference (
    id       : Long,
    name     : String,
    abstr    : String,
    speaker  : Speaker,
    startDate: DateTime,
    length   : Duration
) {
    def timeFromNow: String = {
        val i = DateTime.now to startDate
        val p = i.toPeriod
        val d = i.toDuration

        d.getStandardDays + " days, " + p.hours + " hours, " + p.minutes + " minutes."
    }
}

object Conference {
  val confs = Set(Conference(0, "Les oiseaux chantent", "La vie est belle, et c'est super cool de s'appeller Michel", Speaker.findById(0).get, DateTime.now + 2.week, 1.hour),
        Conference(1, "test conf 2", "test abstr 2", Speaker.findById(1).get, DateTime.now + 1.week, 2.hour))

  var nextId = 2

  def findAll = confs.toList.sortBy(_.id)

  def find(id: Long) = confs.find(c => c.id == id)

  def create(name: String, abstr: String, speakerId: Long, startDate: Date, length: String): Conference = {
    nextId += 1
    Conference(nextId, name, abstr, Speaker.findById(speakerId).get, new DateTime(startDate), DateTimeUtils.strToDuration(length))
  }

  def toPublicTuple(conf: Conference): Option[(String, String, Long, Date, String)] =
    Some(conf.name, conf.abstr, conf.speaker.id, conf.startDate.toDate, conf.length.getStandardHours + "h" + conf.length.getStandardMinutes + "m")
}