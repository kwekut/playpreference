package models

import java.util.UUID
import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

 //Scala Cassandra model
 //Lat/Lng is saved on device
case class User(
  userid: String,
  profiles: Seq[LoginInfo],
  firstname: Option[String],
  lastname: Option[String],
  fullname: Option[String],
  username: Option[String],
  email: Option[String],
  phone: Option[String],
  homeaddress: Option[String],
  officeaddress: Option[String],
  roamingaddress: Option[String],
  paymentsummary: List[String], //chosen payment's summary
  alerttoken: Set[String],
  feedpreferences: List[String],
  shoppreferences: List[String],
  feedlikes: List[String],
  shoplikes: List[String],
  reviewlikes: List[String],
  ratings: List[String],
  feedhistory: List[String],
  shophistory: List[String],
  roles: Set[String] = Set("user"),
  shopoperator: Option[String],
  followings: Set[String],
  avatarURL: Option[String],
  expiredtoken: Option[String], //auth token
  securitythreat: List[String],
  securityquestions: Option[String],
  dailypurchaselimit: Option[String],
  pertransactionlimit: Option[String],
  dailycancellimit: Option[String],
  dailycancelaccumcount: Option[String], //(date, cnt),
  dailyorderaccumamount: Option[String], //(date, amt)
  timezone: String,
  created: String = DateTime.now.toString) extends Identity

// Usr object - Only send non-sensitive data to client,
object User{
  def buildUsr(u: User): Usr = {
    Usr(
      u.userid,
      u.username.getOrElse("No username"),
      u.email.getOrElse("No email"),
      "USER",
      "SHOWPROFILE",
      u.firstname.getOrElse("No firstname"),
      u.lastname.getOrElse("No lastname"),
      u.phone.getOrElse("No phone"),
      u.homeaddress.getOrElse("No home address"),
      u.officeaddress.getOrElse("No office address"),
      u.roamingaddress.getOrElse("No roaming address"),
      u.paymentsummary.mkString(", "),
      u.alerttoken.mkString(","),
      u.roles.mkString(","),
      u.avatarURL.getOrElse("No avatar found"),
      u.dailypurchaselimit.getOrElse("No dailypurchaselimit info"),
      u.pertransactionlimit.getOrElse("No pertransactionlimit info"),
      u.dailycancellimit.getOrElse("No dailycancellimit info"),
      u.timezone,
      u.created)
  }

  def newUser: User = {
    User(
        "userid",
        Seq(LoginInfo("credentials", "email")),
        Some("firstname"),
        Some("lastname"),
        Some("firstname lastname"),
        Some("username"),
        Some("email"),
        Some("phone"),
        Some("homeaddress"),
        Some("officeaddress"),
        Some("roamingaddress"),
        List("paymentsummary"),
        Set("alerttoken"),
        List("feedpreferences"),
        List("shoppreferences"),
        List("feedlikes"),
        List("shoplikes"),
        List("reviewlikes"),
        List("ratings"),
        List("feedhistory"),
        List("shophistory"),
        Set("roles"),
        Some("shopoperator"),
        Set("followings"),
        Some("avatarURL"),
        Some("expiredtoken"),
        List("securitythreat"),
        Some("securityquestions"),
        Some("dailypurchaselimit"),
        Some("pertransactionlimit"),
        Some("dailycancellimit"),
        Some("dailycancelaccumcount"),
        Some("dailyorderaccumamount"),
        "timezone",
        "created"
    )
  }

  def updateUser(u: User, r: Usr): User = {
    User(
        u.userid,
        u.profiles,
        Some(r.firstname),
        Some(r.lastname),
        Some(r.firstname+" "+r.lastname),
        Some(r.username),
        u.email,
        Some(r.phone),
        Some(r.homeaddress),
        Some(r.officeaddress),
        Some(r.roamingaddress),
        u.paymentsummary,
        ((u.alerttoken) + (r.alerttoken)),
        u.feedpreferences,
        u.shoppreferences,
        u.feedlikes,
        u.shoplikes,
        u.reviewlikes,
        u.ratings,
        u.feedhistory,
        u.shophistory,
        u.roles,
        u.shopoperator,
        u.followings,
        u.avatarURL,
        u.expiredtoken,
        u.securitythreat,
        u.securityquestions,
        Some(r.dailypurchaselimit),
        Some(r.pertransactionlimit),
        Some(r.dailycancellimit),
        u.dailycancelaccumcount,
        u.dailyorderaccumamount,
        u.timezone,
        u.created
    )
  }

  def updateUserAddress(u: User, r: Usr): User = {
      u.copy(
        dailypurchaselimit = Some(r.dailypurchaselimit),
        pertransactionlimit = Some(r.pertransactionlimit),
        dailycancellimit = Some(r.dailycancellimit)
      )
  }

  def updateUserTransactions(u: User, r: Usr): User = {
      u.copy(
        homeaddress = Some(r.homeaddress),
        officeaddress = Some(r.officeaddress),
        roamingaddress = Some(r.roamingaddress)
      )
  }

  def updateUserPrivate(u: User, r: Usr): User = {
      u.copy(
        firstname = Some(r.firstname),
        lastname = Some(r.lastname),
        fullname = Some(r.firstname+" "+r.lastname),
        username = Some(r.username),
        phone = Some(r.phone)
      )
  }

  implicit lazy val userFormat = Jsonx.formatCaseClass[User]
}

//User object to client - Client user profile object
// Profile/
case class  Usr(
  id: String,
  username: String,
  email: String,
  typ: String,
  activity: String,
  firstname: String,
  lastname: String,
  phone: String,
  homeaddress: String,
  officeaddress: String,
  roamingaddress: String,
  paymentsummary: String,
  alerttoken: String,
  roles: String,
  picurl: String,
  dailypurchaselimit: String,
  pertransactionlimit: String,
  dailycancellimit: String,
  timezone: String,
  created: String)

object Usr{
  implicit lazy val usrFormat = Jsonx.formatCaseClass[Usr]
}
