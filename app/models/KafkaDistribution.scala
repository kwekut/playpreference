package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

//scala Kafka model
case class  KafkaDistribution(appname: String, userids: Set[String])

object KafkaDistribution{
  implicit val kdReads: Reads[KafkaDistribution] = (
    (JsPath \ "appname").read[String] and
    (JsPath \ "userids").read[Set[String]] 
  )(KafkaDistribution.apply _)

  implicit val kdWrites: Writes[KafkaDistribution] = (
  	(JsPath \ "appname").write[String] and
    (JsPath \ "userids").write[Set[String]]
  )(unlift(KafkaDistribution.unapply))
}
