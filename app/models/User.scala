package models

sealed trait UserRole
case object Administrator extends UserRole
case object Moderator     extends UserRole
case object Contributor   extends UserRole

abstract class User(val displayName: String)
case class LoggedUser(id: Long, firstName: String, lastName: String, email: String, hashedPass: String, role:UserRole)
  extends User(firstName + " " + lastName)
case class GuestUser() extends User("guest")

object LoggedUser {
  val fixtures = Set(LoggedUser(0, "Rosalyn", "Franklin", "rosa@gmail.com", "123456789", Administrator),
                     LoggedUser(1, "James", "Watson", "jimmy@gmail.com", "987654321", Moderator))

  def findById(id: Long): Option[LoggedUser] = fixtures.find(_.id == id)

  def findByEmail(email: String): Option[LoggedUser] = fixtures.find(_.email == email)

  def authenticate(email: String, password: String): Option[LoggedUser] = findByEmail(email).filter(_.hashedPass == password)
}