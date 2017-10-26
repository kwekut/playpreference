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
import services.elastic.ElasticClient
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


object IndexFeedActor {
  //case object CreatePreferenceDB
  case object CreateFeedIndexDB
  val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")
  case object Take
}

class IndexFeedActor @Inject() (
      @Named("error-actor") errorActor: ActorRef,
      cassandraService: CassandraService)  extends Actor {
    import IndexFeedActor._
    import IndexFeedReciever._
    import IFconFig._
    val system = akka.actor.ActorSystem("system")
    import system.dispatcher
    var prompt = 0
    var feeds: List[Feed] = List()
    val client: Client = ElasticClient.client

    override val supervisorStrategy =
      OneForOneStrategy(maxNrOfRetries = maxnrofretries, withinTimeRange = withintimerange milliseconds) {
        case aIE: ActorInitializationException => SendEmail("ActorInitializationException - KafkaProdChildActor Shut:", aIE.getMessage).send; Stop 
        case aKE: ActorKilledException => SendEmail("ActorKilledException - KafkaProdChildActor Shut:", aKE.getMessage).send; Stop 
        case uE: Exception if prompt < 4 => prompt + 1
          SendEmail("Exception -  Restart:", uE.getMessage).send;Restart
      }

    override def preStart() {
    }

   val recActor: ActorRef = context.actorOf(BalancingPool(initialsize).props(IndexFeedReciever.props(errorActor)),"elasticrecieverrouter")

    def receive = { 

      case CreateFeedIndexDB => 
        val response = Future { client.admin().indices().prepareCreate(ElasticClient.feedname)
          .setSettings(
          Settings.builder()             
             .put("index.number_of_shards", ElasticClient.feedshards)
             .put("index.number_of_replicas", ElasticClient.feedreplicas)
          ).get()
        }
        response onComplete {
          case Success(r)  => Logger.info("IndexFeedClient prepareIndex Success" + Try(r.toString).toOption.toString)
          case Failure(failure) => Logger.info("IndexFeedClient prepareIndex Failure" + failure.getMessage)
        }

      // Child pulls 1 at a time to process
      case Take => 
        recActor ! Load(feeds.head)
        feeds = feeds.drop(1)

      case x => recActor forward x 

    }
    //Periodically Index all Feed in elastic for the search feed service
    //Poll from saved data in cassandra
    // Change maibox management to pull, thus keep buffer in circiutbreaker  
    system.scheduler.schedule(initialdelay milliseconds, interval milliseconds) {
      breaker.withCircuitBreaker(
        Future{
    			cassandraService.findFeeds map (x=> if (feeds.length < 1){feeds = x} )
    			//feeds map { x=> recActor ! Load(x) }
        }
      )
    }
  }


object IndexFeedReciever {
  def props(errorActor: ActorRef): Props = 
      Props(new IndexFeedReciever(errorActor))
  case class Load( ms: Feed )
}
class IndexFeedReciever (errorActor: ActorRef) extends Actor {
  import IndexFeedReciever._
  import IndexFeedActor._
  import IFconFig._
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
    //Get Feed from dbase, transform to Msg and update index if object has changed
    case Load(feed) =>
      //Get/Prep next feed for processing
      context.parent ! Take 
        val trans = Future{ Json.parse(client.prepareGet("shopdb", "feedstable", feed.feedid).get().getSourceAsString()).asOpt[Msg].get } 
        trans onComplete {
          case Success(msg)  => 
          //update index only if feed object has changed
          if (msg == feed) { } else {
            val indexRequest:IndexRequest = new IndexRequest("shopdb", "feedstable", feed.feedid)
            .source(Json.toJson(feed).toString)
            val upReq: UpdateRequest = new UpdateRequest("shopdb", "feedstable", feed.feedid)
              .doc(Json.toJson(feed).toString)
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
                  Logger.info("IndexingError: IndexFeedActor - Error on bulk indexing" + bulkResponse.buildFailureMessage() .toString + bulkResponse.getItems().toString)
                  SendEmail("IndexingError: IndexFeedActor - Error on bulk indexing", Try(bulkResponse.buildFailureMessage().toString + bulkResponse.getItems().toString).toOption.toString).send
                } else {
                  Logger.info("Count reached elasticactor bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)
                  cntr = 0
                } 
            }
        	}
          case Failure(failure) => 
            cntr += 1
            Logger.info("cnt increase" + cntr.toString)
            val indexRequest:IndexRequest = new IndexRequest("shopdb", "feedstable", feed.feedid)
                .source(Json.toJson(feed).toString)          
            bulkRequest.add(indexRequest)
            Logger.info("add to bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)          
            //Bulk update every 500
              if (cntr >= batchsavesize){
                val bulkResponse: BulkResponse = bulkRequest.get()
                if (bulkResponse.hasFailures()) {
                  Logger.info("IndexingError: IndexFeedActor - Error on bulk indexing")
                  SendEmail("IndexingError: IndexFeedActor - Error on bulk indexing", Try(bulkResponse.buildFailureMessage().toString + bulkResponse.getItems().toString).toOption.toString).send
                } else {
                  Logger.info("Count reached elasticactor bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)
                  cntr = 0
                }  
              }            
        }

    case HealthCheck => 
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
        if (breaker.isOpen){} else {
          val trans = Future{ client.prepareGet("shopdb", "feedstable", "userid").get().getSourceAsString() } 
          trans onComplete {
            case Success(src)  =>
              sender ! ("IndexFeedActor" + "=" + date + ":")
            case Failure(failure) =>
          }
        }

    case x => errorActor ! (Talker(x.toString))

  }
}

  
object IFconFig {
    //var cntr: Int = 0
    //val client: Client = ElasticClient.client
    //var bulkRequest: BulkRequestBuilder = client.prepareBulk();
    val system = akka.actor.ActorSystem("system")
    val c = ConfigFactory.load()
    c.checkValid(ConfigFactory.defaultReference(), "indexfeedactor")
    
    val initialsize = c.getInt("indexfeedactor.startingRouteeNumber")
    val withintimerange = c.getDuration("indexfeedactor.supervisorStrategy.withinTimeRange", TimeUnit.MILLISECONDS) //should be(1 * 1000)
    val maxnrofretries = c.getInt("indexfeedactor.supervisorStrategy.maxNrOfRetries")  
    val maxfailures = c.getInt("indexfeedactor.breaker.maxFailures")
    val calltimeout = c.getDuration("indexfeedactor.breaker.callTimeout", TimeUnit.MILLISECONDS)
    val resettimeout = c.getDuration("indexfeedactor.breaker.resetTimeout", TimeUnit.MILLISECONDS)
    val initialdelay = c.getDuration("indexfeedactor.scheduler.initialDelay", TimeUnit.MILLISECONDS) 
    val interval =  c.getDuration("indexfeedactor.scheduler.interval", TimeUnit.MILLISECONDS)
    val batchsavesize = c.getInt("indexfeedactor.batchsave.size")

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


