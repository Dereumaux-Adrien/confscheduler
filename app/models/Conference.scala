package models

import anorm.SqlParser._
import com.github.nscala_time.time.Imports._
import helpers.DateTimeUtils._
import play.api.db.DB
import scala.collection.mutable
import controllers.ConferenceController.SimpleConference
import anorm._
import helpers.AnormExtension._
import play.api.Play.current

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

  def asAccepted: Conference =
    Conference(id, title, abstr, speaker, startDate, length, organizedBy, accepted = true)

  def withId(newId: Long): Conference =
    Conference(newId, title, abstr, speaker, startDate, length, organizedBy, accepted)

  def isInFuture: Boolean = startDate > DateTime.now

  def save = Conference.save(this)

  def destroy = Conference.destroy(this)
}

object Conference {
  val insertQuery = SQL("""
      INSERT INTO Conference(title, abstr, speaker, startDate, length, organizedBy, accepted)
      VALUES ({title}, {abstr}, {speaker}, {startDate}, {length}, {organizedBy}, {accepted})
  """)

  val updateQuery = SQL("""
      UPDATE Conference
      SET title = {title}, abstr = {abstr}, speaker = {speaker}, startDate = {startDate}, length = {length}, organizedBy = {organizedBy}, accepted = {accepted}
      WHERE id = {id}
  """)

  def fixtures = Set (
    Conference(0, "Les oiseaux chantent", "La vie est belle, et c'est super cool de s'appeller Michel", Speaker.listAll.head, DateTime.now + 2.week, 1.hour, Lab.listAll.head, true),
    Conference(1, "test conf 2", "test abstr 2", Speaker.listAll.tail.head, DateTime.now + 1.week, 2.hour, Lab.listAll.head, true),
    Conference(2, "past conference", "test abstra 3", Speaker.listAll.head, DateTime.now - 1.week, 2.hour, Lab.listAll.head, true),
    Conference(3, "Conference that needs to be allowed", "This needs to be accepted !", Speaker.listAll.tail.head, DateTime.now + 3.week, 2.hour, Lab.listAll.head, false)
  )


  def findById(id: Long): Option[Conference] = DB.withConnection{implicit c =>
    SQL("SELECT * FROM Conference WHERE id = {id}")
      .on("id" -> id)
      .as(conferenceParser.singleOpt)
  }

  def findConfsAllowableBy(user: User): List[Conference] =
    findNotAccepted.filter(user.role == Administrator || user.lab == _.organizedBy)
      .sortBy(_.startDate)

  def findAccepted: List[Conference] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM Conference WHERE accepted = true")
      .as(conferenceParser *)
  }

  def findNotAccepted: List[Conference] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM Conference WHERE accepted = false")
      .as(conferenceParser *)
  }

  def save(conf: Conference): Option[Conference] = DB.withConnection { implicit c =>
    if(findById(conf.id).isDefined) {
      updateQuery.on(
        "title" -> conf.title,
        "abstr" -> conf.abstr,
        "speaker" -> conf.speaker.id,
        "startDate" -> conf.startDate,
        "length" -> conf.length.millis,
        "organizedBy" -> conf.organizedBy.id,
        "accepted" -> conf.accepted
      ).executeUpdate()
      Option(conf)
    } else {
      val newId: Option[Long] = insertQuery.on(
        "title" -> conf.title,
        "abstr" -> conf.abstr,
        "speaker" -> conf.speaker.id,
        "startDate" -> conf.startDate,
        "length" -> conf.length.millis,
        "organizedBy" -> conf.organizedBy.id,
        "accepted" -> conf.accepted
      ).executeInsert()
      newId.map(conf.withId)
    }
  }

  def destroy(conference: Conference) = DB.withConnection { implicit c =>
    SQL("DELETE FROM Conference WHERE id = {id}").on("id" -> conference.id).executeUpdate()
  }

  def destroyAll(): Unit = DB.withConnection { implicit c =>
    SQL("DELETE FROM Conference").executeUpdate()
  }

  def seedDB(): Unit = {
    for(conf <- fixtures) {
      conf.save
    }
  }

  def listAll: List[Conference] = DB.withConnection {implicit c =>
    SQL("SELECT * FROM Conference").as(conferenceParser *)
  }

  private val conferenceParser: RowParser[Conference] = {
    get[Long]("id") ~
    get[String]("title") ~
    get[String]("abstr") ~
    get[Long]("speaker") ~
    get[DateTime]("startDate") ~
    get[Long]("length") ~
    get[Long]("organizedBy") ~
    get[Boolean]("accepted")  map {
      case id ~ title ~ abstr ~ speaker ~ startDate ~ length ~ organizedBy ~ accepted =>
        Conference(id, title, abstr, Speaker.findById(speaker).get, startDate, new Duration(length), Lab.findById(organizedBy).get, accepted)
    }
  }

  def fromSimpleConference(conf: SimpleConference): Conference = {
    Conference(-1, conf.title, conf.abstr, Speaker.findById(conf.speakerId).get, conf.date, conf.length.toDuration, Lab.findById(conf.organizerId).get, false)
  }
}