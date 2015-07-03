package models

import anorm.SqlParser._
import com.github.nscala_time.time.Imports._
import controllers.routes
import org.joda.time.format.ISODateTimeFormat
import play.api.db.DB
import controllers.ConferenceController.{ConferenceEvent, SimpleConference}
import controllers.LabGroupController.SimpleLabGroup
import anorm._
import helpers.AnormExtension._
import play.api.Play.current
import play.api.libs.Crypto
import com.github.tototoshi.csv.CSVWriter
import java.io.{FilenameFilter, File}

case class Conference (
    id         : Long,
    var title      : String,
    var abstr      : String,
    var speaker    : Speaker,
    var startDate  : DateTime,
    var length     : Duration,
    var organizedBy: Lab,
    var location   : Location,
    var accepted   : Boolean,
    var acceptCode : Option[String],
    var priv       : Boolean,
    var forGroup   : Option[LabGroup] = None,
    var logoId     : Option[String] = None,
    var createdBy  : Option[User] = None
) {
  val formatter = DateTimeFormat.forPattern("YYYY-MM-dd hh:mm")
  val isoFormatter = ISODateTimeFormat.dateTime()

  def timeFromNow: String = {
    val i = DateTime.now to startDate
    val p = i.toPeriod
    val d = i.toDuration

    "in " + d.getStandardDays + " days, " + p.hours + " hours, " + p.minutes + " minutes"
  }

  def displayDate: String = "was " + formatter.print(startDate)

  def asAccepted: Conference =
    Conference(id, title, abstr, speaker, startDate, length, organizedBy, location, accepted = true, None, priv, forGroup, logoId, createdBy = None)

  def withId(newId: Long): Conference =
    Conference(newId, title, abstr, speaker, startDate, length, organizedBy, location, accepted, acceptCode, priv, forGroup, logoId, createdBy)

  def isInFuture: Boolean = startDate > DateTime.now

  def isAccessibleBy(maybeUser: Option[User]) = !priv || (maybeUser.map(_.role) match {
      case Some(Administrator)                               => true
      case Some(_) if maybeUser.get.lab.id == organizedBy.id => true
      case _                                                 => false
    })

  def dateDisplayFormat = if(isInFuture) timeFromNow else displayDate

  def save = Conference.save(this)

  def destroy = Conference.destroy(this)

  def toConfEvent: ConferenceEvent =
    ConferenceEvent(title, isoFormatter.print(startDate), isoFormatter.print(startDate + length), routes.ConferenceController.viewConf(id).toString(), "#FFF")
}

object Conference {
  val isoFormatter = ISODateTimeFormat.dateTime()

  val insertQuery = SQL("""
      INSERT INTO Conference(title, abstr, speaker, startDate, length, organizedBy, location, accepted, acceptCode, private, forGroup, logoId, createdBy)
      VALUES ({title}, {abstr}, {speaker}, {startDate}, {length}, {organizedBy}, {location}, {accepted}, {acceptCode}, {private}, {forGroup}, {logoId}, {createdBy})
  """)

  val updateQuery = SQL("""
      UPDATE Conference
      SET title = {title}, abstr = {abstr}, speaker = {speaker}, startDate = {startDate},
        length = {length}, organizedBy = {organizedBy}, location = {location}, accepted = {accepted}, acceptCode = {acceptCode}, private = {private}, forGroup = {forGroup}, logoId = {logoId}, createdBy = {createdBy}
      WHERE id = {id}
  """)

  def fixtures = Set (
    Conference(-1, "DNA will explain every adult behaviour", "Discover how the new discoveries about lactose regulation in E.Coli will be over-interpreted for generations to come !", Speaker.listAll(0), DateTime.now - 2.week, 1.hour, Lab.listAll(0), Location.listAll(0), true, None, true),
    Conference(-1, "DNA methylations will explain every adult behaviour", "Discover how the new discoveries about DNA expression regulation via histone-methylation will be over-interpreted for generations to come !", Speaker.listAll(2), DateTime.now + 1.week, 2.hour, Lab.listAll(0), Location.listAll(1), true, None, true),
    Conference(-1, "Why Rosalyn Franklin really didn't deserve the Nobel prize", "After all, who wants women in science ? It's not as if they could do important work, like discovering viruses !", Speaker.listAll(1), DateTime.now + 2.week, 2.hour, Lab.listAll(1), Location.listAll(0), true, None, false),
    Conference(-1, "I have a Nobel, I can say anything I want, people will listen", "We are being lied to ! HIV doesn't cause AIDS, climate change isn't real, and astrology from Elle is a better predictor than epigenetics !", Speaker.listAll(3), DateTime.now + 3.week, 2.hour, Lab.listAll(0), Location.listAll(1), false, Some(Crypto.generateToken), false)
  )

  def findById(id: Long): Option[Conference] = DB.withConnection{implicit c =>
    SQL("SELECT * FROM Conference WHERE id = {id}")
      .on("id" -> id)
      .as(conferenceParser.singleOpt)
  }

