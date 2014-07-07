package models

import anorm.SqlParser._
import controllers.LabController.SimpleLab
import anorm._
import play.api.db.DB
import play.api.Play.current

case class Lab (
  id     : Long,
  acronym: String,
  name   : String
) {
  def save = Lab.save(this)
}

object Lab {
  private val insertQuery = SQL("""
      INSERT INTO Lab(acronym, name)
      VALUES ({acronym}, {name})
    """)

  private val updateQuery = SQL("""
    UPDATE Lab
    SET acronym = {acronym}, name = {name}
    WHERE id = {id}
    """)

  def fixtures = Set(Lab(0, "CIRI", "Centre de truc plutot cools"), Lab(1, "POUET", "Le centre des poetes"))

  def findById(id: Long): Option[Lab] = DB.withConnection { implicit  c =>
    SQL("SELECT * FROM Lab WHERE id = {id}")
      .on("id" -> id.toString)
      .as(labParser.singleOpt)
  }

  def fromSimpleLab(lab: SimpleLab): Option[Lab] = {
    Option(Lab(-1, lab.acronym, lab.name))
  }

  def save(lab: Lab): Option[Long] = DB.withConnection { implicit c =>
    if(findById(lab.id).isDefined) {
      updateQuery
        .on("acronym" -> lab.acronym, "name" -> lab.name, "id" -> lab.id.toString)
        .executeUpdate()

      Option(lab.id)
    } else {
      insertQuery
        .on("acronym" -> lab.acronym, "name" -> lab.name)
        .executeInsert()
    }
  }

  def listAll: List[Lab] = DB.withConnection {implicit c =>
    SQL("SELECT * FROM Lab")
      .as(labParser *)
  }

  def destroyAll(): Unit = DB.withConnection {implicit c =>
    SQL("DELETE FROM Lab").executeUpdate()
  }

  def seedDB(): Unit = DB.withConnection {implicit c =>
    for(lab <- fixtures) {
      lab.save
    }
  }

  private val labParser: RowParser[Lab] = {
      get[Long]("id") ~
      get[String]("acronym") ~
      get[String]("name")  map {
      case id ~ acronym ~ name => Lab(id, acronym, name)
    }
  }
}
