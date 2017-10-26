package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


// Used by server to provide feedback to clients previous request
//Feedreview, shopreview
case class  Review(id: String, feedid: String, shopid: String, 
  userid: String, feedname: String, shopname: String, username: String,
  typ: String, activity: String, ratings: String, 
  comment: String, highlight: String, created: String)

object Review{
  implicit val revReads: Reads[Review] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "feedid").read[String] and
    (JsPath \ "shopid").read[String] and
    (JsPath \ "userid").read[String] and
    (JsPath \ "feedname").read[String] and
    (JsPath \ "shopname").read[String] and
    (JsPath \ "username").read[String] and
    (JsPath \ "typ").read[String] and
    (JsPath \ "activity").read[String] and
    (JsPath \ "ratings").read[String] and
    (JsPath \ "comment").read[String] and
    (JsPath \ "highlight").read[String] and
    (JsPath \ "created").read[String] 
  )(Review.apply _)

  implicit val revWrites: Writes[Review] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "feedid").write[String] and
    (JsPath \ "shopid").write[String] and
    (JsPath \ "userid").write[String] and
    (JsPath \ "feedname").write[String] and
    (JsPath \ "shopname").write[String] and
    (JsPath \ "username").write[String] and
    (JsPath \ "typ").write[String] and
    (JsPath \ "activity").write[String] and
    (JsPath \ "ratings").write[String] and
    (JsPath \ "comment").write[String] and
    (JsPath \ "highlight").write[String] and
    (JsPath \ "created").write[String]
  )(unlift(Review.unapply))
}

//out, user, 
