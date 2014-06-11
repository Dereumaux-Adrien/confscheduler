package models

case class Conference (
    id     : Long,
    name   : String,
    abstr  : String,
    speaker: String
)

object Conference {
    val confs = Set(Conference(0, "Les oiseaux chantent", "La vie est belle, et c'est super cool de s'appeller Michel", "Michel Sardou"), 
        Conference(1, "test conf 2", "test abstr 2", "Patrick Bruel"))

    def findAll = confs.toList.sortBy(_.id)

    def find(id: Long) = confs.find(c => c.id == id)
}