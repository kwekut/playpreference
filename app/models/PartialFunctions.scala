package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.LocalDateTime
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.concurrent.duration._
import org.elasticsearch.index.search._
import org.elasticsearch.search._
import scala.concurrent.{ ExecutionContext, Future }
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import scala.util.Try
import java.util.UUID
import org.joda.time.LocalDateTime
import models.Silhouette._

object PartialFunctions{

  //Parse from Json string Msg to scala case Msg
  val jmsgTOsmsgPF: PartialFunction[String, Msg] = {
    case m: String if Json.parse(m).asOpt[Msg].isDefined => (Json.parse(m).asOpt[Msg]).get
  }
  //Parse from Json string Msg to Json Msg
  val jmsgTOjmsgPF: PartialFunction[String, JsValue] = {
    case m: String if Json.parse(m).asOpt[Msg].isDefined => Json.toJson((Json.parse(m).asOpt[Msg]).get)
  }
   //Parse from Json string User to Json Usr
  val juserTOjusrPF: PartialFunction[String, JsValue] = {
    case u: String if Json.parse(u).asOpt[User].isDefined => Json.toJson(User.buildUsr((Json.parse(u).asOpt[User]).get))
  }
   //Parse from Json string User to Scala Usr
  val juserTOsusrPF: PartialFunction[String, Usr] = {
    case u: String if Json.parse(u).asOpt[User].isDefined => User.buildUsr((Json.parse(u).asOpt[User]).get)
  }
  //Parse from Json string User to Scala case User
  val juserTOsuserPF: PartialFunction[String, User] = {
    case u: String if Json.parse(u).asOpt[User].isDefined => (Json.parse(u).asOpt[User]).get
  }



//Encryptor(stringData).userEncrypt
   //Parse from encrpted Json string User to Json Usr
  val jcryptuserTOjusrPF: PartialFunction[String, JsValue] = {
    case u: String if Json.parse(Encryptor(u).userDecrypt.get).asOpt[User].isDefined => Json.toJson(User.buildUsr((Json.parse(Encryptor(u).userDecrypt.get).asOpt[User]).get))
  }
   //Parse from encrpted Json string User to Scala Usr
  val jcryptuserTOsusrPF: PartialFunction[String, Usr] = {
    case u: String if Json.parse(Encryptor(u).userDecrypt.get).asOpt[User].isDefined => User.buildUsr((Json.parse(Encryptor(u).userDecrypt.get).asOpt[User]).get)
  }
  //Parse from Json string User to Scala case User
  val jcryptuserTOsuserPF: PartialFunction[String, User] = {
    case u: String if Json.parse(Encryptor(u).userDecrypt.get).asOpt[User].isDefined => (Json.parse(Encryptor(u).userDecrypt.get).asOpt[User]).get
  }
  //Parse from Json string ljwt to Scala case ljwt
  val jcryptljwtTOsljwtPF: PartialFunction[String, CustomJWTAuthenticator] = {
    case j: String if Json.parse(Encryptor(j).jwtDecrypt.get).asOpt[CustomJWTAuthenticator].isDefined => (Json.parse(Encryptor(j).jwtDecrypt.get).asOpt[CustomJWTAuthenticator]).get
  }
  //Parse from Json string ljwt to Scala case jwt
  val jcryptljwtTOsjwtPF: PartialFunction[String, JWTAuthenticator] = {
    case j: String if Json.parse(Encryptor(j).jwtDecrypt.get).asOpt[CustomJWTAuthenticator].isDefined => CustomJWTAuthenticator.buildJWTAuthenticator((Json.parse(Encryptor(j).jwtDecrypt.get).asOpt[CustomJWTAuthenticator]).get)
  }
  //Parse from Json string ljwt to Scala case ljwt
  val jljwtTOsljwtPF: PartialFunction[String, CustomJWTAuthenticator] = {
    case j: String if Json.parse(j).asOpt[CustomJWTAuthenticator].isDefined => (Json.parse(j).asOpt[CustomJWTAuthenticator]).get
  }
  //Parse from Json string ljwt to Scala case jwt
  val jljwtTOsjwtPF: PartialFunction[String, JWTAuthenticator] = {
    case j: String if Json.parse(j).asOpt[CustomJWTAuthenticator].isDefined => CustomJWTAuthenticator.buildJWTAuthenticator((Json.parse(j).asOpt[CustomJWTAuthenticator]).get)
  }

