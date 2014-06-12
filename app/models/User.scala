package models

sealed trait UserRole
case object Administrator extends UserRole
case object Moderator     extends UserRole
case object Contributor   extends UserRole

abstract class User(displayName: String)
case class LoggedUser(id: Long, firstName: String, lastName: String, email: String, hashedPass: String) extends User(firstName + " " + lastName)
case class GuestUser() extends User("guest")

object LoggedUser {
  val fixtures = Set(LoggedUser(0, "Rosalyn", "Franklin", "rosa@gmail.com", "123456789"),
                     LoggedUser(1, "James", "Watson", "jimmy@gmail.com", "987654321"))

  def findById(id: Long): Option[LoggedUser] = fixtures.find(_.id == id)

  def findByEmail(email: String): Option[User] = fixtures.find(_.email == email)
}