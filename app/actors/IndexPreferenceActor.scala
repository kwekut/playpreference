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


object IndexPreferenceActor {
  case object CreateIndexPreferenceDB
  case object Take
  val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")
}

class IndexPreferenceActor @Inject() (
      @Named("error-actor") errorActor: ActorRef) extends Actor {
    import IndexPreferenceActor._
    import IndexPreferenceReciever._
    import IPconFig._
    val system = akka.actor.ActorSystem("system")
    import system.dispatcher
    var prompt = 0
    var preferences: List[String] = List()
    val client: Client = ElasticClient.client
    val consumer = new KafkaConsumer(Consumer.props)

    override val supervisorStrategy =
      OneForOneStrategy(maxNrOfRetries = maxnrofretries, withinTimeRange = withintimerange milliseconds) {
        case aIE: ActorInitializationException => SendEmail("ActorInitializationException - KafkaProdChildActor Shut:", aIE.getMessage).send; Stop 
        case aKE: ActorKilledException => SendEmail("ActorKilledException - KafkaProdChildActor Shut:", aKE.getMessage).send; Stop 
        case uE: Exception if prompt < 4 => prompt + 1
          SendEmail("Exception -  Restart:", uE.getMessage).send;Restart
      }

    override def preStart() {
      val preftopik = preferencetopic.split(",").toList.asJava
      consumer.subscribe(preftopik)
    }

   val recActor: ActorRef = context.actorOf(BalancingPool(initialsize).props(IndexPreferenceReciever.props(errorActor)),"elasticrecieverrouter")

    def receive = { 
      //Create Index if it doesnt exist
      case CreateIndexPreferenceDB => 
        val response = Future { client.admin().indices().prepareCreate(ElasticClient.preferencename)
          .setSettings(
          Settings.builder()             
             .put("index.number_of_shards", ElasticClient.preferenceshards)
             .put("index.number_of_replicas", ElasticClient.preferencereplicas)
          ).get()
        }
        response onComplete {
          case Success(r)  => Logger.info("IndexPreferenceClient prepareIndex Success" + Try(r.toString).toOption.toString)
          case Failure(failure) => Logger.info("IndexPreferenceClient prepareIndex Failure" + failure.getMessage)
        }

      case Take => 
        recActor ! Load(preferences.head)
        preferences = preferences.drop(1)

      case x => recActor forward x 

    }
  
    // Consumer preference from preftopic(preference) and formard to child
    system.scheduler.schedule(initialdelay milliseconds, interval milliseconds) {
      breaker.withCircuitBreaker(
        Future{
            val stream = consumer.poll(0)
            for {rec <- stream} yield preferences :+ (new String(rec.value(), "UTF-8"))
        }
      )
    }
  }


