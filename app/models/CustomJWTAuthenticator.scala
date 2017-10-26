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

//new FiniteDuration(length: Long, unit: TimeUnit)
case class CustomJWTAuthenticator(
  id: String, 
  loginInfo: LoginInfo, 
  lastUsedDateTime: DateTime, 
  expirationDateTime: DateTime, 
  idleTimeout: Option[String], 
  customClaims: Option[JsObject] = None)

object CustomJWTAuthenticator{
    def buildJWTAuthenticator(j: CustomJWTAuthenticator): JWTAuthenticator = {
      JWTAuthenticator(
        j.id, 
        j.loginInfo, 
        j.lastUsedDateTime, 
        j.expirationDateTime, 
        Try(Duration(j.idleTimeout.get).asInstanceOf[FiniteDuration]).toOption, 
        j.customClaims)
    }

    def buildCustomJWTAuthenticator(j: JWTAuthenticator): CustomJWTAuthenticator = {
      CustomJWTAuthenticator(
        j.id, 
        j.loginInfo, 
        j.lastUsedDateTime, 
        j.expirationDateTime, 
        Try(j.idleTimeout.get.toString).toOption, 
        j.customClaims)
    }

    implicit val jWTAuthenticatorReads: Reads[CustomJWTAuthenticator] = (
      (JsPath \ "id").read[String] and
      (JsPath \ "loginInfo").read[LoginInfo] and
      (JsPath \ "lastUsedDateTime").read[DateTime] and
      (JsPath \ "expirationDateTime").read[DateTime] and
      (JsPath \ "idleTimeout").readNullable[String] and
      (JsPath \ "customClaims").readNullable[JsObject] 
    )(CustomJWTAuthenticator.apply _)
    implicit val jWTAuthenticatorWrites: Writes[CustomJWTAuthenticator] = (
      (JsPath \ "id").write[String] and
      (JsPath \ "loginInfo").write[LoginInfo] and
      (JsPath \ "lastUsedDateTime").write[DateTime] and
      (JsPath \ "expirationDateTime").write[DateTime] and
      (JsPath \ "idleTimeout").writeNullable[String] and
      (JsPath \ "customClaims").writeNullable[JsObject] 
    )(unlift(CustomJWTAuthenticator.unapply))
        
}


// implicit val jWTAuthenticatorWrites: Writes[JWTAuthenticator] = (
//   def writes(jwt: JWTAuthenticator) = Json.obj(
//     "id" -> jwt.id,
//     "loginInfo" -> jwt.loginInfo,
//     "lastUsedDateTime" -> jwt.lastUsedDateTime,
//     "expirationDateTime" -> jwt.expirationDateTime,
//     "idleTimeout" -> jwt.idleTimeout.toString,
//     "customClaims" -> jwt.customClaims)
// }

// implicit val jWTAuthenticatorWrites: Reads[JWTAuthenticator] = (
//   def reads(json: JsValue) = JWTAuthenticator(
//     (json \ "id").as[String],
//     (json \ "loginInfo").as[String],
//     (json \ "lastUsedDateTime").as[String],
//     (json \ "expirationDateTime").as[String],
//     Try(Duration((json \ "idleTimeout").as[String]).asInstanceOf[FiniteDuration]).toOption,
//     (json \ "customClaims").as[String])
// }
// //new FiniteDuration(length: Long, unit: TimeUnit)
// implicit val fdWrites = new Writes[FiniteDuration] {
//   def writes(fd: FiniteDuration) = Json.obj(
//     "length" -> fd.length,
//     "unit" -> fd.unit
//   )
// }

//idleTimeout = c.getAs[FiniteDuration]("30 minutes"),
//idleTmeout.toString

//val duration = Duration("3 seconds")
//val fd = Some(duration).collect { case d: FiniteDuration => d }
//Some(3 seconds)

//Try(Duration("3 seconds").asInstanceOf[FiniteDuration]).toOption
