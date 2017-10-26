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
import scala.util.{Try, Success, Failure}
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


object IndexAbuseActor {
  //case object CreatePreferenceDB
  case object CreateAbuseIndexDB
  val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")
  case object Take
}

class IndexAbuseActor @Inject() (
      @Named("error-actor") errorActor: ActorRef,
      cassandraService: CassandraService)  extends Actor {
    import IndexAbuseActor._
    import IndexAbuseReciever._
    import IAconFig._
    val system = akka.actor.ActorSystem("system")
    import system.dispatcher
    val consumer = new KafkaConsumer(Consumer.props)
    var prompt = 0
    var abuses: List[String] = List()
    val client: Client = ElasticClient.client

    override val supervisorStrategy =
      OneForOneStrategy(maxNrOfRetries = maxnrofretries, withinTimeRange = withintimerange milliseconds) {
        case aIE: ActorInitializationException => SendEmail("ActorInitializationException - KafkaProdChildActor Shut:", aIE.getMessage).send; Stop 
        case aKE: ActorKilledException => SendEmail("ActorKilledException - KafkaProdChildActor Shut:", aKE.getMessage).send; Stop 
        case uE: Exception if prompt < 4 => prompt + 1
          SendEmail("Exception -  Restart:", uE.getMessage).send;Restart
      }

    override def preStart() {
      val topik = abusetopic.split(",").toList.asJava
      consumer.subscribe(topik)
    }

   val recActor: ActorRef = context.actorOf(BalancingPool(initialsize).props(IndexAbuseReciever.props(errorActor)),"elasticrecieverrouter")

    def receive = { 

      case CreateAbuseIndexDB => 
        val response = Future { client.admin().indices().prepareCreate(ElasticClient.abusename)
          .setSettings(
          Settings.builder()             
             .put("index.number_of_shards", ElasticClient.abuseshards)
             .put("index.number_of_replicas", ElasticClient.abusereplicas)
          ).get()
        }
        response onComplete {
          case Success(r)  => Logger.info("IndexAbuseClient prepareIndex Success" + Try(r.toString).toOption.toString)
          case Failure(failure) => Logger.info("IndexAbuseClient prepareIndex Failure" + failure.getMessage)
        }

      // Child pulls 1 at a time to process
      case Take => 
        recActor ! Load(abuses.head)
        abuses = abuses.drop(1)

      case x => recActor forward x 

    }
    //Todo - Mailbox overflow if # abuses is bigger than actor mailbox
    // Change maibox management to pull, thus keep buffer in circiutbreaker  
    // Consumer preference from preftopic(preference) and formard to child
    system.scheduler.schedule(initialdelay milliseconds, interval milliseconds) {
      breaker.withCircuitBreaker(
        Future{
                val stream = consumer.poll(0)
                for {rec <- stream} yield abuses :+ (new String(rec.value(), "UTF-8"))
        }
      )
    }
  }


object IndexAbuseReciever {
  def props(errorActor: ActorRef): Props = 
      Props(new IndexAbuseReciever(errorActor))
  case class Load(g: String)
}
class IndexAbuseReciever (errorActor: ActorRef) extends Actor {
  import IndexAbuseReciever._
  import IndexAbuseActor._
  import IAconFig._
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
    //Get scala case to string from kafka
    case Load(abuse) =>
    //Get/Prep next abuse for processing
        val id = UUID.randomUUID().toString
        if (Try(abuse.asInstanceOf[Abuser]).toOption.isDefined) { 
          val fd = abuse.asInstanceOf[Abuser]
          val indexRequest:IndexRequest = new IndexRequest("shopdb", "abusestable", id)
          .source(Json.toJson(fd).toString)
          val upReq: UpdateRequest = new UpdateRequest("shopdb", "abusestable", id)
            .doc(Json.toJson(fd).toString)
            .upsert(indexRequest)
            .retryOnConflict(batchsavesize) 
          bulkRequest.add(upReq)
          //val bulkResponse: BulkResponse = bulkRequest.get()
          Logger.info("add to bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)          
          //Bulk update every 500
          cntr += 1
          Logger.info("cnt increase" + cntr.toString)
          if (cntr >= batchsavesize){
            Logger.info("bulkRequest called")
              val bulkResponse: BulkResponse = bulkRequest.get()
              if (bulkResponse.hasFailures()) {
                Logger.info("IndexingError: IndexAbuseActor - Error on bulk indexing" + bulkResponse.buildFailureMessage() .toString + bulkResponse.getItems().toString)
                SendEmail("IndexingError: IndexAbuseActor - Error on bulk indexing", Try(bulkResponse.buildFailureMessage().toString + bulkResponse.getItems().toString).toOption.toString).send
              } else {
                Logger.info("Count reached elasticactor bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)
                cntr = 0
              } 
          }
      	} else {
          Logger.info("ParseError: IndexPreferenceActor - Error on IndexPreference parse" + (abuse).toString)
          SendEmail("ParseError: IndexPreferenceActor - Error on IndexPreference parse", (abuse).toString).send
        }
        context.parent ! Take 
           
    case HealthCheck => 
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
        if (breaker.isOpen){} else {
          val trans = Future{ client.prepareGet("shopdb", "abusestable", "userid").get().getSourceAsString() } 
          trans onComplete {
            case Success(src)  =>
              sender ! ("IndexAbuseActor" + "=" + date + ":")
            case Failure(failure) =>
          }
        }

    case x => errorActor ! (Talker(x.toString))

  }
}

  
object IAconFig {
    //var cntr: Int = 0
    //val client: Client = ElasticClient.client
    //var bulkRequest: BulkRequestBuilder = client.prepareBulk();
    val system = akka.actor.ActorSystem("system")
    val c = ConfigFactory.load()
    c.checkValid(ConfigFactory.defaultReference(), "indexabuseactor")
    
    val initialsize = c.getInt("indexabuseactor.startingRouteeNumber")
    val withintimerange = c.getDuration("indexabuseactor.supervisorStrategy.withinTimeRange", TimeUnit.MILLISECONDS) //should be(1 * 1000)
    val maxnrofretries = c.getInt("indexabuseactor.supervisorStrategy.maxNrOfRetries")  
    val maxfailures = c.getInt("indexabuseactor.breaker.maxFailures")
    val calltimeout = c.getDuration("indexabuseactor.breaker.callTimeout", TimeUnit.MILLISECONDS)
    val resettimeout = c.getDuration("indexabuseactor.breaker.resetTimeout", TimeUnit.MILLISECONDS)
    val initialdelay = c.getDuration("indexabuseactor.scheduler.initialDelay", TimeUnit.MILLISECONDS) 
    val interval =  c.getDuration("indexabuseactor.scheduler.interval", TimeUnit.MILLISECONDS)
    val batchsavesize = c.getInt("indexabuseactor.batchsave.size")

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


