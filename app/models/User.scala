package models

sealed trait UserRole
case object Administrator extends UserRole
case object Moderator     extends UserRole
case object Contributor   extends UserRole
case object Guest         extends UserRole

case class User(id: Long, firstName: String, lastName: String, email: String, hashedPass: String, role:UserRole) {
  def canAllowConfs: Boolean = role == Administrator || role == Moderator
}

object User {
  val fixtures = Set(User(0, "Rosalyn", "Franklin", "rosa@gmail.com", "123456789", Administrator),
                     User(1, "James", "Watson", "jimmy@gmail.com", "987654321", Moderator),
                     User(2, "Thomas", "P", "tom@gmail.com", "123456789", Contributor))

  def findById(id: Long): Option[User] = fixtures.find(_.id == id)

  def findByEmail(email: String): Option[User] = fixtures.find(_.email == email)

  def authenticate(email: String, password: String): Option[User] = findByEmail(email).filter(_.hashedPass == password)
}