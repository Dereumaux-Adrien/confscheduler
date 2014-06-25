package models

import controllers.UserController.SimpleUser
import play.api.mvc.Cookie
import play.api.libs.Crypto

sealed trait UserRole
case object Administrator extends UserRole
case object Moderator     extends UserRole
case object Contributor   extends UserRole
case object Guest         extends UserRole

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
  def withRememberMeToken(token: String): User = User(id, firstName, lastName, email, lab, hashedPass, role, token)

  def canAllowConfs: Boolean = role == Administrator || role == Moderator

  // This is just a convenience method.
  def save: Option[User] = User.save(this)

  def destroy: Boolean = User.destroy(this)
}

object User {
  val loggedUserRoleList = List("Administrator", "Moderator", "Contributor")

  var fixtures = Set(User(0, "Rosalyn", "Franklin", "rosa@gmail.com", Lab.findById(0).get, "123456789", Administrator, ""),
                     User(1, "James", "Watson", "jimmy@gmail.com", Lab.findById(0).get, "987654321", Moderator, ""),
                     User(2, "Thomas", "P", "tom@gmail.com", Lab.findById(1).get, "123456789", Contributor, ""))

  var nextId = 3

  def findById(id: Long): Option[User] = fixtures.find(_.id == id)

  def findByEmail(email: String): Option[User] = fixtures.find(_.email == email)

  def findByRememberMe(rememberMeCookie: Option[Cookie]): Option[User] = {
    rememberMeCookie.flatMap(c => fixtures.find(_.rememberMeToken == c.value))
  }

  def count = fixtures.size

  // returns the saved used in an Option or None if saving failed.
  def save(user: User): Option[User] = {
    fixtures = fixtures.filterNot(_.id == user.id) + user
    Some(user)
  }

  // returns true if the user has been destroyed, false if there was an error
  def destroy(user: User): Boolean = {
    fixtures = fixtures.filterNot(_.id == user.id)
    true
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

    val newUser = User(nextId, user.firstName, user.lastName, user.email, newUserLab, user.password, newUserRole, "")
    nextId += 1
    Option(newUser)
  }
}