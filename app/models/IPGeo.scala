package models


import play.api.Configuration
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import com.typesafe.config.ConfigFactory
import scala.util.Try
import scala.util.{Success, Failure}
import java.util.UUID
import scala.concurrent.Future
import models.mail._


object IPGeo {
  private val cong = ConfigFactory.load()
  private val key: String = cong.getString("crypto.key")
}



case class IPGeo(ipAddress:String) {
  import IPGeo._

  def get = {
    val url = "http://api.ipinfodb.com/v3/ip-city"
    val complexResponse = ws.url(url)
      .withRequestTimeout(10000.millis)
      .withQueryString("key" -> key, "ip" -> ipAddress)
      .get()
    val res = complexResponse map { response => 
        (response.json \ "latitude").as[String],
        (response.json \ "longitude").as[String]
    }
    res
  }
}