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

  def save = Speaker.save(this)
}

object Speaker {
  def fixtures = Set(
    Speaker(-1, "Jaques", "Monod", "Pr", "Nobel Prizes Winners", "Institut Pasteur", "jaques-monod-is-a-god@gmail.com"),
    Speaker(-1, "James", "Watson", "Pr", "Nobel Prizes Winners", "Institut Pasteur", "eugenism-isnt-bad@gmail.com"),
    Speaker(-1, "Robin", "Holliday", "Pr", "Nobel Prize should-be-winners", "Cambridge University", "love-methylations@gmail.com"),
    Speaker(-1, "Kary", "Mullis", "Pr", "People we want to take the Nobel Prize back from", "University of California Berkeley", "I-like-wearing-tinfoil-hats@gmail.com")
  )

  private val insertQuery = SQL("""
        INSERT INTO Speaker(firstName, lastName, title, team, organisation, email)
        VALUES({firstName}, {lastName}, {title}, {team}, {organisation}, {email});
    """)

  private val updateQuery = SQL("""
        UPDATE Speaker
        SET firstName = {firstName}, lastName = {lastName}, title = {title}, team = {team}, organisation = {organisation}, email = {email}
        WHERE id = {id}
   """)

  def seedDB() = DB.withConnection {implicit c =>
    for(speaker <- fixtures) {
      speaker.save
    }
  }

  def save(speaker: Speaker): Option[Long] = DB.withConnection { implicit c =>
    if(findById(speaker.id).isDefined) {
      updateQuery
        .on(
          "id"        -> speaker.id,
          "firstName" -> speaker.firstName,
          "lastName" -> speaker.lastName,
          "title"    -> speaker.title,
          "team"     -> speaker.team,
          "organisation" -> speaker.organisation,
          "email"    -> speaker.email
        ).executeUpdate()

      Option(speaker.id)
    } else {
      insertQuery
        .on(
          "firstName" -> speaker.firstName,
          "lastName" -> speaker.lastName,
          "title"    -> speaker.title,
          "team"     -> speaker.team,
          "organisation" -> speaker.organisation,
          "email"    -> speaker.email
        ).executeInsert()
    }
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