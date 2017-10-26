package models.services

import java.util.UUID
import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Logger
import akka.util.Timeout
import akka.pattern.ask
import actors.CassandraReadActor._
import actors.CassandraWriteActor._
import akka.actor.{ ActorRef, ActorSystem, Props, Actor }
import javax.inject._
import com.google.inject.name.Named
import com.datastax.driver.core.{Row, BoundStatement, ResultSet, ResultSetFuture}
import models.daos.core.Tables._
import models.daos.core.cassandra.resultset._
import scala.util.Try
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.Silhouette._
import models.PartialFunctions._
import models._

/**
 * Give access to the user object using Slick
 */
class CassandraServiceImpl @Inject() ( @Named("cassandrawrite-actor") cassWriteActor: ActorRef,
                                @Named("cassandraread-actor") cassReadActor: ActorRef) extends CassandraService {
  import models.daos.core.cassandra.resultset._
  implicit val timeout: Timeout = 5.seconds
  val logger = Logger(this.getClass())


  def findUsers = {
    logger.info("CassandraServiceImpl findUsers:")
    val keyspace = userkeyspace
    val table = usertable
    val conditions = ""
    val columns = "payload"
    val usercolumn = cassReadActor ? FindColumn(keyspace, table, columns, conditions) 
    usercolumn.asInstanceOf[Future[List[PayloadColumn]]] map {prof =>
      {prof map (x=> x.payload collect(jcryptuserTOsuserPF))}.flatten
    }
  }


  def findFeeds = {
    logger.info("CassandraServiceImpl findFeeds:")
    val keyspace = userkeyspace
    val table = feedtable
    val conditions = ""
    val columns = "payload"
    val usercolumn = cassReadActor ? FindColumn(keyspace, table, columns, conditions) 
    usercolumn.asInstanceOf[Future[List[PayloadColumn]]] map {prof =>
      {prof map (x=> x.payload collect(jfeedTOsfeedPF))}.flatten
    }
  }

  def findShops = {
    logger.info("CassandraServiceImpl findShop:")
    val keyspace = shopkeyspace
    val table = shoptable
    val conditions = ""
    val columns = "payload"
    val usercolumn = cassReadActor ? FindColumn(keyspace, table, columns, conditions) 
    usercolumn.asInstanceOf[Future[List[PayloadColumn]]] map {prof =>
      {prof map (x=> x.payload collect(jshopTOsshopPF))}.flatten
    }
  }


  def saveUser(user: User, update: Boolean = false) = {
    logger.info("CassandraServiceImpl Save:" + user.toString)
    val keyspace = userkeyspace
    val table = usertable
    val userid = user.userid
    val email = user.email.getOrElse("")
    //val usr = Encryptor(Json.toJson(user).toString).userEncrypt
    val usr = {Some(user) collect suserTOjcryptuserPF}
    val fin = usr map { usrr=>
      val columns = "userid, email, payload"
      val values = s"'$userid', '$email', '$usrr'"
      cassWriteActor ? SaveColumn(keyspace, table, columns, values) map {x=> user}
    }
    fin.get
  }

  def saveUser(user: User) = {
    logger.info("CassandraServiceImpl Save:" + user.toString)
    val keyspace = userkeyspace
    val table = usertable
    val userid = user.userid
    val email = user.email.getOrElse("")
    //val usr = Encryptor(Json.toJson(user).toString).userEncrypt
    val usr = {Some(user) collect suserTOjcryptuserPF}
    val fin = usr map { usrr=>    
      val columns = "userid, email, payload"
      val values = s"'$userid', '$email', '$usrr'"
      cassWriteActor ? SaveColumn(keyspace, table, columns, values) map {x=> user}
    }
    fin.get  
  }


}

