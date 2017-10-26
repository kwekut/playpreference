package models.mail
//libraryDependencies += "com.sun.mail" % "javax.mail" % "1.5.5"
import java.util.Properties

import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.util.ArrayList
import java.util.Arrays
import play.api.Logger
import com.typesafe.config.ConfigFactory
import scala.util.Try
import scala.util.{Success, Failure}
import play.twirl.api.Html
import play.api.libs.mailer._
import java.io.File
import org.apache.commons.mail.EmailAttachment
import play.api.libs.mailer.{ Email, MailerClient }
import models._

object SendEmail {
  val c = ConfigFactory.load()
  c.checkValid(ConfigFactory.defaultReference(), "consumer")
      val to = c.getStringList("email.recievers")
      val from: String = c.getString("email.from")
      val mailer = new SMTPMailer(SMTPConfiguration("typesafe.org", 1234))
}

case class SendEmail(sbj: String, omsg: String) {
  import SendEmail._
  val logger = Logger(this.getClass())
  val msg = Mail(sbj, omsg).mail
 
  def send() = Try { 
      val email = Email(
        (sbj + ": " + AppName.appname),
        (s"<$from>"),
        ((to.toSeq.map{t=> s"<$t>"}).toSeq),
        bodyHtml = Some(msg)
      )
      mailer.send(email)
  } match {
        case Success(lines) => logger.info("Email Send Success -" + sbj + ":" + msg) 
        case Failure(e) => logger.info("Email Send Failure -" + sbj + ":" + msg + "|||" + e.getMessage) 
  }
}

 