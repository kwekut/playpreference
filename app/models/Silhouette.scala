package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit 
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import scala.util.Try
import java.util.UUID
import org.joda.time.LocalDateTime


object Silhouette{
    implicit val loginInfoReads: Reads[LoginInfo] = (
      (JsPath \ "providerID").read[String] and
      (JsPath \ "providerKey").read[String]
    )(LoginInfo.apply _)
    implicit val loginInfoWrites: Writes[LoginInfo] = (
      (JsPath \ "providerID").write[String] and
      (JsPath \ "providerKey").write[String]
    )(unlift(LoginInfo.unapply))

    implicit val passwordInfo: Reads[PasswordInfo] = (
      (JsPath \ "hasher").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "salt").readNullable[String]
    )(PasswordInfo.apply _)
    implicit val passwordInfoWrites: Writes[PasswordInfo] = (
      (JsPath \ "hasher").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "salt").writeNullable[String]
    )(unlift(PasswordInfo.unapply))


    implicit val commonSocialProfileReads: Reads[CommonSocialProfile] = (
      (JsPath \ "loginInfo").read[LoginInfo] and
      (JsPath \ "firstName").readNullable[String] and
      (JsPath \ "lastName").readNullable[String] and
      (JsPath \ "fullName").readNullable[String] and
      (JsPath \ "email").readNullable[String] and
      (JsPath \ "avatarURL").readNullable[String] 
    )(CommonSocialProfile.apply _)
    implicit val commonSocialProfileWrites: Writes[CommonSocialProfile] = (
      (JsPath \ "loginInfo").write[LoginInfo] and
      (JsPath \ "firstName").writeNullable[String] and
      (JsPath \ "lastName").writeNullable[String] and
      (JsPath \ "fullName").writeNullable[String] and
      (JsPath \ "email").writeNullable[String] and
      (JsPath \ "avatarURL").writeNullable[String] 
    )(unlift(CommonSocialProfile.unapply))

}

//idleTimeout = c.getAs[FiniteDuration]("30 minutes"),
//idleTmeout.toString

//val duration = Duration("3 seconds")
//val fd = Some(duration).collect { case d: FiniteDuration => d }
//Some(3 seconds)

//Try(Duration("3 seconds").asInstanceOf[FiniteDuration]).toOption
