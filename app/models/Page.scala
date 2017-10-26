package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

case class Page(
  id: String,
  userid: String,
  typ: String,
  activity: String,
  range: String,
  page: String,
  filter: String)

object Page{
  implicit lazy val pgFormat = Jsonx.formatCaseClass[Page]
}
