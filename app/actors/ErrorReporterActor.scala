package actors

import akka.actor.{ ActorRef, ActorSystem, Props, Actor }
import akka.actor.{ActorKilledException, ActorInitializationException}
import javax.inject._
import com.google.inject.name.Named
import play.api.libs.json._
import java.util.UUID
import org.joda.time.LocalTime
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.LocalDateTime
import play.api.Logger
import java.util.Properties
import play.api.libs.concurrent.Execution.Implicits._
import org.apache.kafka.clients.producer.ProducerRecord
import scala.collection.JavaConversions._
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import services.kafkas._
import scala.util.{Try, Success, Failure}
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import play.api.libs.ws._
import play.api.inject.ApplicationLifecycle
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import akka.routing._
import com.typesafe.config.ConfigFactory
import akka.pattern._
import akka.pattern.CircuitBreaker
import akka.pattern.pipe
import scala.concurrent.Future
import models.daos._
import models.mail._
import models.Locality._
import models._
import java.lang.Runtime
import play.api.Play.current
import scala.language.postfixOps
import java.io.{ByteArrayOutputStream,File,FileOutputStream,IOException,BufferedWriter,
FileWriter}


object ErrorReporterActor {

}

class ErrorReporterActor @Inject() (userDAO: UserDAO) extends Actor {
    import ErrorReporter._
    import ErrorconFig._
    val system = akka.actor.ActorSystem("system")
    import system.dispatcher
    var prompt = 0

    override val supervisorStrategy ={
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
      OneForOneStrategy(maxNrOfRetries = maxnrofretries, withinTimeRange = withintimerange milliseconds) {
        case aIE: ActorInitializationException => self ! (Announcer("ActorInitializationException", aIE.getMessage, "none", s"line $LINE of file $FILE", date)); Stop 
        case aKE: ActorKilledException => self ! (Announcer("ActorKilledException", aKE.getMessage, "none", s"line $LINE of file $FILE", date)); Stop
        case uE: Exception if prompt < 4 => prompt + 1
          self ! (Announcer("ActorException", uE.getMessage, "none", s"line $LINE of file $FILE", date)) ;Restart
      }
    }
    override def preStart() {
    }

   val recActor: ActorRef = context.actorOf(BalancingPool(initialsize).props(ErrorReporter.props(userDAO)),"errorreporterrouter")

    def receive = {
        case x => recActor forward x
    }
  
    // Consumer topics for this app instance(val apptopic)
    system.scheduler.schedule(initialdelay milliseconds, interval milliseconds) {
      breaker.withCircuitBreaker(
        Future{
           recActor ! Reports
           recActor ! FlushReporter
           recActor ! FlushAnnouncer
           recActor ! FlushDivulger
           recActor ! FlushTalker
        }
      )
    }
  }


object ErrorReporter {
  def props(userDAO: UserDAO): Props = Props(new ErrorReporter(userDAO))
}
class ErrorReporter(userDAO: UserDAO) extends Actor {
  import ErrorReporter._
  import ErrorconFig._
  implicit val ec = context.dispatcher
  var reporters: List[Reporter] = List[Reporter]()
  var announcers: List[Announcer] = List[Announcer]()
  var divulgers: List[Divulger] = List[Divulger]()
  var talkers: List[Talker] = List[Talker]()
  val producer = new KafkaProducer(Producer.props)

  //If message causes a restart triggering exception, Warn user, Inform Administrator
  override def preRestart(reason: Throwable, message: Option[Any]) {
    val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
    message match {
      case Some(m) => self ! (Announcer("ActorRestartException", reason.getMessage, m.toString, s"line $LINE of file $FILE", date))
      case None => self ! (Announcer("ActorRestartException", reason.getMessage, "none", s"line $LINE of file $FILE", date)) 
    }
  }

