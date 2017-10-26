package services.elastic

import java.util.Properties
import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.util.ArrayList
import java.util.Arrays
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future
import scala.util.Try
import scala.util.{Success, Failure}
import javax.inject._
import com.google.inject.name.Named
import com.typesafe.config.ConfigFactory
import scala.util.Try
import scala.util.{Success, Failure}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models._
import java.util.UUID
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
//import org.elasticsearch.node.NodeBuilder._
//import org.elasticsearch.common.settings.ImmutableSettings._
import org.elasticsearch.common.settings.Settings
//import org.elasticsearch.common.settings.SettingsModule
//import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
//import org.elasticsearch.shield.authc.support.SecuredString
import java.net.InetAddress
//import java.net.InetSocketAddress
import org.elasticsearch.action.index.IndexResponse
import java.net.UnknownHostException
import org.elasticsearch.client._
import java.io.File

object ElasticClient {
  val c = ConfigFactory.load()
  c.checkValid(ConfigFactory.defaultReference(), "elasticclient")
  val myClusterName = c.getString("elasticclient.cluster.name")
  val sniff = c.getBoolean("elasticclient.client.transport.sniff")
  val ignoreclustername = c.getBoolean("elasticclient.client.transport.ignore_cluster_name")
  val hosts = c.getStringList("elasticclient.hosts")
  val port = c.getInt("elasticclient.port")

  val preferenceshards = c.getInt("elasticclient.preference.index.number_of_shards")
  val preferencereplicas = c.getInt("elasticclient.preference.index.number_of_replicas")            
  val preferenceindex = c.getString("elasticclient.preference.index.name")
  val preferencetype = c.getString("elasticclient.preference.index.type")

  val postshards = c.getInt("elasticclient.post.index.number_of_shards")
  val postreplicas = c.getInt("elasticclient.post.index.number_of_replicas")            
  val postindex = c.getString("elasticclient.post.index.name")
  val posttype = c.getString("elasticclient.post.index.type")
  
  
  val searchfeedshards = c.getInt("elasticclient.searchfeed.index.number_of_shards")
  val searchfeedreplicas = c.getInt("elasticclient.searchfeed.index.number_of_replicas")            
  val searchfeedname = c.getString("elasticclient.searchfeed.index.name")
  val searchfeedtype = c.getString("elasticclient.searchfeed.index.type")
  
  val searchshopshards = c.getInt("elasticclient.searchshop.index.number_of_shards")
  val searchshopreplicas = c.getInt("elasticclient.searchshop.index.number_of_replicas")            
  val searchshopname = c.getString("elasticclient.searchshop.index.name")
  val searchshoptype = c.getString("elasticclient.searchshop.index.type")

  val hintshards = c.getInt("elasticclient.hint.index.number_of_shards")
  val hintreplicas = c.getInt("elasticclient.hint.index.number_of_replicas")            
  val hintindex = c.getString("elasticclient.hint.index.name")
  val hinttype = c.getString("elasticclient.hint.index.type")

  val abuseshards = c.getInt("elasticclient.abuse.index.number_of_shards")
  val abusereplicas = c.getInt("elasticclient.abuse.index.number_of_replicas")            
  val abuseindex = c.getString("elasticclient.abuse.index.name")
  val abusetype = c.getString("elasticclient.abuse.index.type")

  val errorshards = c.getInt("elasticclient.error.index.number_of_shards")
  val errorreplicas = c.getInt("elasticclient.error.index.number_of_replicas")            
  val errorindex = c.getString("elasticclient.error.index.name")
  val errortype = c.getString("elasticclient.error.index.type")

  def client: Client = {
    val settings: Settings = Settings.settingsBuilder()
      .put("cluster.name", myClusterName)
      .put("client.transport.sniff", sniff).build()
    
    if (hosts.size == 1) {
        val host1 = hosts.head 
  	    TransportClient.builder().settings(settings).build()
          .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host1), port))
    } else {
        val host1 = hosts.head 
        val host2 = hosts.last 
        TransportClient.builder().settings(settings).build()
          .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host1), port))       
          .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host2), port))
    }
  }

  // val testuser = User("userid",Seq(LoginInfo("alfa", "beta")),
  // Some("firstname"),Some("lastname"),Some("fullname"),
  // Some("username"),Some("email"),Some("phone"),Some("address"),
  // Some("token"),List("feedpreferences"),List("shoppreferences"),
  // Set("user"),Set("shops"),Set("followings"),Some("avatarURL"),
  // "created")


  val testpreference = Preference("userid", "customer", List("transactionid"),
  List("expiry"),List("keywords"),List("shopids"),List("shopnames"),List("customerids"), 
  List("productids"), List("productnames"),List(50),List("5"),List("5"),
  List("41.12,-71.34"),List("typs"),List("activities"), List("suggestedshops"), 
  List("suggestedcustomers"),"created")

    val response = Future{client.prepareIndex("preferencedb", "preferencestable", testpreference.userid)
         .setSource(Json.toJson(Pref.newPref(testpreference)).toString).get()}
    response onComplete {
        case Success(r)  => Logger.info("ElasticClient prepareIndex Success: " + Try(r.toString).toOption.toString)
        case Failure(failure) => Logger.info("ElasticClient prepareIndex Failure: " + failure.getMessage)
    }
}


// @Singleton
// class ElasticClient @Inject() (appLifecycle: ApplicationLifecycle) {
// 	import ElasticClient._

//   appLifecycle.addStopHook { () =>
//     Future.successful(client.close())
//   }
// }
