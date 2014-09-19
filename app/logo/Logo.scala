package logo

import java.io.File

import com.sksamuel.scrimage.{Format, Image}
import play.api.Play
import play.api.libs.Crypto
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class Logo private(tempFile: FilePart[TemporaryFile]) {
  val savePath = Play.configuration.getString("application.imageSavePath").getOrElse(System.getenv("HOME") + "/.confscheduler/logos/")
  val logoId = Crypto.generateToken

  def save(): Future[Unit] = {
    val logoFile = new File(savePath + logoId + ".jpg")
    Future(Image(tempFile.ref.file).fit(400, 200).writer(Format.JPEG).withCompression(95).write(logoFile))
  }
}

object Logo {
  def apply(tempFile: FilePart[TemporaryFile]): Option[Logo] = {
    def isImage(file: FilePart[TemporaryFile]) = file.contentType.exists(t => t.equals("image/jpeg") || t.equals("image/png"))
    def isUnder4Mb(file: FilePart[TemporaryFile]) = file.ref.file.length() < 4 * 1024 * 1024

    if(isImage(tempFile) && isUnder4Mb(tempFile)) Some(new Logo(tempFile))
    else                                          None
  }

  def find(id: String) = new File(Play.configuration.getString("application.imageSavePath").getOrElse(System.getenv("HOME") + "/.confscheduler/logos/" + id + ".jpg"))
}
