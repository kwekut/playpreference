package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx
import akka.actor.ActorRef
//Scala Cassandra model
case class RequestReturn(status: String, message: String, devicetoken: String, token: String, username: String, email: String)

case class AuthReply(id: String, email: String, 
  username: String, activity: String, devicetoken: String, 
  token: String, homeurl: String, mapurl: String, 
  adminurl: String, searchurl: String, qrurl: String, openurl: String)

object AuthReply{ 
  implicit lazy val authreplyFormat = Jsonx.formatCaseClass[AuthReply]
}
