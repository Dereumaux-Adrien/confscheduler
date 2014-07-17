package models

import anorm.SqlParser._
import com.github.nscala_time.time.Imports._
import controllers.routes
import org.joda.time.format.ISODateTimeFormat
import play.api.db.DB
import controllers.ConferenceController.{ConferenceEvent, SimpleConference}
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
  val formatter = DateTimeFormat.forPattern("YYYY-MM-dd hh:mm")
  val isoFormatter = ISODateTimeFormat.dateTime()

  def timeFromNow: String = {
    val i = DateTime.now to startDate
    val p = i.toPeriod
    val d = i.toDuration

    d.getStandardDays + " days, " + p.hours + " hours, " + p.minutes + " minutes."
  }

  def displayDate: String = formatter.print(startDate)

  def asAccepted: Conference =
    Conference(id, title, abstr, speaker, startDate, length, organizedBy, accepted = true)

  def withId(newId: Long): Conference =
    Conference(newId, title, abstr, speaker, startDate, length, organizedBy, accepted)

  def isInFuture: Boolean = startDate > DateTime.now

  def save = Conference.save(this)

  def destroy = Conference.destroy(this)

  def toConfEvent: ConferenceEvent =
    ConferenceEvent(title, isoFormatter.print(startDate), isoFormatter.print(startDate + length), routes.ConferenceController.viewConf(id).toString(), "#FFF")
}

object Conference {
  val isoFormatter = ISODateTimeFormat.dateTime()

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
    Conference(-1, "DNA will explain every adult behaviour", "Discover how the new discoveries about lactose regulation in E.Coli will be over-interpreted for generations to come !", Speaker.listAll(0), DateTime.now - 2.week, 1.hour, Lab.listAll(0), true),
    Conference(-1, "DNA methylations will explain every adult behaviour", "Discover how the new discoveries about DNA expression regulation via histone-methylation will be over-interpreted for generations to come !", Speaker.listAll(2), DateTime.now + 1.week, 2.hour, Lab.listAll(0), true),
    Conference(-1, "Why Rosalyn Franklin really didn't deserve the Nobel prize", "After all, who wants women in science ? It's not as if they could do important work, like discovering viruses !", Speaker.listAll(1), DateTime.now + 2.week, 2.hour, Lab.listAll(1), true),
    Conference(-1, "I have a Nobel, I can say anything I want, people will listen", "We are being lied to ! HIV doesn't cause AIDS, climate change isn't real, and astrology from Elle is a better predictor than epigenetics !", Speaker.listAll(3), DateTime.now + 3.week, 2.hour, Lab.listAll(0), false)
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
        "id" -> conf.id,
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

  def count: Long = DB.withConnection{implicit c =>
    SQL("SELECT count(*) FROM Conference")
      .as(scalar[Long].single)
  }

  def between(start: DateTime, end: DateTime) = DB.withConnection { implicit c =>
    SQL("""
        SELECT * FROM Conference
        WHERE (startDate, startDate) OVERLAPS ({startDate}, {endDate})
      """)
    .on(
      "startDate" -> start,
      "endDate"   -> end
    ).as(conferenceParser *)
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
    if(conf.speakerId != -1 ){
      Conference(-1, conf.title, conf.abstr, Speaker.findById(conf.speakerId).get, conf.date + conf.time, conf.length, Lab.findById(conf.organizerId).get, false)
    } else {
      val newSpeaker = Speaker(-1, conf.firstName.get, conf.lastName.get, conf.speakerTitle.get, conf.team.get, conf.organisation.get, conf.email.get).save.get
      Conference(-1, conf.title, conf.abstr, newSpeaker, conf.date + conf.time, conf.length, Lab.findById(conf.organizerId).get, false)
    }
  }
}