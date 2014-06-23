package MySecurity

import scala.util.Random

object SecurityHelper {
  val UIDLength = 40
  
  def UIDGenerator: String = new Random().alphanumeric.take(UIDLength).mkString("")
}
