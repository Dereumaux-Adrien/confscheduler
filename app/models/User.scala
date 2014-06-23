package models

import play.api.mvc.Cookie
import play.api.libs.Crypto

sealed trait UserRole
case object Administrator extends UserRole
case object Moderator     extends UserRole
case object Contributor   extends UserRole
case object Guest         extends UserRole

case class User(id: Long, firstName: String, lastName: String, email: String, hashedPass: String, role:UserRole, rememberMeToken: String) {
  def withRememberMeToken(token: String): User = User(id, firstName, lastName, email, hashedPass, role, token)

  def canAllowConfs: Boolean = role == Administrator || role == Moderator
}

object User {
  var fixtures = Set(User(0, "Rosalyn", "Franklin", "rosa@gmail.com", "123456789", Administrator, ""),
                     User(1, "James", "Watson", "jimmy@gmail.com", "987654321", Moderator, ""),
                     User(2, "Thomas", "P", "tom@gmail.com", "123456789", Contributor, ""))

  def findById(id: Long): Option[User] = fixtures.find(_.id == id)

  def findByEmail(email: String): Option[User] = fixtures.find(_.email == email)

  def findByRememberMe(rememberMeCookie: Option[Cookie]): Option[User] = {
    rememberMeCookie.flatMap(c => fixtures.find(_.rememberMeToken == c.value))
  }

  def save(user: User) = {
    println(fixtures)
    fixtures = fixtures.filterNot(_.id == user.id) + user
    println(fixtures)
  }

  def authenticate(email: String, password: String, rememberMe: Boolean): Option[User] = {
    val user = findByEmail(email).filter(_.hashedPass == password)

    if(rememberMe && user.isDefined) {
      val rememberMeToken = Crypto.generateSignedToken
      val authUser = user.get.withRememberMeToken(rememberMeToken)
      User.save(authUser)
      Some(authUser)
    } else {
      user
    }
  }
}