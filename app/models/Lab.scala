package models

import anorm.SqlParser._
import controllers.LabController.SimpleLab
import anorm._
import java.sql.SQLException
import play.api.Logger
import play.api.db.DB
import play.api.Play.current

case class Lab (
  id     : Long,
  acronym: String,
  name   : String,
  email  : String,
  logoId : Option[String]
) {
  def destroy() = Lab.destroy(this)

  def save() = Lab.save(this)

  def withId(newId: Long): Lab = Lab(newId, acronym, name, email, logoId)
}

object Lab {
  private val insertQuery = SQL("""
      INSERT INTO Lab(acronym, name, email, logoId)
      VALUES ({acronym}, {name}, {email}, {logoId})
    """)

  private val updateQuery = SQL("""
    UPDATE Lab
    SET acronym = {acronym}, name = {name}, email = {email}, logoId = {logoId}
    WHERE id = {id}
    """)

  def fixtures = Set(Lab(-1, "CIRI", "International Center for Infectiology Research", "mailCIRI@gmail.com", None),
                     Lab(-1, "RO", "Retroviral Oncogenesis", "mailRO@gmail.com", None))

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
    Option(Lab(-1, lab.acronym, lab.name, lab.email, logoId))
  }

  def save(lab: Lab): Option[Lab] = DB.withConnection { implicit c =>
    if(findById(lab.id).isDefined) {
      updateQuery
        .on(
          "id" -> lab.id.toString,
          "acronym" -> lab.acronym,
          "name" -> lab.name,
          "email" -> lab.email,
          "logoId" -> lab.logoId)
        .executeUpdate()

      Option(lab)
    } else {
      val newId: Option[Long] = insertQuery
        .on(
          "acronym" -> lab.acronym,
          "name" -> lab.name,
          "email" -> lab.email,
          "logoId" -> lab.logoId)
        .executeInsert()
      newId.map(lab.withId)
    }
  }

  def filteredWith(filter: String): List[Lab] = DB.withConnection {implicit c =>
    val wideFilter = "%" + filter.toLowerCase + "%"
    SQL("SELECT * FROM Lab WHERE lower(name) LIKE {filter} OR lower(acronym) LIKE {filter}")
      .on("filter" -> wideFilter)
      .as(labParser *)
  }

  def listAll: List[Lab] = DB.withConnection {implicit c =>
    SQL("SELECT * FROM Lab")
      .as(labParser *)
  }

  def count: Long = DB.withConnection{implicit c =>
    SQL("SELECT count(*) FROM Lab")
      .as(scalar[Long].single)
  }

  def destroy(lab: Lab):Boolean = DB.withConnection {implicit c =>
    try {
      SQL("DELETE FROM Lab WHERE id = {id}")
        .on("id" -> lab.id)
        .executeUpdate()
      true
    } catch {
      case e: SQLException
        if e.getMessage.contains("update or delete on table \"lab\" violates foreign key constraint \"conference_organizedby_fkey\" on table \"conference\"") |
           e.getMessage.contains("update or delete on table \"lab\" violates foreign key constraint \"appuser_lab_fkey\" on table \"appuser\"")
        => Logger.warn(e.getMessage); false
    }
  }

  def destroyAll(): Unit = DB.withConnection {implicit c =>
    SQL("DELETE FROM Lab").executeUpdate()
  }

  def seedDB(): Unit = DB.withConnection {implicit c =>
    for(lab <- fixtures) {
      lab.save()
    }
  }

  def findLabToAddToLabGroup(idLabGroup: Long, filter: String = ""): List[Lab] = DB.withConnection {implicit c =>
    val wideFilter = "%" + filter.toLowerCase + "%"
    SQL("SELECT * FROM Lab WHERE id NOT IN (SELECT l.id FROM Lab l JOIN IndexLabGroup i on l.id=i.id_lab WHERE i.id_group={id}) AND (lower(name) LIKE {filter} OR lower(acronym) LIKE {filter})")
      .on("filter" -> wideFilter, "id" -> idLabGroup)
      .as(Lab.labParser *)
  }

  def findLabToRemoveFromLabGroup(idLabGroup: Long, filter: String = ""): List[Lab] = DB.withConnection {implicit c =>
    val wideFilter = "%" + filter.toLowerCase + "%"
    SQL("SELECT l.* FROM Lab l JOIN IndexLabGroup i on l.id=i.id_lab WHERE i.id_group={id} AND (lower(name) LIKE {filter} OR lower(acronym) LIKE {filter})")
      .on("filter" -> wideFilter, "id" -> idLabGroup)
      .as(Lab.labParser *)
  }

  private val labParser: RowParser[Lab] = {
      get[Long]("id") ~
      get[String]("acronym") ~
      get[String]("name") ~
      get[String]("email") ~
      get[Option[String]]("logoId") map {
      case id ~ acronym ~ name ~ email ~ logoId => Lab(id, acronym, name, email, logoId)
    }
  }

}
