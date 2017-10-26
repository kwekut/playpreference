package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


object KafkaTopic {
	val topicMsg = "topicMessage"
	val topicAds = "topicAdvertisement"
	val topicStats = "topicStatistics"

	val topicName = topicMsg
	val source = "mapsApp" 
	val genre = "message"
}