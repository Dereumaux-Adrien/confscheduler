package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

case class Speaker (
    id           : Long,
    firstName    : String,
    lastName     : String,
    title        : String,
    team         : String,
    organisation : String,
    email        : String
) {
    def fullName: String = title + ". " + firstName + " " + lastName
}

object Speaker {
  private val insertQuery = SQL( """
        INSERT INTO Speaker(firstName, lastName, title, team, organisation, email)
        VALUES({firstName}, {lastName}, {title}, {team}, {organisation}, {email});
    """)

  def seedDB = DB.withConnection {implicit c =>
    insertQuery.on(
        "firstName" -> "Jacques",
        "lastName"  -> "Monod",
        "title"     -> "Pr",
        "team"      -> "Nobel Prizes Winners",
        "organisation" -> "Institut Pasteur",
        "email"     -> "jaques-monod-is-a-good@gmail.com"
      ).executeUpdate()

    insertQuery.on(
        "firstName" -> "François",
        "lastName"  -> "Jacob",
        "title"     -> "Pr",
        "team"      -> "Nobel Prizes Winners",
        "organisation" -> "Institut Pasteur",
        "email"     -> "jaques-monod-is-a-bad@gmail.com"
      ).executeUpdate()
  }

  def findById(id: Long): Option[Speaker] = DB.withConnection {implicit c =>
    SQL("SELECT * FROM Speaker WHERE id = {id};")
      .on("id" -> id.toString)
      .as(speakerParser.singleOpt)
  }

  def listAll: List[Speaker] = DB.withConnection {implicit c =>
    SQL("SELECT * FROM Speaker;")
      .as(speakerParser *)
  }

  def destroyAll(): Unit = DB.withConnection {implicit  c =>
    SQL("DELETE FROM Speaker;").executeUpdate()
  }

  def first: Speaker = DB.withConnection {implicit  c =>
    SQL("SELECT * FROM Speaker")
      .as(speakerParser *)
      .head
  }

  def second: Speaker = DB.withConnection {implicit  c =>
    SQL("SELECT * FROM Speaker")
      .as(speakerParser *)
      .tail
      .head
  }

  private val speakerParser: RowParser[Speaker] = {
      get[Long]("id") ~
      get[String]("firstName") ~
      get[String]("lastName") ~
      get[String]("title") ~
      get[String]("team") ~
      get[String]("organisation") ~
      get[String]("email") map {
        case id ~ firstName ~ lastName ~ title ~ team ~ organisation ~ email => Speaker(id, firstName, lastName, title, team, organisation, email)
      }
  }
}