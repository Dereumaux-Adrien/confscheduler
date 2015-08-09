package models

/**
 * Created by adrien on 5/12/15.
 */

import anorm.SqlParser._
import controllers.LabGroupController.SimpleLabGroup
import anorm._
import play.api.db.DB
import play.api.Play.current

case class LabGroup (
  id     : Long,
  var name   : String
) {
  def destroy() = LabGroup.destroy(this)

  def save() = LabGroup.save(this)

  def withId(newId: Long): LabGroup = LabGroup(newId, name)
}

object LabGroup {
  private val insertQuery = SQL("""
      INSERT INTO LabGroup(name)
      VALUES ({name})
                                """)

  private val updateQuery = SQL("""
    UPDATE LabGroup
    SET name = {name}
    WHERE id = {id}
                                """)

  def findById(id: Long): Option[LabGroup] = DB.withConnection { implicit  c =>
    SQL("SELECT * FROM LabGroup WHERE id = {id}")
      .on("id" -> id)
      .as(labGroupParser.singleOpt)
  }

  def destroy(labGroup: LabGroup) = DB.withConnection {implicit c =>
    SQL("DELETE FROM LabGroup WHERE id = {id}").on("id" -> labGroup.id).executeUpdate()
  }

  def destroyAll(): Unit = DB.withConnection {implicit c =>
    SQL("DELETE FROM LabGroup").executeUpdate()
  }

  def fromSimpleLabGroup(labGroup: SimpleLabGroup): Option[LabGroup] = {
    Option(LabGroup(-1, labGroup.name))
  }

  def modifyFromSimpleLabGroup(oldGroup: LabGroup, group: SimpleLabGroup){
    oldGroup.name = group.name
  }

  def save(labGroup: LabGroup): Option[LabGroup] = DB.withConnection { implicit c =>
    if(findById(labGroup.id).isDefined) {
      updateQuery
        .on(
          "id" -> labGroup.id.toString,
          "name" -> labGroup.name)
        .executeUpdate()

      Option(labGroup)
    } else {
      val newId: Option[Long] = insertQuery
        .on(
          "name" -> labGroup.name)
        .executeInsert()
      newId.map(labGroup.withId)
    }
  }

  def filteredWith(filter: String): List[LabGroup] = DB.withConnection {implicit c =>
    val wideFilter = "%" + filter.toLowerCase + "%"
    SQL("SELECT * FROM LabGroup WHERE lower(name) LIKE {filter}")
      .on("filter" -> wideFilter)
      .as(labGroupParser *)
  }

  def listAll: List[LabGroup] = DB.withConnection {implicit c =>
    SQL("SELECT * FROM LabGroup")
      .as(labGroupParser *)
  }

  def count: Long = DB.withConnection{implicit c =>
    SQL("SELECT count(*) FROM LabGroup")
      .as(scalar[Long].single)
  }

  private val labGroupParser: RowParser[LabGroup] = {
    get[Long]("id") ~
      get[String]("name") map {
      case id  ~ name => LabGroup(id, name)
    }
  }

  def addLabToGroup(idLabGroup: Long, idLab: Long) = DB.withConnection {implicit c =>
    SQL("""
      INSERT INTO IndexLabGroup
      VALUES ({idLabGroup}, {idLab})
        """)
      .on("idLabGroup" -> idLabGroup, "idLab" -> idLab)
      .executeUpdate()
  }

  def removeLabFromGroup(idLabGroup: Long, idLab: Long) = DB.withConnection {implicit c =>
    SQL("""
      DELETE FROM IndexLabGroup
      WHERE id_group={idLabGroup} AND id_lab={idLab}
        """)
      .on("idLabGroup" -> idLabGroup, "idLab" -> idLab)
      .executeUpdate()
    true
  }

  def findGroupsByLab(idLab: Long): List[LabGroup] = DB.withConnection {implicit c =>
    SQL("SELECT l.* FROM LabGroup l JOIN IndexLabGroup i ON l.id=i.id_group WHERE i.id_lab = {idLab}")
      .on("idLab" -> idLab)
      .as(labGroupParser *)
  }

}
