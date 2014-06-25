package models
import com.github.nscala_time.time.Imports._
import helpers.DateTimeUtils
import DateTimeUtils.TimeString
import scala.collection.mutable
import controllers.ConferenceController.SimpleConference

case class Conference (
    id         : Long,
    title      : String,
    abstr      : String,
    speaker    : Speaker,
    startDate  : DateTime,
    length     : Duration,
    organizedBy: Lab,
    accepted   : Boolean
) {
    def timeFromNow: String = {
        val i = DateTime.now to startDate
        val p = i.toPeriod
        val d = i.toDuration

        d.getStandardDays + " days, " + p.hours + " hours, " + p.minutes + " minutes."
    }

    def asAccepted: Conference = {
      Conference(id, title, abstr, speaker, startDate, length, organizedBy, accepted = true)
    }

    def isInFuture: Boolean = startDate > DateTime.now
}

object Conference {
  val confs = mutable.Set(
        Conference(0, "Les oiseaux chantent", "La vie est belle, et c'est super cool de s'appeller Michel", Speaker.findById(0).get, DateTime.now + 2.week, 1.hour, Lab.findById(0).get, true),
        Conference(1, "test conf 2", "test abstr 2", Speaker.findById(1).get, DateTime.now + 1.week, 2.hour, Lab.findById(0).get, true),
        Conference(2, "past conference", "test abstra 3", Speaker.findById(1).get, DateTime.now - 1.week, 2.hour, Lab.findById(0).get, true)
  )

  var nextId = 2

  def findAll = confs.toList.sortBy(_.id)

  def find(id: Long) = confs.find(c => c.id == id)

  def save(conf: SimpleConference): Long = {
    val resultId = nextId
    confs += Conference.fromSimpleConference(conf).asAccepted
    nextId += 1
    resultId
  }

  def fromSimpleConference(conf: SimpleConference): Conference = {
    Conference(nextId, conf.title, conf.abstr, Speaker.findById(conf.speakerId).get, conf.date, conf.length.toDuration, Lab.findById(conf.organizerId).get, false)
  }
}