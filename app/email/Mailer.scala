package email

import akka.actor.Actor
import play.api.{Logger, Play}
import org.apache.commons.mail.HtmlEmail
import play.api.Play.current

case class SendMail(to: String, subject:String, htmlContent: String)

class Mailer extends Actor {
  val smtpServerHostname = Play.application.configuration.getString("mail.smtp.host")
  val smtpServerUsername = Play.application.configuration.getString("mail.smtp.user")
  val smtpServerPassword = Play.application.configuration.getString("mail.smtp.pass")
  val smtpServerPort     = Play.application.configuration.getInt("mail.smtp.port")
  val from               = Play.application.configuration.getString("mail.from")

  override def receive = {
    case SendMail(to, subject, html) => {
      if(smtpServerHostname.isEmpty) {
        Logger.error("EMAIL: Tried to send a mail without setting an smtp server")
      } else {
        val email = new HtmlEmail
        email.setHostName(smtpServerHostname.get)
        smtpServerPort.map(email.setSmtpPort)

        if(smtpServerPassword.isEmpty || smtpServerUsername.isEmpty) {
          Logger.warn("EMAIL: No username or password set for the smtp server, trying to send without authentication")
        } else {
          email.setAuthentication(smtpServerUsername.get, smtpServerPassword.get)
        }
        email.setFrom(from.getOrElse("noreply@confscheduler.com"))
        email.addTo(to)
        email.setSubject(subject)
        email.setHtmlMsg(html)
        email.send()
      }
    }
  }
}
