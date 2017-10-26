package actors

import akka.actor._
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
import akka.actor.{Props, Actor}
import akka.actor.{ActorKilledException, ActorInitializationException}
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.Try
import scala.util.{Success, Failure}
import akka.actor.SupervisorStrategy._
import org.apache.kafka.clients.consumer._
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
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import services.kafkas.{Consumer, Producer}
import services.kafkas._
import services.elastic._
import play.api.Play.current
import scala.language.postfixOps
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.bulk._
import org.elasticsearch.action.update._
import org.elasticsearch.client._
import org.elasticsearch.common.settings.Settings
import models.services.{CassandraService,CassandraServiceImpl}
import models.daos.core._
import models.daos.core.Tables._
import models.mail._
import models.Locality._
import models._
import java.lang.Runtime
//import org.elasticsearch.common.xcontent.XContentFactory.*;


object IndexErrorActor {
  //case object CreatePreferenceDB
  case object CreateErrorIndexDB
  val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")
  case object Take
}

class IndexErrorActor @Inject() (
  @Named("error-actor") errorActor: ActorRef,
      cassandraService: CassandraService)  extends Actor {
    import IndexErrorActor._
    import IndexErrorReciever._
    import IEconFig._
    val system = akka.actor.ActorSystem("system")
    import system.dispatcher
    val consumer = new KafkaConsumer(Consumer.props)
    var prompt = 0
    var errors: List[String] = List()
    val client: Client = ElasticClient.client

    override val supervisorStrategy =
      OneForOneStrategy(maxNrOfRetries = maxnrofretries, withinTimeRange = withintimerange milliseconds) {
        case aIE: ActorInitializationException => SendEmail("ActorInitializationException - KafkaProdChildActor Shut:", aIE.getMessage).send; Stop 
        case aKE: ActorKilledException => SendEmail("ActorKilledException - KafkaProdChildActor Shut:", aKE.getMessage).send; Stop 
        case uE: Exception if prompt < 4 => prompt + 1
          SendEmail("Exception -  Restart:", uE.getMessage).send;Restart
      }

    override def preStart() {
      val topik = errortopic.split(",").toList.asJava
      consumer.subscribe(topik)
    }

   val recActor: ActorRef = context.actorOf(BalancingPool(initialsize).props(IndexErrorReciever.props(errorActor)),"elasticrecieverrouter")

    def receive = { 

      case CreateErrorIndexDB => 
        val response = Future { client.admin().indices().prepareCreate(ElasticClient.errorname)
          .setSettings(
          Settings.builder()             
             .put("index.number_of_shards", ElasticClient.errorshards)
             .put("index.number_of_replicas", ElasticClient.errorreplicas)
          ).get()
        }
        response onComplete {
          case Success(r)  => Logger.info("IndexErrorClient prepareIndex Success" + Try(r.toString).toOption.toString)
          case Failure(failure) => Logger.info("IndexErrorClient prepareIndex Failure" + failure.getMessage)
        }

      // Child pulls 1 at a time to process
      case Take => 
        recActor ! Load(errors.head)
        errors = errors.drop(1)

      case x => recActor forward x 

    }
    //Todo - Mailbox overflow if # errors is bigger than actor mailbox
    // Change maibox management to pull, thus keep buffer in circiutbreaker  
    // Consumer preference from preftopic(preference) and formard to child
    system.scheduler.schedule(initialdelay milliseconds, interval milliseconds) {
      breaker.withCircuitBreaker(
        Future{
                val stream = consumer.poll(0)
                for {rec <- stream} yield errors :+ (new String(rec.value(), "UTF-8"))
        }
      )
    }
  }


object IndexErrorReciever {
  def props(errorActor: ActorRef): Props = 
      Props(new IndexErrorReciever(errorActor))
  case class Load( msg: String )
}
class IndexErrorReciever (errorActor: ActorRef) extends Actor {
  import IndexErrorReciever._
  import IndexErrorActor._
  import IEconFig._
  private var cntr: Int = 0
  private var cnt: Int = 0
  val client: Client = ElasticClient.client
  var bulkRequest: BulkRequestBuilder = client.prepareBulk()
  val system = akka.actor.ActorSystem("system")
  import system.dispatcher

  //If message causes a restart triggering exception, Warn user, Inform Administrator
  override def preRestart(reason: Throwable, message: Option[Any]) {
    message match {
      case Some(m) => SendEmail("ActorRestartException - KafkaProdChildActor Shut:", reason.getMessage).send
      case None => SendEmail("ActorRestartException - KafkaProdChildActor Shut:", reason.getMessage).send
    }
  }
    // system.scheduler.schedule(10 seconds, 15 seconds) {
    //   val pr = Json.toJson(ElasticClient.testpreference).toString
    //   self ! Load(pr)
    // }

