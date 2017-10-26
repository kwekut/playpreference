package actors

import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import akka.actor.{ActorKilledException, ActorInitializationException}
import akka.pattern.pipe
import javax.inject._
import com.google.inject.name.Named
import com.google.inject.assistedinject.Assisted
import java.util.UUID
import play.api.Logger
import models.User
import scala.util.Try
import scala.util.{Success, Failure}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.language.postfixOps
import org.joda.time.LocalTime
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.google.common.io.Files
import java.io.File
import akka.actor.SupervisorStrategy._
import akka.routing._
import scala.concurrent.Future
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import models.PartialFunctions._
import services.kafkas._
import services.elastic._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import scala.util.Random
import scala.concurrent.duration._
import akka.pattern._
import akka.pattern.CircuitBreaker
import akka.pattern.pipe
import play.api.Play.current
import org.elasticsearch.index.search.geo._
import org.elasticsearch.index.search._
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search._
import org.elasticsearch.action.search._
import org.elasticsearch.common.geo.ShapeRelation
import org.elasticsearch.common.geo.builders.ShapeBuilder 
//import org.elasticsearch.index.query.FilterBuilders._
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.index.query.QueryBuilder 
import org.elasticsearch.common.geo.GeoDistance
import org.elasticsearch.common.unit._
import org.elasticsearch.client._
import play.api.libs.concurrent.Execution.Implicits._
import models.services.{CassandraService,CassandraServiceImpl}
import models.mail._
import models.Locality._
import models._
import java.lang.Runtime

object UpdatePreferenceActor {
  case object Mount
  case object Take
  case class Jrime(usr: User)
}

class UpdatePreferenceActor @Inject() (
      @Named("error-actor") errorActor: ActorRef,
      cassandraService: CassandraService)  extends Actor {
    import UpdatePreferenceActor._
    import UpdatePreferenceChildActor._
    import ElasticConfig._
    implicit val ec = context.dispatcher
    var users: List[User] = List()
    val consumer = new KafkaConsumer(Consumer.props)
    var prompt = 0

    override val supervisorStrategy =
      OneForOneStrategy(maxNrOfRetries = maxnrofretries, withinTimeRange = withintimerange milliseconds) {
        case aIE: ActorInitializationException => SendEmail("ActorInitializationException - KafkaProdChildActor Shut:", aIE.getMessage).send; Stop 
        case aKE: ActorKilledException => SendEmail("ActorKilledException - KafkaProdChildActor Shut:", aKE.getMessage).send; Stop 
        case uE: Exception if prompt < 4 => prompt + 1
          SendEmail("Exception -  Restart:", uE.getMessage).send;Restart
      }
    override def preStart() {
    }

    val reader: ActorRef = context.actorOf(
      BalancingPool(initialsize).props(UpdatePreferenceChildActor.props(cassandraService, errorActor)), "updateprefrouter")
    
  def receive = {
    // Child pulls 1 at a time to process
    case Take => 
      reader ! Jrime(users.head)
      users = users.drop(1)

    case x => reader forward x
    }
  //Get Preferences from Elastic, process them and Save to Users preferences
  //Poll from saved data in cassandra
  // Change maibox management to pull, thus keep buffer in circiutbreaker
  // Dont send sensitive user data via kafka
    system.scheduler.schedule(initialdelay milliseconds, interval milliseconds) {
      breaker.withCircuitBreaker(
        Future{
            cassandraService.findUsers map (x=> if (users.length < 1){users = x} )
            //val users = cassandraService.findusers
            //users map { x=> x foreach (y => reader ! Jrime(y)) }
        }
      )
    }
}

object UpdatePreferenceChildActor {
  def props(cassandraService: CassandraService, errorActor: ActorRef): Props = 
      Props(new UpdatePreferenceChildActor(cassandraService, errorActor))
  val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")
}

class UpdatePreferenceChildActor(cassandraService: CassandraService, errorActor: ActorRef) extends Actor {
  import UpdatePreferenceChildActor._
  import UpdatePreferenceActor._
  import ElasticConfig._
  val client: Client = ElasticClient.client

