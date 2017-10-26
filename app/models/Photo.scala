package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

case class  Photo(id: String, picid: String, picname: String, picformat: String, 
  typ: String, activity: String, picdimension: String, picdesc: String, picurl: String, created: String)

object Photo{
  implicit lazy val phtoFormat = Jsonx.formatCaseClass[Photo]
}
