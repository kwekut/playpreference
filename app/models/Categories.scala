package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import java.util.{Properties, UUID}
import play.api.Logger
import scala.concurrent.duration._
import models.mail._

//Used by client 
object Categories{
  val feedtemplatecategories = List(
    Json.obj("id" -> "1", "activity" -> "SHOPTEMPLATECATEGORY", "typ" -> "event", "templatename" -> "eventtemplate-1", "templatedesc" -> "Default templates for event", "templatepic" ->"www.eventpicurl1.com"),
    Json.obj("id" -> "2", "activity" -> "SHOPTEMPLATECATEGORY", "typ" -> "promo", "templatename" -> "promotemplate-1", "templatedesc" -> "Default templates for promo", "templatepic" ->"www.promopicurl1.com"),
    Json.obj("id" -> "3", "activity" -> "SHOPTEMPLATECATEGORY", "typ" -> "order", "templatename" -> "ordertemplate-1", "templatedesc" -> "Default template for order", "templatepic" ->"www.orderpicurl1.com"),
    Json.obj("id" -> "4", "activity" -> "SHOPTEMPLATECATEGORY", "typ" -> "resv", "templatename" -> "resvtemplate-1", "templatedesc" -> "Default template for resv", "templatepic" ->"www.resvpicurl1.com"),
    Json.obj("id" -> "5", "activity" -> "SHOPTEMPLATECATEGORY", "typ" -> "mail", "templatename" -> "mailtemplate-1", "templatedesc" -> "Default template for mail", "templatepic" ->"www.mailpicurl1.com")
  )
  val shoptemplatecategories = List(
    Json.obj("id" -> "1", "activity" -> "SHOPTEMPLATECATEGORY", "typ" -> "restaurant", "templatename" -> "shoptemplate-1", "templatedesc" -> "Default templates for restaurants", "templatepic" ->"www.shoppicurl1.com")
  )
  val feedcategories = List(
    Json.obj("id" -> "1", "activity" -> "FEEDCATEGORY", "typ" -> "restaurant", "category" -> "Italian","name" ->"Pizza"),
    Json.obj("id" -> "2", "activity" -> "FEEDCATEGORY", "typ" -> "restaurant", "category" -> "Korean","name" ->"Kimchi"),
    Json.obj("id" -> "3", "activity" -> "FEEDCATEGORY", "typ" -> "restaurant", "category" -> "Chinese","name" ->"Gen-Tsao")
  )
  val shopcategories = List(
    Json.obj("id" -> "1", "activity" -> "SHOPCATEGORY", "typ" -> "restaurant", "category" -> "FOOD","name" ->"Italian"),
    Json.obj("id" -> "2", "activity" -> "SHOPCATEGORY", "typ" -> "restaurant", "category" -> "FOOD","name" ->"Korean"),
    Json.obj("id" -> "3", "activity" -> "SHOPCATEGORY", "typ" -> "restaurant", "category" -> "FOOD","name" ->"Chinese")
  )
  val abusecategories = List(
    Json.obj("id" -> "1", "activity" -> "ABUSECATEGORY", "typ" -> "Financial", "category" -> "FRAUD","name" ->"Payment-Fraud"),
    Json.obj("id" -> "2", "activity" -> "ABUSECATEGORY", "typ" -> "Troubleshoot", "category" -> "ERROR", "name" ->"Server-Error"),
    Json.obj("id" -> "3", "activity" -> "ABUSECATEGORY", "typ" -> "Report", "category" -> "COMPLIANT","name" ->"Service-Compliant"),
    Json.obj("id" -> "4", "activity" -> "ABUSECATEGORY", "typ" -> "Info", "category" -> "ENQUIRY","name" ->"Request-Info")

  ) 
  val currencycategories = List(
    Json.obj("id" -> "1", "activity" -> "CURRENCYCATEGORY", "currency" -> "USD","name" ->"US-Dollar"),
    Json.obj("id" -> "2", "activity" -> "CURRENCYCATEGORY", "currency" -> "EURO","name" ->"European-Euro"),
    Json.obj("id" -> "3", "activity" -> "CURRENCYCATEGORY", "currency" -> "GBP","name" ->"British-Pound"),
    Json.obj("id" -> "4", "activity" -> "CURRENCYCATEGORY", "currency" -> "CND","name" ->"Canadian-Dollar"),
    Json.obj("id" -> "5", "activity" -> "CURRENCYCATEGORY", "currency" -> "YEN","name" ->"Japanese-Yen"),
    Json.obj("id" -> "6", "activity" -> "CURRENCYCATEGORY", "currency" -> "PESO","name" ->"Mexican-Peso"),
    Json.obj("id" -> "7", "activity" -> "CURRENCYCATEGORY", "currency" -> "YUAN","name" ->"Chinese-Yuan")
  )  
  val timezones = List(
    Json.obj("id" -> "1", "activity" -> "TIMEZONECATEGORY", "offset" -> "-12:00", "canonid" -> "Etc/GMT+12"),
    Json.obj("id" -> "2", "activity" -> "TIMEZONECATEGORY", "offset" -> "-11:00", "canonid" -> "Pacific/Midway"),
    Json.obj("id" -> "3", "activity" -> "TIMEZONECATEGORY", "offset" -> "-10:00", "canonid" -> "Pacific/Honolulu"),
    Json.obj("id" -> "4", "activity" -> "TIMEZONECATEGORY", "offset" -> "-09:00", "canonid" -> "America/Anchorage"),
    Json.obj("id" -> "5", "activity" -> "TIMEZONECATEGORY", "offset" -> "-08:00", "canonid" -> "America/Los_Angeles"),
    Json.obj("id" -> "6", "activity" -> "TIMEZONECATEGORY", "offset" -> "-07:00", "canonid" -> "America/Phoenix"),
    Json.obj("id" -> "7", "activity" -> "TIMEZONECATEGORY", "offset" -> "-06:00", "canonid" -> "America/Chicago"),
    Json.obj("id" -> "8", "activity" -> "TIMEZONECATEGORY", "offset" -> "-05:00", "canonid" -> "America/New_York"),
    Json.obj("id" -> "9", "activity" -> "TIMEZONECATEGORY", "offset" -> "-04:00", "canonid" -> "America/Puerto_Rico"),
    Json.obj("id" -> "10", "activity" -> "TIMEZONECATEGORY", "offset" -> "-03:00", "canonid" -> "America/Argentina/Buenos_Aires"),
    Json.obj("id" -> "11", "activity" -> "TIMEZONECATEGORY", "offset" -> "+00:00", "canonid" -> "Europe/London"),
    Json.obj("id" -> "12", "activity" -> "TIMEZONECATEGORY", "offset" -> "+01:00", "canonid" -> "Europe/Paris"),
    Json.obj("id" -> "13", "activity" -> "TIMEZONECATEGORY", "offset" -> "+02:00", "canonid" -> "Europe/Istanbul"),
    Json.obj("id" -> "14", "activity" -> "TIMEZONECATEGORY", "offset" -> "+03:00", "canonid" -> "Europe/Moscow"),
    Json.obj("id" -> "15", "activity" -> "TIMEZONECATEGORY", "offset" -> "+04:00", "canonid" -> "Asia/Dubai"),
    Json.obj("id" -> "16", "activity" -> "TIMEZONECATEGORY", "offset" -> "+05:00", "canonid" -> "Asia/Karachi"),
    Json.obj("id" -> "17", "activity" -> "TIMEZONECATEGORY", "offset" -> "+06:00", "canonid" -> "Asia/Dhaka"),
    Json.obj("id" -> "18", "activity" -> "TIMEZONECATEGORY", "offset" -> "+07:00", "canonid" -> "Asia/Jakarta"),
    Json.obj("id" -> "19", "activity" -> "TIMEZONECATEGORY", "offset" -> "+08:00", "canonid" -> "Asia/Shanghai"),
    Json.obj("id" -> "20", "activity" -> "TIMEZONECATEGORY", "offset" -> "+09:00", "canonid" -> "Asia/Tokyo"),
    Json.obj("id" -> "21", "activity" -> "TIMEZONECATEGORY", "offset" -> "+10:00", "canonid" -> "Australia/Sydney"),
    Json.obj("id" -> "22", "activity" -> "TIMEZONECATEGORY", "offset" -> "+11:00", "canonid" -> "Antarctica/Macquarie"),
    Json.obj("id" -> "23", "activity" -> "TIMEZONECATEGORY", "offset" -> "+12:00", "canonid" -> "Pacific/Auckland")
  )
  val defaulthints = List(
    Json.obj("id" -> "1", "activity" -> "HINTSDEFAULTS", "hint" -> "EVENT"),
    Json.obj("id" -> "2", "activity" -> "HINTSDEFAULTS", "hint" -> "PROMO"),
    Json.obj("id" -> "3", "activity" -> "HINTSDEFAULTS", "hint" -> "RESV"),
    Json.obj("id" -> "1", "activity" -> "HINTSDEFAULTS", "hint" -> "ORDER"),
    Json.obj("id" -> "2", "activity" -> "HINTSDEFAULTS", "hint" -> "MAIL"),
    Json.obj("id" -> "3", "activity" -> "HINTSDEFAULTS", "hint" -> "READ")
  )
}
