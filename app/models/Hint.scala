package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

case class Hint(
  id: String,
  activity: String,
  hint: String)

case class SparkToSocHints(activity: String,hints: List[Hint])

object Hint{
  implicit lazy val hintFormat = Jsonx.formatCaseClass[Hint]
}
