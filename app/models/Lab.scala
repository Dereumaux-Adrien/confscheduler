package models

import controllers.LabController.SimpleLab

case class Lab (
  id     : Long,
  acronym: String,
  name   : String
) {
  def save = Lab.save(this)
}

object Lab {
  var fixtures = Set(Lab(0, "CIRI", "Centre de truc plutot cools"), Lab(1, "POUET", "Le centre des poetes"))
  var nextId = 1

  def findById(id: Long): Option[Lab] = fixtures.find(_.id == id)

  def fromSimpleLab(lab: SimpleLab): Option[Lab] = {
    val result = Lab(nextId, lab.acronym, lab.name)
    nextId += 1
    Option(result)
  }

  def save(lab: Lab) = fixtures = fixtures + lab
}
