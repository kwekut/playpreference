package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

case class  PaymentUser(activity: String, firstName: String, lastName: String,
	email: String, ipAddress: String, typ: String, address: String,
	city: String, state: String, zip: String, dob: String, ssn: String,
	phone: String)

object PaymentUser{
  implicit lazy val phtoFormat = Jsonx.formatCaseClass[PaymentUser]
}