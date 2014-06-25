package models

case class Lab (
  id     : Long,
  acronym: String,
  name   : String
)

object Lab {
  var fixtures = Set(Lab(0, "CIRI", "Centre de truc plutot cools"), Lab(1, "POUET", "Le centre des poetes"))

  def findById(id: Long): Option[Lab] = fixtures.find(_.id == id)
}