  //Parse from Json string Feed to Json Msg
  val jfeedTOjmsgPF: PartialFunction[String, JsValue] = {
    case f: String if Json.parse(f).asOpt[Feed].isDefined => (Json.toJson(Feed.buildMsg((Json.parse(f).asOpt[Feed]).get)))
  }
  //Parse from Json string Feed to scala Msg
  val jfeedTOsmsgPF: PartialFunction[String, Msg] = {
    case f: String if Json.parse(f).asOpt[Feed].isDefined => (Feed.buildMsg((Json.parse(f).asOpt[Feed]).get))
  }
  //Parse from Json string Feed to case Feed
  val jfeedTOsfeedPF: PartialFunction[String, Feed] = {
    case f: String if Json.parse(f).asOpt[Feed].isDefined => (Json.parse(f).asOpt[Feed]).get
  }
  //Parse from Json string Shop to Scala case Shop
  val jshopTOsshopPF: PartialFunction[String, Shop] = {
    case s: String if Json.parse(s).asOpt[Shop].isDefined => (Json.parse(s).asOpt[Shop]).get
  }
  //Parse from Json string Shop to Json Shp
  val jshopTOjshpPF: PartialFunction[String, JsValue] = {
    case s: String if Json.parse(s).asOpt[Shop].isDefined => (Json.toJson(Shop.buildShp((Json.parse(s).asOpt[Shop]).get)))
  }
  val jshopTOclientjshpPF: PartialFunction[Tuple2[Option[String], User], JsValue] = {
    case s: Tuple2[Option[String], User] if Json.parse(s._1.get).asOpt[Shop].isDefined => (Json.toJson(Shop.buildClientShp((Json.parse(s._1.get).asOpt[Shop].get), s._2)))
  }
  val jshopTOclientsshpPF: PartialFunction[Tuple2[Option[String], User], Shp] = {
    case s: Tuple2[Option[String], User] if Json.parse(s._1.get).asOpt[Shop].isDefined => (Shop.buildClientShp((Json.parse(s._1.get).asOpt[Shop].get), s._2))
  }
  //Parse from Json string Shop to scala Shp
  val jshopTOsshpPF: PartialFunction[String, Shp] = {
    case s: String if Json.parse(s).asOpt[Shop].isDefined => (Shop.buildShp((Json.parse(s).asOpt[Shop]).get))
  }

   //Parse from Json string Review to Scala Review
  val jreviewTOsreviewPF: PartialFunction[String, Review] = {
    case u: String if Json.parse(u).asOpt[Review].isDefined => (Json.parse(u).asOpt[Review]).get
  }

   //Parse from Json string Photo to Scala Photo
  val jphotoTOsphotoPF: PartialFunction[String, Photo] = {
    case u: String if Json.parse(u).asOpt[Photo].isDefined => (Json.parse(u).asOpt[Photo]).get
  }
// ////////////////Cassandra Direct-Without Spark/////////////////////////
//   val pclmTOsshopPF: PartialFunction[PayloadColumn, Shop] = {
//     case f: PayloadColumn if Json.parse(f.payload.get).asOpt[Shop].isDefined => (Json.parse(f.payload.get).asOpt[Shop]).get
//   }
//   val pclmTOsfeedPF: PartialFunction[PayloadColumn, Feed] = {
//     case f: PayloadColumn if Json.parse(f.payload.get).asOpt[Feed].isDefined => (Json.parse(f.payload.get).asOpt[Feed]).get
//   }
//   val pclmTOsprefnPF: PartialFunction[PayloadColumn, Preference] = {
//     case f: PayloadColumn if Json.parse(f.payload.get).asOpt[Preference].isDefined => (Json.parse(f.payload.get).asOpt[Preference]).get
//   }
//   val pclmTOsuserPF: PartialFunction[PayloadColumn, User] = {
//     case f: PayloadColumn if Json.parse(f.payload.get).asOpt[User].isDefined => (Json.parse(f.payload.get).asOpt[User]).get
//   }
////////////ElasticSearch on clickapp/////////////
 //make sure expired promos and events are not included
  val ehitTOprodidswoPF: PartialFunction[SearchHit, Set[String]] = {
    case f: SearchHit if Json.parse(f.getSourceAsString()).asOpt[Pref].isDefined
    =>
    val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")
    val now: DateTime = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z"))
    val p = (Json.parse(f.getSourceAsString()).asOpt[Pref]).get.prdid2typ2exp.toSet
    for {
      x <- p
      z <- (x.toString.split(" ").head.toString) if (
        x.contains("order") || x.contains("promo")
        && x.contains("eternal") || x.contains("Eternal")
        || dtz.parseDateTime((x.split(" ").last)).isBefore(now)
        )
    } yield z.toString
  }
  val ehitTOprodidsPF: PartialFunction[SearchHit, Set[String]] = {
    case f: SearchHit if Json.parse(f.getSourceAsString()).asOpt[Pref].isDefined => (Json.parse(f.getSourceAsString()).asOpt[Pref]).get.top10productids.toSet
  }
  val ehitTOshopidsPF: PartialFunction[SearchHit, Set[String]] = {
    case f: SearchHit if Json.parse(f.getSourceAsString()).asOpt[Pref].isDefined => (Json.parse(f.getSourceAsString()).asOpt[Pref]).get.shopids.toSet
  }
  val ehitTOcustomeridsPF: PartialFunction[SearchHit, Set[String]] = {
    case f: SearchHit if Json.parse(f.getSourceAsString()).asOpt[Pref].isDefined => (Json.parse(f.getSourceAsString()).asOpt[Pref]).get.customerids.toSet
  }
  val ehitTOjprefPF: PartialFunction[SearchHit, Pref] = {
    case f: SearchHit if Json.parse(f.getSourceAsString()).asOpt[Pref].isDefined => (Json.parse(f.getSourceAsString()).asOpt[Pref]).get
  }

