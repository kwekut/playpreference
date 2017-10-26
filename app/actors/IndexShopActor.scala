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


object IndexShopActor {
  case object CreateIndexShopDB
  case object Take
  val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")
}

class IndexShopActor @Inject() (
      @Named("error-actor") errorActor: ActorRef,
      cassandraService: CassandraService)  extends Actor {
    import IndexShopActor._
    import IndexShopReciever._
    import ISconFig._
    val system = akka.actor.ActorSystem("system")
    import system.dispatcher
    var prompt = 0
    var shops: List[Shop] = List()
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

   val recActor: ActorRef = context.actorOf(BalancingPool(initialsize).props(IndexShopReciever.props(errorActor)),"elasticrecieverrouter")

    def receive = { 

      case CreateIndexShopDB => 
        val response = Future { client.admin().indices().prepareCreate(ElasticClient.shopname)
          .setSettings(
          Settings.builder()             
             .put("index.number_of_shards", ElasticClient.shopshards)
             .put("index.number_of_replicas", ElasticClient.shopreplicas)
          ).get()
        }
        response onComplete {
          case Success(r)  => Logger.info("IndexShopClient prepareIndex Success" + Try(r.toString).toOption.toString)
          case Failure(failure) => Logger.info("IndexShopClient prepareIndex Failure" + failure.getMessage)
        }

      // Child pulls 1 at a time to process
      case Take => 
        recActor ! Load(shops.head)
        shops = shops.drop(1)

      case x => recActor forward x 

    }
    //Periodically Index all Shops in elastic for the search shop service
    //Poll from saved data in cassandra
    // Change maibox management to pull, thus keep buffer in circiutbreaker then
    //monitor time to finish with circuitbreaker  
    system.scheduler.schedule(initialdelay milliseconds, interval milliseconds) {
      breaker.withCircuitBreaker(
        Future{
    			cassandraService.findShops map (x=> if (shops.length < 1){shops = x} )
    			//shops map { x=> recActor ! Load(x) }
    		}
      )
    }
  }


object IndexShopReciever {
  def props(errorActor: ActorRef): Props = 
      Props(new IndexShopReciever(errorActor))
  case class Load( sg: Shop )
}
class IndexShopReciever ( errorActor: ActorRef) extends Actor {
  import IndexShopReciever._
  import IndexShopActor._
  import ISconFig._
  //implicit val ec = context.dispatcher
  private var cntr: Int = 0
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
     //Get Shop from dbase, transform to Shp and update index if object has changed
    case Load(shop) => 
      context.parent ! Take 
        val sp= Shop.buildShp(shop)
        val trans = Future{ Json.parse(client.prepareGet("shopdb", "shopstable", sp.id).get().getSourceAsString()).asOpt[Pref].get } 
        trans onComplete {
          case Success(shp)  => 
          if (sp == shp) { } else {
            val indexRequest:IndexRequest = new IndexRequest("shopdb", "shopstable", sp.id)
            .source(Json.toJson(sp).toString)
            val upReq: UpdateRequest = new UpdateRequest("shopdb", "shopstable", sp.id)
              .doc(Json.toJson(sp).toString)
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
                  Logger.info("IndexingError: IndexShopActor - Error on bulk indexing" + bulkResponse.buildFailureMessage() .toString + bulkResponse.getItems().toString)
                  SendEmail("IndexingError: IndexShopActor - Error on bulk indexing", Try(bulkResponse.buildFailureMessage().toString + bulkResponse.getItems().toString).toOption.toString).send
                } else {
                  Logger.info("Count reached elasticactor bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)
                  cntr = 0
                } 
            } 
          }  
          //Index if its a new shop object        
          case Failure(failure) => 
            cntr += 1
            Logger.info("cnt increase" + cntr.toString)
            val indexRequest:IndexRequest = new IndexRequest("shopdb", "shopstable", sp.id)
                .source(Json.toJson(sp).toString)          
            bulkRequest.add(indexRequest)
            Logger.info("add to bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)          
            //Bulk update every 500
              if (cntr >= batchsavesize){
                val bulkResponse: BulkResponse = bulkRequest.get()
                if (bulkResponse.hasFailures()) {
                  Logger.info("IndexingError: IndexShopActor - Error on bulk indexing")
                  SendEmail("IndexingError: IndexShopActor - Error on bulk indexing", Try(bulkResponse.buildFailureMessage().toString + bulkResponse.getItems().toString).toOption.toString).send
                } else {
                  Logger.info("Count reached elasticactor bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)
                  cntr = 0
                }  
              }            
        }

    case HealthCheck => 
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
        if (breaker.isOpen){} else {
          val trans = Future{ client.prepareGet("shopdb", "shopstable", "userid").get().getSourceAsString() } 
          trans onComplete {
            case Success(src)  =>
              sender ! ("IndexShopActor" + "=" + date + ":")
            case Failure(failure) =>
          }
        }

    case x => errorActor ! (Talker(x.toString))

  }
}

  
object ISconFig {
    //var cntr: Int = 0
    //val client: Client = ElasticClient.client
    //var bulkRequest: BulkRequestBuilder = client.prepareBulk();
    var CircuitBreakerState = "Closed"
    val system = akka.actor.ActorSystem("system")
    val c = ConfigFactory.load()
    c.checkValid(ConfigFactory.defaultReference(), "indexshopactor")
    
    val initialsize = c.getInt("indexshopactor.startingRouteeNumber")
    val withintimerange = c.getDuration("indexshopactor.supervisorStrategy.withinTimeRange", TimeUnit.MILLISECONDS) //should be(1 * 1000)
    val maxnrofretries = c.getInt("indexshopactor.supervisorStrategy.maxNrOfRetries")  
    val maxfailures = c.getInt("indexshopactor.breaker.maxFailures")
    val calltimeout = c.getDuration("indexshopactor.breaker.callTimeout", TimeUnit.MILLISECONDS)
    val resettimeout = c.getDuration("indexshopactor.breaker.resetTimeout", TimeUnit.MILLISECONDS)
    val initialdelay = c.getDuration("indexshopactor.scheduler.initialDelay", TimeUnit.MILLISECONDS) 
    val interval =  c.getDuration("indexshopactor.scheduler.interval", TimeUnit.MILLISECONDS)
    val batchsavesize = c.getInt("indexshopactor.batchsave.size")

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

