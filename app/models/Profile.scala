package models

import java.util.UUID
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }

//Scala Cassandra model
// case class Profile(
//   email: String,
//   username: String,
//   phone: Option[String],
//   fullname: Option[String],
//   address: Option[String],
//   token: Option[String],
//   preference: Option[String])

case class Profile(
  //userid: String,
  loginInfo: LoginInfo,
  firstName: Option[String],
  lastName: Option[String],
  fullName: Option[String],
  email: Option[String],
  avatarURL: Option[String])// extends CommonSocialProfile