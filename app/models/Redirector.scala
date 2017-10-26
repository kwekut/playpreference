package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


// Used by server to provide feedback to clients previous request
// LikeShop/ LikeProd/ MarkRead/ Follow/ Unfollow/ Followings/ Followers/ DeleteFeed
// SeeShop/ SeeShopProds/ Profile/ History
case class  Redirector(id: String, comment: String, reason: String, 
  activity: String, recommendation: String, created: String)

object Redirector{
  implicit val notReads: Reads[Redirector] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "comment").read[String] and
    (JsPath \ "reason").read[String] and
    (JsPath \ "activity").read[String] and
    (JsPath \ "recommendation").read[String] and
    (JsPath \ "created").read[String] 
  )(Redirector.apply _)

  implicit val notWrites: Writes[Redirector] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "comment").write[String] and
    (JsPath \ "reason").write[String] and
    (JsPath \ "activity").write[String] and
    (JsPath \ "recommendation").write[String] and
    (JsPath \ "created").write[String]
  )(unlift(Redirector.unapply))
}
