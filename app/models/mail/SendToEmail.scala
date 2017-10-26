package models.mail

import java.util.Properties
import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.util.ArrayList
import java.util.Arrays
import play.api.Logger
import play.twirl.api.Html
import com.typesafe.config.ConfigFactory
import scala.util.Try
import java.util.UUID
import scala.util.{Success, Failure}
import play.api.libs.mailer._
import java.io.File
import org.apache.commons.mail.EmailAttachment
import play.api.libs.mailer.{ Email, MailerClient }
import models._

object SendToEmail {
  val c = ConfigFactory.load()
  c.checkValid(ConfigFactory.defaultReference(), "consumer")
      val from: String = c.getString("email.from")
      val mailer = new SMTPMailer(SMTPConfiguration("typesafe.org", 1234))
}

case class SendToEmail(sbj: String, omsg: String, to: String) {
  import SendEmail._
  val logger = Logger(this.getClass())

  val msg = Mail(sbj, omsg).mail
  
    def send() = Try {
      val cid = UUID.randomUUID().toString
      val email = Email(
        (sbj + ": " + AppName.appname),
        (s"<$from>"),
        ((to.toSeq.map{t=> s"<$t>"}).toSeq),  
        //bodyText = Some("A text message"),
        bodyHtml = Some(msg)
      )
      mailer.send(email)
    } match {
      case Success(lines) => logger.info("Email Send Success -" + sbj + ":" + msg) 
      case Failure(e) => logger.info("Email Send Failure -" + sbj + ":" + msg + "|||" + e.getMessage) 
    }
}
