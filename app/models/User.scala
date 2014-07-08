package models

import anorm.SqlParser._
import controllers.UserController.SimpleUser
import play.api.db.DB
import play.api.mvc.Cookie
import play.api.libs.Crypto
import anorm._
import play.api.Play.current

sealed trait UserRole {
  def fromInt(i: Int): UserRole = i match {
    case 0 => Administrator
    case 1 => Moderator
    case 2 => Contributor
    case 3 => Guest
  }

  def toInt: Int
}
case object Administrator extends UserRole {
  def toInt = 0
}
case object Moderator     extends UserRole {
  def toInt = 1
}
case object Contributor   extends UserRole {
  def toInt = 2
}
case object Guest         extends UserRole {
  def toInt = 3
}

case class User (
   id: Long,
   firstName: String,
   lastName: String,
   email: String,
   lab: Lab,
   hashedPass: String,
   role:UserRole,
   rememberMeToken: String
){
  def canAllowConf(confId: Long): Boolean = Conference.findById(id).fold(false)(_.id == lab.id) || role == Administrator

  def withRememberMeToken(token: String): User = User(id, firstName, lastName, email, lab, hashedPass, role, token)

  def canAllowConfs: Boolean = role == Administrator || role == Moderator

  def withId(newId: Long): User = User(newId, firstName, lastName, email, lab, hashedPass, role, rememberMeToken)

  // This is just a convenience method.
  def save: Option[User] = User.save(this)

  def destroy: Boolean = User.destroy(this)
}

object User {
  val loggedUserRoleList = List("Administrator", "Moderator", "Contributor")

  def fixtures = Set(User(-1, "Rosalyn", "Franklin", "rosa@gmail.com", Lab.listAll.head, "123456789", Administrator, ""),
                     User(-1, "James", "Watson", "jimmy@gmail.com", Lab.listAll.tail.head, "987654321", Moderator, ""),
                     User(-1, "Thomas", "P", "tom@gmail.com", Lab.listAll.tail.head, "123456789", Contributor, ""))

  val updateQuery = SQL("""
      UPDATE User
      SET firstName = {firstName}, lastName = {lastName}, email = {email}, lab = {lab}, hashedPass = {hashedPass}, role = {role}, rememberMeToken = {rememberMeToken}
      WHERE id = {id}
  """)

  val insertQuery = SQL("""
      INSERT INTO User(firstName, lastName, email, lab, hashedPass, role, rememberMeToken)
      VALUES ({firstName}, {lastName}, {email}, {lab}, {hashedPass}, {role}, {rememberMeToken})
  """)

  def findById(id: Long): Option[User] = DB.withConnection {implicit  c =>
    SQL("SELECT * FROM User WHERE id = {id}")
      .on("id" -> id)
      .as(userParser.singleOpt)
  }

  def findByEmail(email: String): Option[User] = DB.withConnection {implicit  c =>
    SQL("SELECT * FROM User WHERE email = {email}")
      .on("email" -> email)
      .as(userParser.singleOpt)
  }

  def findByRememberMe(rememberMeCookie: Option[Cookie]): Option[User] = DB.withConnection {implicit conn =>
    rememberMeCookie.flatMap {c =>
      SQL("SELECT * FROM User WHERE rememberMeToken = {rememberMeToken}")
        .on("rememberMeToken" -> c.value)
        .as(userParser.singleOpt)
    }
  }

  def count: Long = DB.withConnection{implicit c =>
    SQL("SELECT count(*) FROM User")
      .executeQuery()
      .as(scalar[Long].single)
  }

  // returns the saved used in an Option or None if saving failed.
  def save(user: User): Option[User] = DB.withConnection { implicit c =>
    if(findById(user.id).isDefined) {
      if(updateQuery
        .on(
          "id"        -> user.id,
          "firstName" -> user.firstName,
          "lastName" -> user.lastName,
          "email"    -> user.email,
          "lab"    -> user.lab.id,
          "hashedPass"     -> user.hashedPass,
          "role" -> user.role.toInt,
          "rememberMeToken" -> user.rememberMeToken
        ).execute()) {
        Option(user)
      } else {
        None
      }
    } else {
      val newId: Option[Long] = insertQuery
        .on(
          "firstName" -> user.firstName,
          "lastName" -> user.lastName,
          "email"    -> user.email,
          "lab"    -> user.lab.id,
          "hashedPass"     -> user.hashedPass,
          "role" -> user.role.toInt,
          "rememberMeToken" -> user.rememberMeToken
        ).executeInsert()
      newId.map(user.withId)
    }
  }

  // returns true if the user has been destroyed, false if there was an error
  def destroy(user: User): Boolean = DB.withConnection {implicit c =>
    SQL("DELETE FROM User WHERE id = {id}")
      .on("id" -> user.id)
      .execute()
  }

  def destroyAll(): Boolean = DB.withConnection {implicit c =>
    SQL("DELETE FROM User")
      .execute()
  }

  def authenticate(email: String, password: String, rememberMe: Boolean): Option[User] = {
    val user = findByEmail(email).filter(_.hashedPass == password)

    if(rememberMe && user.isDefined) {
      val rememberMeToken = Crypto.generateSignedToken
      val authUser = user.get.withRememberMeToken(rememberMeToken)
      authUser.save
    } else {
      user
    }
  }

  def fromSimpleUser(user: SimpleUser): Option[User] = {
    val newUserRole = user.newUserRole match {
      case "Administrator" => Administrator
      case "Moderator"     => Moderator
      case "Contributor"   => Contributor
      case _               => return None
    }

    val newUserLab = Lab.findById(user.labId).getOrElse(return None)

    val newUser = User(-1, user.firstName, user.lastName, user.email, newUserLab, user.password, newUserRole, "")
    Option(newUser)
  }

  def seedDB(): Unit = {
    for(user <- fixtures) {
      user.save
    }
  }

  private val userParser: RowParser[User] = {
    get[Long]("id") ~
    get[String]("firstName") ~
    get[String]("lastName") ~
    get[String]("email") ~
    get[Long]("lab") ~
    get[String]("hashedPass") ~
    get[Int]("role") ~
    get[String]("rememberMeToken") map {
      case id ~ firstName ~ lastName ~ email ~ lab ~ hashedPass ~ role ~ rememberMeToken =>
        User(id, firstName, lastName, email, Lab.findById(lab).get, hashedPass, Administrator.fromInt(role), rememberMeToken)
    }
  }
}