  //If message causes a restart triggering exception, Warn user, Inform Administrator
  override def preRestart(reason: Throwable, message: Option[Any]) {
    message match {
      case Some(m) => SendEmail("ActorRestartException - KafkaProdChildActor Shut:", reason.getMessage).send
      case None => SendEmail("ActorRestartException - KafkaProdChildActor Shut:", reason.getMessage).send
    }
  }

  val system = akka.actor.ActorSystem("system")
  import system.dispatcher
    // system.scheduler.schedule(10 seconds, 15 seconds) {
    //   val us = ElasticClient.testuser
    //   self ! Jrime(Json.toJson(us).toString)
    // }

  def receive = {

//http://rajish.github.io/api/elasticsearch/0.20.0.Beta1-SNAPSHOT/org/elasticsearch/search/SearchHit.html
//Retrieve pref for each user
//Get users with same type of profile(keywords=attend same type, shopids=attend same shops)
//Get their followings and minus current users followings
//Trim by locattion
//Take top 500 and sort Desc and take 100
//Calculate suggestedshops/suggestedfollowers
      //top50keywords, top25shopids, top5locations, popularity
      //200km around the most popular location
      // most popular location =home/office/friends 
    case Jrime(user) =>
    context.parent ! Take
    Logger.info("You have sent an updatepreferenceactor Call:" + dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString)
    val trans = Future{ Json.parse(client.prepareGet("preferencedb", "preferencestable", user.userid).get().getSourceAsString()).asOpt[Pref].get } 
    trans onComplete {
      case Success(pref) =>
      if (pref.shoporcust == "customer") {
        ///////////Get Shopids for customers/////////////////
        val bq:QueryBuilder = boolQuery()
          .mustNot(termQuery("shoporcust", "shop"))
          .must(matchQuery("flatkeywords", pref.flattop50keywords))
          .should(matchQuery("flatshopids", pref.flattop25shopids));
        val gq: QueryBuilder = geoDistanceQuery("flattoplocation")  
          .point(pref.flattoplocation.split(",").head.toDouble, pref.flattoplocation.split(",").last.toDouble)                                 
          .distance(200, DistanceUnit.KILOMETERS)         
          .optimizeBbox("indexed")                         
          .geoDistance(GeoDistance.SLOPPY_ARC)
        val response: SearchResponse = client.prepareSearch()
          .setIndices(ElasticClient.preferencename)
          .setTypes(ElasticClient.preferencetype)
          .setQuery(bq)
          //.setPostFilter(gq)
          .setSize(1500)
          .addSort("shopscount", SortOrder.ASC)
          .execute()
          .actionGet()
          val res:SearchHits = response.getHits() 
          val re: Array[SearchHit] = res.getHits()
          val ra: Array[Set[String]] = re collect(ehitTOshopidsPF)
          val r: Set[String] = ra.flatten.toSet
          val newshoplist: Set[String] = (r -- (pref.shopids.toSet ++ pref.suggestedshops.toSet)).toSet

        //Get shopids
        //Get shop top 10 products, limit to 5, 
        //exc mail, reservtion, expired events, expired promo
        ///////////////////Get Productids for customer////////////
        val bq1:QueryBuilder = boolQuery()
          .mustNot(termQuery("shoporcust", "customer"))
          .mustNot(matchQuery("userid", newshoplist.mkString(" ")))
          // .must(matchQuery("flattop50productids", pref.flattop50keywords))
          // .should(matchQuery("flatshopids", pref.flattop25shopids))
        val response1: SearchResponse = client.prepareSearch()
          .setIndices(ElasticClient.preferencename)
          .setTypes(ElasticClient.preferencetype)
          .setQuery(bq1)
          .setSize(1500)
          .addSort("shopscount", SortOrder.ASC)
          .execute()
          .actionGet()          
          val res1:SearchHits = response.getHits() 
          val re1: Array[SearchHit] = res1.getHits()
          val ra1: Array[Set[String]] = re1 collect(ehitTOprodidsPF)
          val r1: Set[String] = ra1.flatten.toSet
          val newprodlist: Set[String] = (r1 -- (pref.productids.toSet ++ user.feedpreferences.toSet)).toSet
          ///////////////Save User Object///////////////////
          val newsuggestedprods = Random.shuffle(newprodlist).take(100).toList
          val newsuggestedshops = Random.shuffle(newshoplist).take(100).toList
          val updateduser = user.copy(shoppreferences = newsuggestedshops, feedpreferences = newsuggestedprods)
          cassandraService.saveUser(updateduser)

      } else if (pref.shoporcust == "shop") {
        ////////////For Shops/////////////////////////
        val bq:QueryBuilder = boolQuery()
          .mustNot(termQuery("shoporcust", "customer"))
          .should(matchQuery("flatcustomerids", pref.flatcustomerids));
        val gq: QueryBuilder = geoDistanceQuery("pin.location")  
            .point(pref.flattoplocation.split(",").head.toDouble, pref.flattoplocation.split(",").last.toDouble)                                 
            .distance(200, DistanceUnit.KILOMETERS)         
            .optimizeBbox("indexed")                         
            .geoDistance(GeoDistance.SLOPPY_ARC);
        val response: SearchResponse = client.prepareSearch()
            .setIndices(ElasticClient.preferencename)
            .setTypes(ElasticClient.preferencetype)
            .setQuery(bq)
            .setPostFilter(gq)
            .setSize(1500)
            .addSort("shopscount", SortOrder.ASC)
            .execute()
            .actionGet();
      
          val res:SearchHits = response.getHits() 
          val re: Array[SearchHit] = res.getHits()
          val ra: Array[Set[String]] = re collect(ehitTOcustomeridsPF)
          val r: Set[String] = ra.flatten.toSet
          val newsuggestedcustomers: Set[String] = (r -- (pref.customerids)).toSet
          val updateduser = user.copy(shoppreferences = newsuggestedcustomers.toList)
          cassandraService.saveUser(updateduser)
      
        }
      case Failure(e) =>
            Logger.info("ElasticGetError: UpdatePreferenceActor - Error on ElasticSearch user get" + e.getMessage.toString)
            SendEmail("ElasticGetError: UpdatePreferenceActor - Error on ElasticSearch user get", e.getMessage.toString).send           
    }  
    // Health check both the elastic actors and the elastic cluster
    case HealthCheck => 
        val adminid = "none"
        val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
        // val trans = Future{sc.cassandraTable[String]("usersdb", "poststable").select().where("userid = ?", adminid).limit(10).collect() }            
        // trans map (x=> ("ChildUpdatePreferenceActor" + "=" + date + ":")) pipeTo sender

    case x => errorActor ! (Talker(x.toString))

  }
}