  def receive = {
    //Get Error from dbase, transform to Msg and update index if object has changed
    case Load(error) =>
      var save = ""
      val id = UUID.randomUUID().toString
      if (Try(error.asInstanceOf[Announcer]).toOption.isDefined){
          val mg = error.asInstanceOf[Announcer]
          save = Json.toJson(mg).toString
      } else if (Try(error.asInstanceOf[JDivulger]).toOption.isDefined){
          val mg = error.asInstanceOf[JDivulger]
          save = Json.toJson(mg).toString
      } else if (Try(error.asInstanceOf[Talker]).toOption.isDefined){
          val mg = error.asInstanceOf[Talker]
          save = Json.toJson(mg).toString
      } else if (Try(error.asInstanceOf[Reporter]).toOption.isDefined){  
          val mg = error.asInstanceOf[Reporter]
          save = Json.toJson(mg).toString
      }
      val indexRequest:IndexRequest = new IndexRequest("shopdb", "errorstable", id)
        .source(save)
        val upReq: UpdateRequest = new UpdateRequest("shopdb", "errorstable", id)
          .doc(Json.toJson(save).toString)
          .upsert(indexRequest)
          .retryOnConflict(batchsavesize) 
        bulkRequest.add(upReq)
        //val bulkResponse: BulkResponse = bulkRequest.get()
        Logger.info("add to bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)          
        cntr += 1
        Logger.info("cnt increase" + cntr.toString)
        if (cntr >= batchsavesize){
          Logger.info("bulkRequest called")
            val bulkResponse: BulkResponse = bulkRequest.get()
            if (bulkResponse.hasFailures()) {
              Logger.info("IndexingError: IndexErrorActor - Error on bulk indexing" + bulkResponse.buildFailureMessage() .toString + bulkResponse.getItems().toString)
              SendEmail("IndexingError: IndexErrorActor - Error on bulk indexing", Try(bulkResponse.buildFailureMessage().toString + bulkResponse.getItems().toString).toOption.toString).send
            } else {
              Logger.info("Count reached elasticactor bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)
              cntr = 0
            } 
        }
        context.parent ! Take

    case HealthCheck => 
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
        if (breaker.isOpen){} else {
          val trans = Future{ client.prepareGet("shopdb", "errorstable", "userid").get().getSourceAsString() } 
          trans onComplete {
            case Success(src)  =>
              sender ! ("IndexErrorActor" + "=" + date + ":")
            case Failure(failure) =>
          }
        }

    case x => errorActor ! (Talker(x.toString))

  }
}

  
object IEconFig {
    //var cntr: Int = 0
    //val client: Client = ElasticClient.client
    //var bulkRequest: BulkRequestBuilder = client.prepareBulk();
    val system = akka.actor.ActorSystem("system")
    val c = ConfigFactory.load()
    c.checkValid(ConfigFactory.defaultReference(), "indexerroractor")
    
    val initialsize = c.getInt("indexerroractor.startingRouteeNumber")
    val withintimerange = c.getDuration("indexerroractor.supervisorStrategy.withinTimeRange", TimeUnit.MILLISECONDS) //should be(1 * 1000)
    val maxnrofretries = c.getInt("indexerroractor.supervisorStrategy.maxNrOfRetries")  
    val maxfailures = c.getInt("indexerroractor.breaker.maxFailures")
    val calltimeout = c.getDuration("indexerroractor.breaker.callTimeout", TimeUnit.MILLISECONDS)
    val resettimeout = c.getDuration("indexerroractor.breaker.resetTimeout", TimeUnit.MILLISECONDS)
    val initialdelay = c.getDuration("indexerroractor.scheduler.initialDelay", TimeUnit.MILLISECONDS) 
    val interval =  c.getDuration("indexerroractor.scheduler.interval", TimeUnit.MILLISECONDS)
    val batchsavesize = c.getInt("indexerroractor.batchsave.size")
  
    val errorpartition: Integer = c.getInt("kafka.errorpartition")
    val errortopic = c.getString("kafka.errortopic")
    val abusepartition: Integer = c.getInt("kafka.abusepartition")
    val abusetopic = c.getString("kafka.abusetopic")

  val breaker =
    new CircuitBreaker(system.scheduler,
      maxFailures = maxfailures,
      callTimeout = calltimeout milliseconds,
      resetTimeout = resettimeout milliseconds)
  breaker.onClose({
    breakerIsOpen = false
    SendEmail("CircuitBreakerOpen", s"resetTimeout: $resettimeout  callTimeout: $calltimeout  maxFailures: $maxfailures").send 
  })
  breaker.onOpen({
    breakerIsOpen = true
    SendEmail("CircuitBreakerClosed", s"resetTimeout: $resettimeout  callTimeout: $calltimeout  maxFailures: $maxfailures").send 
  })
  var breakerIsOpen = false
}


