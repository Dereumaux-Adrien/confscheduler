package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.Play.current

case class Location (
  id             : Long,
  instituteName  : String,
  buildingName   : Option[String],
  roomDesignation: String,
  floor          : String,
  streetName     : String,
  streetNb       : Int,
  city           : String
) {

  def withId(newId: Long): Location =
    Location(newId, instituteName, buildingName, roomDesignation, floor, streetName, streetNb, city)

  def save = Location.save(this)
}

object Location {
  val insertQuery = SQL("""
      INSERT INTO Location(instituteName, buildingName, roomDesignation, floor, streetName, streetNb, city)
      VALUES ({instituteName}, {buildingName}, {roomDesignation}, {floor}, {streetName}, {streetNb}, {city})
  """)

  val updateQuery = SQL("""
      UPDATE locerence
      SET instituteName = {instituteName}, buildingName = {buildingName}, roomDesignation = {roomDesignation},
        floor = {floor}, streetName = {streetName}, streetNb = {streetNb}, city = {city}
      WHERE id = {id}
  """)

  def fixtures = Set(
    Location(-1, "Institut Pasteur", None, "301", "3", "45 rue des fleurs", 5, "Paris"),
    Location(-1, "Institut Curie", None, "021", "Ground", "45 rue des chapeaux", 5, "Paris")
  )

  def seedDB(): Unit = {
    for(loc <- fixtures) {
      loc.save
    }
  }

  def listAll: List[Location] = DB.withConnection {implicit c =>
    SQL("SELECT * FROM Location").as(locationParser *)
  }

  def destroyAll(): Unit = DB.withConnection { implicit c =>
    SQL("DELETE FROM Location").executeUpdate()
  }

  def count: Long = DB.withConnection{implicit c =>
    SQL("SELECT count(*) FROM Location")
      .as(scalar[Long].single)
  }

  def findById(id: Long): Option[Location] = DB.withConnection{implicit c =>
    SQL("SELECT * FROM Location WHERE id = {id}")
      .on("id" -> id)
      .as(locationParser.singleOpt)
  }

  def save(loc: Location): Option[Location] = DB.withConnection { implicit c =>
    if(findById(loc.id).isDefined) {
      updateQuery.on(
        "id" -> loc.id,
        "instituteName" -> loc.instituteName,
        "buildingName" -> loc.buildingName,
        "roomDesignation" -> loc.roomDesignation,
        "floor" -> loc.floor,
        "streetName" -> loc.streetName,
        "streetNb" -> loc.streetNb,
        "city" -> loc.city
      ).executeUpdate()
      Option(loc)
    } else {
      val newId: Option[Long] = insertQuery.on(
        "instituteName" -> loc.instituteName,
        "buildingName" -> loc.buildingName,
        "roomDesignation" -> loc.roomDesignation,
        "floor" -> loc.floor,
        "streetName" -> loc.streetName,
        "streetNb" -> loc.streetNb,
        "city" -> loc.city
      ).executeInsert()
      newId.map(loc.withId)
    }
  }

  private val locationParser: RowParser[Location] = {
    get[Long]("id") ~
    get[String]("instituteName") ~
    get[Option[String]]("buildingName") ~
    get[String]("roomDesignation") ~
    get[String]("floor") ~
    get[String]("streetName") ~
    get[Int]("streetNb") ~
    get[String]("city") map {
    case id ~ instituteName ~ buildingName ~ roomDesignation ~ floor ~ streetName ~ streetNb ~ city =>
      Location(id, instituteName, buildingName, roomDesignation, floor, streetName, streetNb, city)
    }
  }
}