  def findConfsAllowableBy(user: User): List[Conference] =
    findNotAccepted.filter(user.role == Administrator || user.lab == _.organizedBy)
      .sortBy(_.startDate)

  // Passing None as the argument to this function means the user is a Guest
  def findAccepted(viewableBy: Option[User]): List[Conference] = DB.withConnection { implicit c =>
    viewableBy.map(_.role) match {
      case Some(Administrator) => SQL("SELECT * FROM Conference WHERE accepted = true").as(conferenceParser *)
      case Some(_)             => {
        SQL("SELECT * FROM Conference WHERE accepted = true AND (organizedBy = {labId} OR private = false OR forGroup IN (SELECT id_group FROM IndexLabGroup WHERE id_lab = {labId}))")
          .on("labId" -> viewableBy.get.lab.id)
          .as(conferenceParser *)
      }
      case None                => SQL("SELECT * FROM Conference WHERE accepted = true AND private = false").as(conferenceParser *)
    }
  }


  def findAcceptedWithFilter(viewableBy: Option[User], filter: String) = DB.withConnection { implicit c =>
    val query = """SELECT c.* FROM Conference c
                  |JOIN Speaker s ON c.speaker = s.id
                  |JOIN Lab l ON c.organizedBy = l.id
                  |WHERE c.accepted = true AND
                  |(lower(c.title) LIKE {filter}
                  |OR lower(s.firstName) LIKE {filter}
                  |OR lower(s.lastName) LIKE {filter}
                  |OR lower(c.abstr) LIKE {filter}
                  |OR lower(l.acronym) LIKE {filter}
                  |OR lower(l.name) LIKE {filter})""".stripMargin

    val wideFilter = "%" + filter.toLowerCase + "%"
    val adminQuery = SQL(query)
      .on("filter" -> wideFilter)

    val userQuery = SQL(query + "AND (c.organizedBy = {organizerId} OR c.private = false OR forGroup IN (SELECT id_group FROM IndexLabGroup WHERE id_lab = {labId}))")
      .on("filter" -> wideFilter)

    val guestQuery = SQL(query + "AND private = false")
      .on("filter" -> wideFilter)

    viewableBy.map(_.role) match {
      case Some(Administrator) => adminQuery.as(conferenceParser *)
      case Some(_)             => userQuery.on("organizerId" -> viewableBy.get.lab.id)
                                    .as(conferenceParser *)
      case None                =>guestQuery.as(conferenceParser *)
    }
  }

