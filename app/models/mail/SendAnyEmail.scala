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

object SendAnyEmail {
  val c = ConfigFactory.load()
  c.checkValid(ConfigFactory.defaultReference(), "consumer")
      val from: String = c.getString("email.from")
      val mailer = new SMTPMailer(SMTPConfiguration("typesafe.org", 1234))
}

case class SendAnyEmail(sbj: String, omsg: String, attachmentnamefile: Map[String, Any], to: String) {
  import SendEmail._
  val logger = Logger(this.getClass())

  val msg = Mail(sbj, omsg).mail
  
    def send() = Try {
      val cid = UUID.randomUUID().toString
      val email = Email(
        (sbj + ": " + AppName.appname),
        (s"<$from>"),
        ((to.toSeq.map{t=> s"<$t>"}).toSeq),
        attachments = (attachmentnamefile.map{ 
            case x if x._1.endsWith(".pdf") =>
              AttachmentFile(x._1, x._2.asInstanceOf[File])
            case y if y._1.endsWith(".txt")=>
              AttachmentData(y._1, y._2.asInstanceOf[String].getBytes, "text/plain", Some("Simple data"), Some(EmailAttachment.INLINE))
            case z if z._1.endsWith(".jpg")=>  
              AttachmentFile(z._1, z._2.asInstanceOf[File], contentId = Some(cid))   
            case a if a._1.endsWith(".png")=>  
              AttachmentFile(a._1, a._2.asInstanceOf[File], contentId = Some(cid))      
          }.toSeq),     
        //bodyText = Some("A text message"),
        bodyHtml = Some(msg)
      )
      mailer.send(email)
    } match {
      case Success(lines) => logger.info("Email Send Success -" + sbj + ":" + msg) 
      case Failure(e) => logger.info("Email Send Failure -" + sbj + ":" + msg + "|||" + e.getMessage) 
    }
}
