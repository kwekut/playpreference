package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

//scala Kafka model
case class  KafkaMessage(
  ids: Seq[String], source: String, priority: String, 
  timetolive: Int, clickaction: String, msg: Msg)

object KafkaMessage{
  implicit lazy val kmsgFormat = Jsonx.formatCaseClass[KafkaMessage]
}

case class PersistKmsgs(kmsg: KafkaMessage)
case class JsonProduce(user: User, topicName: String, kmsg: KafkaMessage)