object ElasticConfig {
    //var cntr: Int = 0
    //val client: Client = ElasticClient.client
    //var bulkRequest: BulkRequestBuilder = client.prepareBulk();
    val system = akka.actor.ActorSystem("system")
    val c = ConfigFactory.load()
    c.checkValid(ConfigFactory.defaultReference(), "updatepreferenceactor")
    
    val initialsize = c.getInt("updatepreferenceactor.startingRouteeNumber")
    val withintimerange = c.getDuration("updatepreferenceactor.supervisorStrategy.withinTimeRange", TimeUnit.MILLISECONDS) //should be(1 * 1000)
    val maxnrofretries = c.getInt("updatepreferenceactor.supervisorStrategy.maxNrOfRetries")  
    val maxfailures = c.getInt("updatepreferenceactor.breaker.maxFailures")
    val calltimeout = c.getDuration("updatepreferenceactor.breaker.callTimeout", TimeUnit.MILLISECONDS)
    val resettimeout = c.getDuration("updatepreferenceactor.breaker.resetTimeout", TimeUnit.MILLISECONDS)
    val initialdelay = c.getDuration("updatepreferenceactor.scheduler.initialDelay", TimeUnit.MILLISECONDS) 
    val interval =  c.getDuration("updatepreferenceactor.scheduler.interval", TimeUnit.MILLISECONDS)

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


