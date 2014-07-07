package models
import com.github.nscala_time.time.Imports._
import helpers.DateTimeUtils
import DateTimeUtils.TimeString
import play.api.db.DB
import scala.collection.mutable
import controllers.ConferenceController.SimpleConference
import anorm._

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

  def save = Conference.save(this)

  def destroy = Conference.destroy(this)
}

object Conference {
  val confs = mutable.HashMap[Long, Conference](
    (0, Conference(0, "Les oiseaux chantent", "La vie est belle, et c'est super cool de s'appeller Michel", Speaker.first, DateTime.now + 2.week, 1.hour, Lab.findById(0).get, true)),
    (1, Conference(1, "test conf 2", "test abstr 2", Speaker.second, DateTime.now + 1.week, 2.hour, Lab.findById(0).get, true)),
    (2, Conference(2, "past conference", "test abstra 3", Speaker.first, DateTime.now - 1.week, 2.hour, Lab.findById(0).get, true)),
    (3, Conference(3, "Conference that needs to be allowed", "This needs to be accepted !", Speaker.second, DateTime.now + 3.week, 2.hour, Lab.findById(0).get, false))
  )

  def findAll = confs.values.toList.sortBy(_.id)

  def find(id: Long): Option[Conference] = confs.get(id)

  def findConfsAllowableBy(user: User): List[Conference] =
    confs.values
    .filter(c => !c.accepted && (user.role == Administrator || user.lab == c.organizedBy))
    .toList.sortBy(_.startDate)

  def findAccepted: List[Conference] = confs.values.filter(_.accepted == true).toList

  def saveNew(conf: SimpleConference): Long = {
    val nextId = confs.keySet.max + 1
    confs += ((nextId, Conference.fromSimpleConference(conf, nextId).asAccepted))
    nextId
  }

  def save(conf: Conference): Option[Conference] = {confs += ((conf.id, conf)); Option(conf)}

  def destroy(conference: Conference) = confs.remove(conference.id)

  def fromSimpleConference(conf: SimpleConference, withId: Long): Conference = {
    Conference(withId, conf.title, conf.abstr, Speaker.findById(conf.speakerId).get, conf.date, conf.length.toDuration, Lab.findById(conf.organizerId).get, false)
  }
}