  def receive = {
    //SLA - send all errors to ElasticSearch via Kafka for easier searching
    //Initially Report only the first of each typ/category of error via email
    //Actor Supervisor - Reporter("id", "DatabaseException", ex.getMessage, s"line $LINE of file $FILE", date)
    case r: Abuser => 
      Try(r.toString).toOption map {rt=>
          val key = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
          val producerRecord0 = new ProducerRecord[Array[Byte],Array[Byte]](sla, partition, key.getBytes("UTF8"), rt.getBytes("UTF8"))
          producer.send(producerRecord0.asInstanceOf[ProducerRecord[Nothing, Nothing]]) 
          SendEmail("AbuseReporter - Unknown message type", rt).send
          Logger.info("AbuseReporter got something:"+ rt)
        reporters ++ List(r)
      }


    case r: Reporter => 
      Try(r.toString).toOption map {rt=>
          val key = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
          val producerRecord0 = new ProducerRecord[Array[Byte],Array[Byte]](sla, partition, key.getBytes("UTF8"), rt.getBytes("UTF8"))
          producer.send(producerRecord0.asInstanceOf[ProducerRecord[Nothing, Nothing]]) 
        if (reporters.exists(x=> x.typ == r.typ)) {} else {
          SendEmail("GeneralReporter - Unknown message type", rt).send
          Logger.info("GeneralReporter got something:"+ rt)
        }
        reporters ++ List(r)
      }
    //Get userid and tally, Save tally and assign to Honeybox, user fields[Map] to string
      //Read all msgs to log,
      //Actors - Announcer(id, "ActorRestartException", reason.getMessage, "none", s"line $LINE of file $FILE", date)  
    case a: Announcer =>
      Try(a.toString).toOption map {at=>
          val key = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
          val producerRecord0 = new ProducerRecord[Array[Byte],Array[Byte]](sla, partition, key.getBytes("UTF8"), at.getBytes("UTF8"))
          producer.send(producerRecord0.asInstanceOf[ProducerRecord[Nothing, Nothing]]) 
        if (announcers.exists(x=> x.typ == a.typ)) {} else {
          SendEmail("ActorError - Bad message type", at).send
          Logger.info("ActorError got something:"+ at)
        }
        announcers ++ List(a)
      }      
      //Divulger(user, out, js) from Websock
      //Remove user details/only show userid before sending through kafka
    case d: Divulger => 
      Try(Divulger.toJDivulger(d).toString).toOption map {dt=>
          val key = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
          val producerRecord0 = new ProducerRecord[Array[Byte],Array[Byte]](sla, partition, key.getBytes("UTF8"), dt.getBytes("UTF8"))
          producer.send(producerRecord0.asInstanceOf[ProducerRecord[Nothing, Nothing]]) 
        if (divulgers.exists(x=> x.user.username == d.user.username)) {} else {
          SendEmail("Websocket Mop - Unknown message type", dt).send
          Logger.info("Websocket Mop got something:"+ dt)
        }
        divulgers ++ List(d)
      }    
    //Talker(x) Catch all unkowns in secondary Actors
    case t: Talker =>
      Try(t.toString).toOption map {tt=>
          val key = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
          val producerRecord0 = new ProducerRecord[Array[Byte],Array[Byte]](sla, partition, key.getBytes("UTF8"), tt.getBytes("UTF8"))
          producer.send(producerRecord0.asInstanceOf[ProducerRecord[Nothing, Nothing]]) 
        if (talkers.exists(x=> x == t)) {} else {
          SendEmail("Security Concern: ErrorReporter - Unknown message type", tt).send
          Logger.info("ErrorReporter got something kafka:" + tt)
        }
        talkers ++ List(t)
      }
    // Bulk report & clear every 1hr if any exists
    case Reports =>
      //Show tally of occurence of each error typ
      val rfreq = reporters.groupBy(x=>x.typ).mapValues(_.size)
      val afreq = announcers.groupBy(x=>x.typ).mapValues(_.size)
      val dfreq = divulgers.groupBy(x=>x.user.username).mapValues(_.size)
      val tfreq = talkers.groupBy(x=>x).mapValues(_.size)

      if (rfreq.nonEmpty) {  
        SendEmail(s"$AppName.appname GeneralErrorStatistic", rfreq.mkString(", ")).send
        Logger.info("GeneralErrorStatistic:  " + rfreq.mkString(", ")) 
      }
      if (afreq.nonEmpty) {  
        SendEmail(s"$AppName.appname ActorErrorStatistic", afreq.mkString(", ")).send
        Logger.info("ActorErrorStatistic:  " + afreq.mkString(", ")) 
      }
      if (dfreq.nonEmpty) {  
        SendEmail(s"$AppName.appname WebsocketCorncernStatistic", dfreq.mkString(", ")).send
        Logger.info("WebsocketCorncernStatistic:  " + dfreq.mkString(", ")) 
      }
      if (tfreq.nonEmpty) {  
        SendEmail(s"$AppName.appname MopCorncernStatistic", tfreq.mkString(", ")).send
        Logger.info("MopCorncernStatistic:  " + tfreq.mkString(", ")) 
      }
    
    //General Errors - Reporter("id", "ActorInitializationException", ex.getMessage, s"line $LINE of file $FILE", date)
    case FlushReporter =>
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
      val subject = AppName.appname + "Reporter"
      val message = "Caught errors in actors"
      val file = new File("reporters.txt")
      val bw = new BufferedWriter(new FileWriter(file))
      val attachmentnamefile: Map[String, File] = Map()
      val rvalues = reporters.groupBy(x=>x.typ) map { case (k, v) => v.takeRight(10) }
      reporters map { x => 
        bw.write(x.toString)        
        Logger.info(date + "Reporter:" + x.toString) 
      }
      bw.close() 
      reporters.clear
      if(file.length() == 0){
        attachmentnamefile + ((date +".txt") -> file)
        emailrecievers map {email =>
          SendAnyEmail(subject,message,attachmentnamefile,email).send
        }
      }
    
      //Actors Errors - Announcer(id, "ActorRestartException", reason.getMessage, "none", s"line $LINE of file $FILE", date)  
    case FlushAnnouncer =>
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
      val subject = AppName.appname + "Announcer"
      val message = "Caught all actors exception"
      val file = new File("announcers.txt")
      val bw = new BufferedWriter(new FileWriter(file))
      val attachmentnamefile: Map[String, File] = Map()
      val avalues = announcers.groupBy(x=>x.typ) map { case (k, v) => v.takeRight(10) }
      announcers map { x => 
            bw.write(x.toString)
            Logger.info(date + "Announcer:" + x.toString)
      }
      bw.close() 
      announcers.clear
      if(file.length() == 0){
        attachmentnamefile + ((date +".txt") -> file)
        emailrecievers map {email =>
          SendAnyEmail(subject,message,attachmentnamefile,email).send
        }
      }
     //Websock Errors- Divulger(user, out, js) from 
    case FlushDivulger =>
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
      val subject = AppName.appname + "Divulger"
      val message = "Mopped up all messages not caught in Websocket"
      val file = new File("divulgers.txt")
      val bw = new BufferedWriter(new FileWriter(file))
      val attachmentnamefile: Map[String, File] = Map()      
      val dvalues = divulgers.groupBy(x=>x.user.username) map { case (k, v) => v.takeRight(10) }
      divulgers map {x =>
        Try(Json.prettyPrint(x.obj)).toOption match {
          case Some(obj) => 
            bw.write(x.user.username.getOrElse(""))
            bw.write(obj)
            userDAO.save(
              x.user.copy(securitythreat=x.user.securitythreat ++ List(date+" : "+ obj))
            )
            Logger.info(date + "Divulger:" + x.user.username.getOrElse("") + " => " + obj)
          case None =>
            bw.write(x.user.username.getOrElse(""))
            bw.write(x.obj.toString)
            Try(userDAO.save(
              x.user.copy(securitythreat=x.user.securitythreat ++ List(date+" : "+x.obj.toString)))
            )
            Logger.info(date + "Divulger:" + x.user.username.getOrElse("") + " => " + x.toString)
        }
      }
      bw.close() 
      reporters.clear
      if(file.length() == 0){
        attachmentnamefile + ((date +".txt") -> file)
        emailrecievers map {email =>
          SendAnyEmail(subject,message,attachmentnamefile,email).send
        }
      }

    //Actor Mop Unknowns - Talker(x) Catch all unkowns in secondary Actors
    case FlushTalker =>
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
      val subject = AppName.appname + "Talker"
      val message = "Mopped up all messages not caught in actors"
      val file = new File("talkers.txt")
      val bw = new BufferedWriter(new FileWriter(file))
      val attachmentnamefile: Map[String, File] = Map()      
      val tvalues = talkers.groupBy(x=>x) map { case (k, v) => v.takeRight(10) }
      talkers map {x =>
        bw.write(x.toString)
        Logger.info(date + "Talker:" + x.toString)
      }
      bw.close()
      talkers.clear 
      if(file.length() == 0){
        attachmentnamefile + ((date +".txt") -> file)
        emailrecievers map {email =>
          SendAnyEmail(subject,message,attachmentnamefile,email).send
        }
      }

    case HealthCheck => 
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
        if (breakerIsOpen){} else {
          sender ! ("ChildErrorReporterActor" + "=" + date + ":")
        }
        
    case x => self ! (Talker(x.toString))

  }
}

  
object ErrorconFig {
    val system = akka.actor.ActorSystem("system")
    val c = ConfigFactory.load()
    c.checkValid(ConfigFactory.defaultReference(), "errorreporteractor")
    val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")
    val initialsize = c.getInt("errorreporteractor.startingRouteeNumber")
    val withintimerange = c.getDuration("errorreporteractor.supervisorStrategy.withinTimeRange", TimeUnit.MILLISECONDS) //should be(1 * 1000)
    val maxnrofretries = c.getInt("errorreporteractor.supervisorStrategy.maxNrOfRetries")  
    val maxfailures = c.getInt("errorreporteractor.breaker.maxFailures")
    val calltimeout = c.getDuration("errorreporteractor.breaker.callTimeout", TimeUnit.MILLISECONDS)
    val resettimeout = c.getDuration("errorreporteractor.breaker.resetTimeout", TimeUnit.MILLISECONDS)
    val initialdelay = c.getDuration("errorreporteractor.scheduler.initialDelay", TimeUnit.MILLISECONDS) 
    val interval =  c.getDuration("errorreporteractor.scheduler.interval", TimeUnit.MILLISECONDS)
    val emailrecievers = c.getStringList("email.recievers") 
    val sla = "sla"
    val partition = 1

  val breaker =
    new CircuitBreaker(system.scheduler,
      maxFailures = maxfailures,
      callTimeout = calltimeout milliseconds,
      resetTimeout = resettimeout milliseconds)
  breaker.onClose({
    breakerIsOpen = false
    // val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
    // self ! (Reporter("CircuitBreakerClosed", s"resetTimeout: $resettimeout  callTimeout: $calltimeout  maxFailures: $maxfailures", s"line $LINE of file $FILE", date))
  })
  breaker.onOpen({
    breakerIsOpen = true
    // val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
    // self ! (Reporter("CircuitBreakerOpen", s"resetTimeout: $resettimeout  callTimeout: $calltimeout  maxFailures: $maxfailures", s"line $LINE of file $FILE", date)) 
  })
  var breakerIsOpen = false
}

