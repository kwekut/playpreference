package models

import java.util.UUID
import akka.actor.{ ActorRef, ActorSystem, Props, Actor }
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

// Used by server to provide feedback to clients previous request
// LikeShop/ LikeProd/ MarkRead/ Follow/ Unfollow/ Followings/ Followers/ DeleteFeed
// SeeShop/ SeeShopProds/ Profile/ History

//Send feedback to client
case object  Reports
case object  FlushInteractor
case object  FlushReporter
case object  FlushAnnouncer
case object  FlushDivulger
case object  FlushTalker
//Send feedback to client
case class  Interactor(id: String, activity: String, message: String, created: String)
//Report Errors Database, Uploadcare, Stripe
case class  Reporter(typ: String, message: String, location: String, created: String)
//Report Errors Actors Supervisor/ CircuitBreaker
case class  Announcer(typ: String, message: String, payload: String, location: String, created: String) 
//Report Secirity Actor Websocket
case class Divulger(user: User, out: ActorRef, obj: JsValue)
case class JDivulger(userid: String, obj: JsValue)
//Report Secirity Mop all actor leaks
case class  Talker(obj: String)
//Report Client Abuse, Reports
case class  Abuser(typ: String, activity: String, category: String, name: String, created: String)

case class SendNotification(userid: String, body: String, title: String, priority: String, timetolive: Int, clickaction: String)
//Firebase notification models
case class Notification(body: String, title: String, icon: String, clickAction: String)
case class PushNotification(to: String, priority: String, timetolive: Int, notification: Notification)
//Firebase Request reply
case class FireHead(multicast_id: Option[String], success: Option[String], failure: Option[String], canonical_ids: Option[String])
case class FireResult(message_id: Option[String], registration_id: Option[String], error: Option[String])
case object CheckServerKey

object Abuser{
  implicit lazy val absrFormat = Jsonx.formatCaseClass[Abuser]
}
object Interactor{
  implicit lazy val intrFormat = Jsonx.formatCaseClass[Interactor]
}
object Reporter{
  implicit lazy val rptrFormat = Jsonx.formatCaseClass[Reporter]
}
object Announcer{
  implicit lazy val ancrFormat = Jsonx.formatCaseClass[Announcer]
}
object Divulger{
  def toJDivulger(d: Divulger): JDivulger = JDivulger(d.user.userid, d.obj)
}
object JDivulger{
  implicit lazy val dvlgFormat = Jsonx.formatCaseClass[JDivulger]
}
object Talker{
  implicit lazy val tlkrFormat = Jsonx.formatCaseClass[Talker]
}


object Notification{
  implicit lazy val notFormat = Jsonx.formatCaseClass[Notification]
}
object PushNotification{
  implicit lazy val pushnotFormat = Jsonx.formatCaseClass[PushNotification]
}
object FireHead{
  implicit lazy val fhFormat = Jsonx.formatCaseClass[FireHead]
}
object FireResult{
  implicit lazy val fresFormat = Jsonx.formatCaseClass[FireResult]
}
