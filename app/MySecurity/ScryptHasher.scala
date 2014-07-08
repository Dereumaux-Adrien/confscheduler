package MySecurity

import com.lambdaworks.crypto.SCryptUtil

object ScryptHasher {
  val N = 16384
  val r = 8
  val p = 1

  implicit class ScryptString(pass: String) {
    def scrypt: String = SCryptUtil.scrypt(pass, N, r, p)
  }
  def checkPasswd(pass: String, hashed: String): Boolean = SCryptUtil.check(pass, hashed)
}