  def findNotAccepted: List[Conference] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM Conference WHERE accepted = false")
      .as(conferenceParser *)
  }

  def findVisibleByLabBetween(lab: Lab, startPeriod: DateTime, endPeriod: DateTime): List[Conference] = DB.withConnection {implicit c =>
    SQL(
      "SELECT * FROM Conference WHERE (organizedBy = {labId} OR forGroup IN (SELECT id_group FROM IndexLabGroup WHERE id_lab = {labId}))  AND accepted = true AND startDate BETWEEN {startPeriod} AND {endPeriod}")
      .on("labId" -> lab.id,
          "startPeriod" -> startPeriod,
          "endPeriod" -> endPeriod)
      .as(conferenceParser *)
  }

  def listOrganizedBy(lab: Long): List[Conference] = DB.withConnection {implicit c =>
    SQL(
      "SELECT * FROM Conference WHERE organizedBy = {labId}")
      .on("labId" -> lab)
      .as(conferenceParser *)
  }

  def findPublicBetween(startPeriod: DateTime, endPeriod: DateTime): List[Conference] = DB.withConnection {implicit c =>
    SQL(
      "SELECT * FROM Conference WHERE accepted = true AND private = false AND startDate BETWEEN {startPeriod} AND {endPeriod}")
      .on("startPeriod" -> startPeriod,
        "endPeriod" -> endPeriod)
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
        "location"    -> conf.location.id,
        "accepted" -> conf.accepted,
        "acceptCode" -> conf.acceptCode,
        "private" -> conf.priv,
        if(conf.forGroup.isDefined){"forGroup" -> conf.forGroup.get.id}else{"forGroup" -> None},
        if(conf.logoId.isDefined){"logoId" -> conf.logoId.get}else{"logoId" -> None},
        if(conf.createdBy.isDefined){"createdBy" -> conf.createdBy.get.id}else{"createdBy" -> None}
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
        "location" -> conf.location.id,
        "accepted" -> conf.accepted,
        "acceptCode" -> conf.acceptCode,
        "private" -> conf.priv,
        if(conf.forGroup.isDefined){"forGroup" -> conf.forGroup.get.id}else{"forGroup" -> None},
        if(conf.logoId.isDefined){"logoId" -> conf.logoId.get}else{"logoId" -> None},
        if(conf.createdBy.isDefined){"createdBy" -> conf.createdBy.get.id}else{"createdBy" -> None}
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

  def destroyAfterTenYearPeriod() = DB.withConnection { implicit c =>
    SQL("DELETE FROM Conference WHERE datediff(startDate,now())<-3653;").executeUpdate()
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
        WHERE accepted = true
        AND  startDate BETWEEN {startDate} AND {endDate}
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
    get[Long]("location") ~
    get[Boolean]("accepted") ~
    get[Option[String]]("acceptCode") ~
    get[Boolean]("private") ~
    get[Option[Long]]("forGroup") map {
      case id ~ title ~ abstr ~ speaker ~ startDate ~ length ~ organizedBy ~ location ~ accepted ~ acceptCode ~ priv ~ forGroup =>
        Conference(id, title, abstr, Speaker.findById(speaker).get,
          startDate, new Duration(length), Lab.findById(organizedBy).get, Location.findById(location).get, accepted, acceptCode, priv, LabGroup.findById(forGroup.getOrElse(0)))
    }
  }

  def fromSimpleConference(conf: SimpleConference, logoId: Option[String] = None, createdBy: Option[User] = None): Conference = {
    val speaker =
      if(conf.speaker.speakerId != -1) Speaker.findById(conf.speaker.speakerId).get
      else {
      Speaker(-1, conf.speaker.firstName.get, conf.speaker.lastName.get, conf.speaker.speakerTitle.get,
        conf.speaker.team.get, conf.speaker.organisation.get, conf.speaker.email.get).save.get
      }

    val location =
      if(conf.location.locationId != -1) Location.findById(conf.location.locationId).get
      else {
        Location(-1, conf.location.instituteName.get, conf.location.buildingName, conf.location.roomDesignation.get, conf.location.floor.get,
        conf.location.streetName.get, conf.location.streetNb.get, conf.location.city.get).save.get
      }

    Conference(-1, conf.title, conf.abstr, speaker, conf.date + conf.time,
      conf.length, Lab.findById(conf.organizerId).get, location, accepted = false, Some(Crypto.generateToken), priv = conf.priv, None, logoId, createdBy)
  }

  def modifyFromSimpleConference(oldConf: Conference, conf: SimpleConference, logoId: Option[String] = None, createdBy: Option[User] = None){
    val speaker =
      if(conf.speaker.speakerId != -1) Speaker.findById(conf.speaker.speakerId).get
      else {
        Speaker(-1, conf.speaker.firstName.get, conf.speaker.lastName.get, conf.speaker.speakerTitle.get,
          conf.speaker.team.get, conf.speaker.organisation.get, conf.speaker.email.get).save.get
      }

    val location =
      if(conf.location.locationId != -1) Location.findById(conf.location.locationId).get
      else {
        Location(-1, conf.location.instituteName.get, conf.location.buildingName, conf.location.roomDesignation.get, conf.location.floor.get,
          conf.location.streetName.get, conf.location.streetNb.get, conf.location.city.get).save.get
      }

    oldConf.title = conf.title
    oldConf.abstr = conf.abstr
    oldConf.speaker = speaker
    oldConf.startDate = conf.date + conf.time
    oldConf.length = conf.length
    oldConf.organizedBy = Lab.findById(conf.organizerId).get
    oldConf.location = location
    oldConf.accepted = false
    oldConf.acceptCode = Some(Crypto.generateToken)
    oldConf.priv = conf.priv
    oldConf.forGroup = None
    oldConf.logoId = logoId
    oldConf.createdBy = createdBy
  }

  def exportAllConfToCSV(labId : Option[Long] = None) : File = {
    val dir = new File("temporary_files");
    if(!dir.exists){
      dir.mkdirs();
    }

    if(labId.isDefined){
      val file = new File(dir.getPath+File.separator+"Conferences_export_Lab_"+labId.get+".csv")

      val writer = CSVWriter.open(file)

      writer.writeRow(List("id", "organizedBy", "priv", "title", "speaker", "abstr", "startDate", "accepted", "forGroup"))

      for(conf <- listOrganizedBy(labId.get)){
        if(conf.forGroup.isDefined){
          writer.writeRow(List(conf.id,conf.organizedBy.name,conf.priv, conf.title, conf.speaker.fullName, conf.abstr, conf.startDate, conf.accepted, conf.forGroup))
        }else{
          writer.writeRow(List(conf.id,conf.organizedBy.name,conf.priv, conf.title, conf.speaker.fullName, conf.abstr, conf.startDate, conf.accepted))
        }
      }

      writer.close()

      file
    }else{
      val file = new File(dir.getPath+File.separator+"Conferences_export_all.csv")

      val writer = CSVWriter.open(file)

      writer.writeRow(List("id", "organizedBy", "priv", "title", "speaker", "abstr", "startDate", "accepted", "forGroup"))

      for(conf <- listAll){
        if(conf.forGroup.isDefined){
          writer.writeRow(List(conf.id,conf.organizedBy.name,conf.priv, conf.title, conf.speaker.fullName, conf.abstr, conf.startDate, conf.accepted, conf.forGroup))
        }else{
          writer.writeRow(List(conf.id,conf.organizedBy.name,conf.priv, conf.title, conf.speaker.fullName, conf.abstr, conf.startDate, conf.accepted))
        }
      }

      writer.close()

      file
    }

  }
}

