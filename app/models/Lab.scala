package models

import anorm.SqlParser._
import controllers.LabController.SimpleLab
import anorm._
import play.api.db.DB
import play.api.Play.current

case class Lab (
  id     : Long,
  acronym: String,
  name   : String,
  logoId : Option[String]
) {
  def destroy() = Lab.destroy(this)

  def save() = Lab.save(this)
}

object Lab {
  private val insertQuery = SQL("""
      INSERT INTO Lab(acronym, name, logoId)
      VALUES ({acronym}, {name}, {logoId})
    """)

  private val updateQuery = SQL("""
    UPDATE Lab
    SET acronym = {acronym}, name = {name}, logoId = {logoId}
    WHERE id = {id}
    """)

  def fixtures = Set(Lab(-1, "CIRI", "International Center for Infectiology Research", None), Lab(-1, "RO", "Retroviral Oncogenesis", None))

  def findById(id: Long): Option[Lab] = DB.withConnection { implicit  c =>
    SQL("SELECT * FROM Lab WHERE id = {id}")
      .on("id" -> id)
      .as(labParser.singleOpt)
  }

  def listVisible(user: User): List[Lab] = user.role match {
    case Administrator => listAll
    case _             => List(user.lab)
  }

  def fromSimpleLab(lab: SimpleLab, logoId: Option[String]): Option[Lab] = {
    Option(Lab(-1, lab.acronym, lab.name, logoId))
  }

  def save(lab: Lab): Option[Long] = DB.withConnection { implicit c =>
    if(findById(lab.id).isDefined) {
      updateQuery
        .on(
          "acronym" -> lab.acronym,
          "name" -> lab.name,
          "id" -> lab.id.toString,
          "logoId" -> lab.logoId)
        .executeUpdate()

      Option(lab.id)
    } else {
      insertQuery
        .on(
          "acronym" -> lab.acronym,
          "name" -> lab.name,
          "logoId" -> lab.logoId)
        .executeInsert()
    }
  }

  def listAll: List[Lab] = DB.withConnection {implicit c =>
    SQL("SELECT * FROM Lab")
      .as(labParser *)
  }

  def count: Long = DB.withConnection{implicit c =>
    SQL("SELECT count(*) FROM Lab")
      .as(scalar[Long].single)
  }

  def destroy(lab: Lab) = DB.withConnection {implicit c =>
    SQL("DELETE FROM Lab WHERE id = {id}")
      .on("id" -> lab.id)
      .executeUpdate()
  }

  def destroyAll(): Unit = DB.withConnection {implicit c =>
    SQL("DELETE FROM Lab").executeUpdate()
  }

  def seedDB(): Unit = DB.withConnection {implicit c =>
    for(lab <- fixtures) {
      lab.save()
    }
  }

  private val labParser: RowParser[Lab] = {
      get[Long]("id") ~
      get[String]("acronym") ~
      get[String]("name") ~
      get[Option[String]]("logoId") map {
      case id ~ acronym ~ name ~ logoId => Lab(id, acronym, name, logoId)
    }
  }
}
