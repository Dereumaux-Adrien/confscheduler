package models

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
    val fixtures = Set(
        Speaker(0, "Jaques", "Monod", "Pr", "Nobel Prizes Winners", "Institut Pasteur", "jaques-monod-is-a-good@gmail.com"),
        Speaker(1, "François", "Jacob", "Pr", "Nobel Prizes Winners", "Institut Pasteur", "jaques-monod-is-a-bad@gmail.com"))

    def find(id: Long) = fixtures.find(_.id == id)
}