  val ehitTOsshpPF: PartialFunction[SearchHit, Shp] = {
    case f: SearchHit if Json.parse(f.getSourceAsString()).asOpt[Shp].isDefined => (Json.parse(f.getSourceAsString()).asOpt[Shp]).get
  }
  val ehitTOjshpPF: PartialFunction[SearchHit, Shp] = {
    case f: SearchHit if Json.parse(f.getSourceAsString()).asOpt[Shp].isDefined => Json.parse(f.getSourceAsString()).asOpt[Shp].get
  }
  val ehitTOsmsgPF: PartialFunction[SearchHit, Msg] = {
    case f: SearchHit if Json.parse(f.getSourceAsString()).asOpt[Msg].isDefined => (Json.parse(f.getSourceAsString()).asOpt[Msg]).get
  }
  val ehitTOjmsgPF: PartialFunction[SearchHit, Msg] = {
    case f: SearchHit if Json.parse(f.getSourceAsString()).asOpt[Msg].isDefined => (Json.parse(f.getSourceAsString()).asOpt[Msg]).get
  }



  val spwinfoTOjcryptpwinfoPF: PartialFunction[PasswordInfo, String] = {
    case u: PasswordInfo if Json.parse(Encryptor(Json.toJson(u).toString).secretEncrypt.get).asOpt[String].isDefined => Encryptor(Json.toJson(u).toString).secretEncrypt.get
  }
  val jcryptpwinfoTOspwinfoPF: PartialFunction[String, PasswordInfo] = {
    case u: String if Json.parse(Encryptor(u).secretDecrypt.get).asOpt[PasswordInfo].isDefined => (Json.parse(Encryptor(u).secretDecrypt.get).asOpt[PasswordInfo]).get
  }

  val scspflTOjcryptcspflPF: PartialFunction[CommonSocialProfile, String] = {
    case u: CommonSocialProfile if Json.parse(Encryptor(Json.toJson(u).toString).secretEncrypt.get).asOpt[String].isDefined => Encryptor(Json.toJson(u).toString).secretEncrypt.get
  }
  val jcryptcspflTOscspflPF: PartialFunction[String, CommonSocialProfile] = {
    case u: String if Json.parse(Encryptor(u).secretDecrypt.get).asOpt[CommonSocialProfile].isDefined => (Json.parse(Encryptor(u).secretDecrypt.get).asOpt[CommonSocialProfile]).get
  }

  val suserTOjcryptuserPF: PartialFunction[User, String] = {
    case u: User if Json.parse(Encryptor(Json.toJson(u).toString).userEncrypt.get).asOpt[String].isDefined => Encryptor(Json.toJson(u).toString).userEncrypt.get
  }
  val sjwtTOjcryptljwtPF: PartialFunction[JWTAuthenticator, String] = {
    case j: JWTAuthenticator if Json.parse(Encryptor(Json.toJson(CustomJWTAuthenticator.buildCustomJWTAuthenticator(j)).toString).jwtEncrypt.get).asOpt[String].isDefined => Encryptor(Json.toJson(CustomJWTAuthenticator.buildCustomJWTAuthenticator(j)).toString).jwtEncrypt.get
  }


}