object IndexPreferenceReciever {
  def props(errorActor: ActorRef): Props = 
      Props(new IndexPreferenceReciever(errorActor))
  case class Load( msg: String )
}
class IndexPreferenceReciever ( errorActor: ActorRef) extends Actor {
  import IndexPreferenceReciever._
  import IndexPreferenceActor._
  import IPconFig._
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
    //Update a user's/shop's preference object with new preference data
    //Update a user's/shop's preference object with new preference data
    case Load(jpreference) => 
      context.parent ! Take 
      Json.parse(jpreference).validate[Preference] match {
        case preference: JsSuccess[Preference] => 
        val trans = Future{ Json.parse(client.prepareGet("preferencedb", "preferencestable", preference.get.userid).get().getSourceAsString()).asOpt[Pref].get } 
        trans onComplete {
          case Success(pref)  => 
            val indexRequest:IndexRequest = new IndexRequest("preferencedb", "preferencestable", preference.get.userid)
            .source(Json.toJson(Pref.newPref(preference.get)).toString)
            val upReq: UpdateRequest = new UpdateRequest("preferencedb", "preferencestable", preference.get.userid)
              .doc(Json.toJson(Pref.addPreference(pref, preference.get)).toString)
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
                  Logger.info("IndexingError: IndexPreferenceActor - Error on bulk indexing" + bulkResponse.buildFailureMessage() .toString + bulkResponse.getItems().toString)
                  SendEmail("IndexingError: IndexPreferenceActor - Error on bulk indexing", Try(bulkResponse.buildFailureMessage().toString + bulkResponse.getItems().toString).toOption.toString).send
                } else {
                  Logger.info("Count reached elasticactor bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)
                  cntr = 0
                } 
            }    
          // if object doesnt exist, create new       
          case Failure(failure) => 
            cntr += 1
            Logger.info("cnt increase" + cntr.toString)
            val indexRequest:IndexRequest = new IndexRequest("preferencedb", "preferencestable", preference.get.userid)
                .source(Json.toJson(Pref.newPref(preference.get)).toString)          
            bulkRequest.add(indexRequest)
            Logger.info("add to bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)          
            //Bulk update every 500
              if (cntr >= batchsavesize){
                val bulkResponse: BulkResponse = bulkRequest.get()
                if (bulkResponse.hasFailures()) {
                  Logger.info("IndexingError: IndexPreferenceActor - Error on bulk indexing")
                  SendEmail("IndexingError: IndexPreferenceActor - Error on bulk indexing", Try(bulkResponse.buildFailureMessage().toString + bulkResponse.getItems().toString).toOption.toString).send
                } else {
                  Logger.info("Count reached elasticactor bulkRequest:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)
                  cntr = 0
                }  
              }            
        }
        case e: JsError => 
          Logger.info("ParseError: IndexPreferenceActor - Error on IndexPreference parse" + JsError.toJson(e).toString)
          SendEmail("ParseError: IndexPreferenceActor - Error on IndexPreference parse", JsError.toJson(e).toString).send
      }

    case HealthCheck => 
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
        if (breaker.isOpen){} else {
          val trans = Future{ client.prepareGet("preferencedb", "preferencestable", "userid").get().getSourceAsString() } 
          trans onComplete {
            case Success(src)  =>
              sender ! ("IndexPreferenceActor" + "=" + date + ":")
            case Failure(failure) =>
          }
        }

    case x => errorActor ! (Talker(x.toString))

  }
}

  
object IPconFig {
    //var cntr: Int = 0
    //val client: Client = ElasticClient.client
    //var bulkRequest: BulkRequestBuilder = client.prepareBulk();
    val system = akka.actor.ActorSystem("system")
    val c = ConfigFactory.load()
    c.checkValid(ConfigFactory.defaultReference(), "indexpreferenceactor")
    
    val initialsize = c.getInt("indexpreferenceactor.startingRouteeNumber")
    val withintimerange = c.getDuration("indexpreferenceactor.supervisorStrategy.withinTimeRange", TimeUnit.MILLISECONDS) //should be(1 * 1000)
    val maxnrofretries = c.getInt("indexpreferenceactor.supervisorStrategy.maxNrOfRetries")  
    val maxfailures = c.getInt("indexpreferenceactor.breaker.maxFailures")
    val calltimeout = c.getDuration("indexpreferenceactor.breaker.callTimeout", TimeUnit.MILLISECONDS)
    val resettimeout = c.getDuration("indexpreferenceactor.breaker.resetTimeout", TimeUnit.MILLISECONDS)
    val initialdelay = c.getDuration("indexpreferenceactor.scheduler.initialDelay", TimeUnit.MILLISECONDS) 
    val interval =  c.getDuration("indexpreferenceactor.scheduler.interval", TimeUnit.MILLISECONDS)
    val batchsavesize = c.getInt("indexpreferenceactor.batchsave.size")
    val preftopic = c.getString("kafka.preferencetopic")

    val preferencepartition: Integer = c.getInt("kafka.preferencepartition")
    val preferencetopic = c.getString("kafka.preferencetopic")
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

