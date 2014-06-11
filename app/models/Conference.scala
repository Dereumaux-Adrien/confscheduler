package models
import com.github.nscala_time.time.Imports._

case class Conference (
    id       : Long,
    name     : String,
    abstr    : String,
    speaker  : Speaker,
    startDate: DateTime,
    length   : Duration
) {
    def timeFromNow: String = {
        val i = (DateTime.now to startDate)
        val p = i.toPeriod
        val d = i.toDuration

        d.getStandardDays() + " days, " + p.hours + " hours, " + p.minutes + " minutes."
    }
}

object Conference {
    val confs = Set(Conference(0, "Les oiseaux chantent", "La vie est belle, et c'est super cool de s'appeller Michel", Speaker.find(0).get, DateTime.now + 2.week, 1.hour), 
        Conference(1, "test conf 2", "test abstr 2", Speaker.find(1).get, DateTime.now + 1.week, 2.hour))

    def findAll = confs.toList.sortBy(_.id)

    def find(id: Long) = confs.find(c => c.id == id)
}