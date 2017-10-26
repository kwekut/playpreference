package models

import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import java.util.{Properties, UUID}
import scala.concurrent.duration._

object AppName{
	val c = ConfigFactory.load()
	c.checkValid(ConfigFactory.defaultReference(), "appname")
    
  private val name = c.getString("app.appname")
  private val instance = UUID.randomUUID().toString
  val appname = name + instance
}

case object HealthCheck
