package models.daos

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Logger
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.{ ActorRef, ActorSystem, Props, Actor }
import javax.inject._
import com.google.inject.name.Named
import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext
import com.mohiva.play.silhouette.api.StorableAuthenticator
//import com.mohiva.play.silhouette.impl.daos.AuthenticatorDAO
import com.mohiva.play.silhouette.api.repositories.AuthenticatorRepository
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.api.LoginInfo
import org.joda.time.DateTime
import com.mohiva.play.silhouette.api.StorableAuthenticator
import actors.CassandraReadActor._
import actors.CassandraWriteActor._
import com.datastax.driver.core.{Row, BoundStatement, ResultSet, ResultSetFuture}
import models.daos.core.Tables._
import models.daos.core.cassandra.resultset._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.Silhouette._
import models.PartialFunctions._
import models.CustomJWTAuthenticator._
import models._
//AuthenticatorRepository
//class FakeAuthenticatorRepository[T <: StorableAuthenticator] extends AuthenticatorRepository[T] {
class JWTAuthenticatorDAO @Inject() ( 
        cassWriteActor: ActorRef, cassReadActor: ActorRef)
                                        extends AuthenticatorRepository[JWTAuthenticator] {
  implicit val timeout: Timeout = 5.seconds
  val logger = Logger(this.getClass())

  def find(id: String): Future[Option[JWTAuthenticator]] = {
    logger.info("PersistAuthDAO findby id")
    val keyspace = userkeyspace
    val table = jwttable    
    val authid = id 
    val conditions = s"authid='$authid'"
    val columns = "payload"
    val usercolumn = cassReadActor ? FindColumn(keyspace, table, columns, conditions)
    usercolumn.asInstanceOf[Future[List[PayloadColumn]]] map {profile =>
      Try(
        ({profile.headOption map (x=> x.payload collect(jcryptljwtTOsjwtPF))}.flatten).get.asInstanceOf[JWTAuthenticator]
      ).toOption  
    }
  }

  def add(authenticator: JWTAuthenticator): Future[JWTAuthenticator] = {
    logger.info("PersistAuthDAO add")
    val keyspace = userkeyspace
    val table = jwttable 
    val authid = authenticator.id 
    val authenticatort = authenticator.asInstanceOf[JWTAuthenticator]
    //val authj = Json.toJson(CustomJWTAuthenticator.buildCustomJWTAuthenticator(authenticatort)).toString
    //val authenticatorj = Encryptor(authj).jwtEncrypt
    val auth = {Some(authenticatort) collect sjwtTOjcryptljwtPF}
    val fin = auth map { authenticatorj=>
      val columns = "authid, payload"
      val values = s"'$authid', '$authenticatorj'"
      cassWriteActor ? SaveColumn(keyspace, table, columns, values) map {x=> authenticator}
    }
    fin.get
  }

  def update(authenticator: JWTAuthenticator): Future[JWTAuthenticator] = {
    logger.info("PersistAuthDAO update")
    val keyspace = userkeyspace
    val table = jwttable 
    val authid = authenticator.id 
    val authenticatort = authenticator.asInstanceOf[JWTAuthenticator]
    val auth = {Some(authenticatort) collect sjwtTOjcryptljwtPF}
    val fin = auth map { authenticatorj=>
      val conditions = s"authid = '$authid'"
      val values = s"payload = '$authenticatorj'"      
      cassWriteActor ? SaveColumn(keyspace, table, values, conditions) map {x=> authenticator}
    }
    fin.get
  }

  def remove(id: String): Future[Unit] = {
    logger.info("PersistAuthDAO remove")
    val keyspace = userkeyspace
    val table = jwttable  
    val conditions = s"authid = '$id'"
    Future( cassWriteActor ! DeleteRow(keyspace, table, conditions) )
  }

}

