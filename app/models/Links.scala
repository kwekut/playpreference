package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

//Used by client to send simple messages to server
// LikeShop/ LikeProd/ MarkRead/ Follow/ Unfollow/ Followings/ Followers/ DeleteFeed
// SeeShop/ SeeShopProds/ Profile/ History
case class Links(
  id: String, //UUID as String
  userid: String,
  shopid: String,
  activity: String,
  feedid: String,
  pagination: String,
  filter: String)

object Links{
  implicit lazy val lnksFormat = Jsonx.formatCaseClass[Links]
}

case class Lnke(
  id: String, //UUID as String
  userid: String,
  shopid: String,
  activity: String,
  feedid: String,
  pagination: String,
  filter: String)

object Lnke{
  implicit lazy val lnksFormat = Jsonx.formatCaseClass[Lnke]
}