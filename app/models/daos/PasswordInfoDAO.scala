package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import play.api.libs.concurrent.Execution.Implicits._
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits._
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

class PasswordInfoDAO @Inject() ( @Named("cassandrawrite-actor") cassWriteActor: ActorRef,
                                @Named("cassandraread-actor") cassReadActor: ActorRef)
                                                   extends DelegableAuthInfoDAO[PasswordInfo] {
  implicit val timeout: Timeout = 5.seconds
  val logger = Logger(this.getClass())
   //val optuser = {result.headOption map{x=> x.payload collect(jcryptuserTOsuserPF)}}.flatten                                                 
  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    logger.info("PasswordInfoDAO findby loginInfo:" + loginInfo.toString)
    val keyspace = userkeyspace
    val table = profiletable
    val providerKey = loginInfo.providerKey
    val providerID = loginInfo.providerID
    val conditions = s"providerkey='$providerKey' AND providerid='$providerID'"
    val columns = "payload"
    val usercolumn = cassReadActor ? FindColumn(keyspace, table, columns, conditions) 
    usercolumn.asInstanceOf[Future[List[PayloadColumn]]] map {profile => 
      {profile.headOption map (x=> x.payload collect(jcryptpwinfoTOspwinfoPF))}.flatten 
    }
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    logger.info("PasswordInfoDAO save:" + loginInfo.toString)
    val keyspace = userkeyspace
    val table = profiletable
    val providerKey = loginInfo.providerKey
    val providerID = loginInfo.providerID
    //val authinfo = Encryptor(Json.toJson(authInfo).toString).secretEncrypt
    val authinfo = {Some(authInfo) collect spwinfoTOjcryptpwinfoPF}
    val fin = authinfo map { ainfo=>
      //val ainfo = authinfo.get 
      val columns = "providerkey, providerid, payload"
      val values = s"'$providerKey', '$providerID', '$ainfo'"
      cassWriteActor ? SaveColumn(keyspace, table, columns, values) map {x=> authInfo}
    }
    fin.get
  }

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    logger.info("PasswordInfoDAO add")
    val keyspace = userkeyspace
    val table = profiletable
    val providerKey = loginInfo.providerKey
    val providerID = loginInfo.providerID
    //val authinfo = Encryptor(Json.toJson(authInfo).toString).secretEncrypt
    val authinfo = {Some(authInfo) collect spwinfoTOjcryptpwinfoPF}
    val fin = authinfo map { ainfo=>
      //val ainfo = authinfo.get 
      val columns = "providerkey, providerid, payload"
      val values = s"'$providerKey', '$providerID', '$ainfo'"
      cassWriteActor ? SaveColumn(keyspace, table, columns, values) map {x=> authInfo}
    }
    fin.get
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    logger.info("PasswordInfoDAO update")
    val keyspace = userkeyspace
    val table = profiletable
    val providerKey = loginInfo.providerKey
    val providerID = loginInfo.providerID
    //val authinfo = Encryptor(Json.toJson(authInfo).toString).secretEncrypt
    val authinfo = {Some(authInfo) collect spwinfoTOjcryptpwinfoPF}
    val fin = authinfo map { ainfo=>
      //val ainfo = authinfo.get   
      val conditions = s"providerkey = '$providerKey' AND providerid = '$providerID'"
      val values = s"providerKey = '$providerKey', providerID = '$providerID', payload = '$ainfo'"      
      cassWriteActor ? SaveColumn(keyspace, table, values, conditions) map {x=> authInfo}
    }
    fin.get
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    val keyspace = userkeyspace
    val table = profiletable
    val providerKey = loginInfo.providerKey
    val providerID = loginInfo.providerID
    //logger.info("PasswordInfoDAO Remove" )
    val conditions = s"providerkey = '$providerKey' AND providerid = '$providerID'"
    Future( cassWriteActor ! DeleteRow(keyspace, table, conditions) )
    //Database.execute(PasswordInfoQueries.removeById(Seq(loginInfo.providerID, loginInfo.providerKey))).map(x => Unit)
  }
}

// /**
//  * The DAO to store the password information.
//  */
// class PasswordInfoDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
//     extends DelegableAuthInfoDAO[PasswordInfo] with DAOSlick {

//   import driver.api._

//   protected def passwordInfoQuery(loginInfo: LoginInfo) = for {
//     dbLoginInfo <- loginInfoQuery(loginInfo)
//     dbPasswordInfo <- slickPasswordInfos if dbPasswordInfo.loginInfoId === dbLoginInfo.id
//   } yield dbPasswordInfo
  
//   // Use subquery workaround instead of join to get authinfo because slick only supports selecting
//   // from a single table for update/delete queries (https://github.com/slick/slick/issues/684).
//   protected def passwordInfoSubQuery(loginInfo: LoginInfo) =
//     slickPasswordInfos.filter(_.loginInfoId in loginInfoQuery(loginInfo).map(_.id))

//   protected def addAction(loginInfo: LoginInfo, authInfo: PasswordInfo) =
//     loginInfoQuery(loginInfo).result.head.flatMap { dbLoginInfo =>
//       slickPasswordInfos +=
//         DBPasswordInfo(authInfo.hasher, authInfo.password, authInfo.salt, dbLoginInfo.id.get)
//     }.transactionally
    
//   protected def updateAction(loginInfo: LoginInfo, authInfo: PasswordInfo) =
//     passwordInfoSubQuery(loginInfo).
//       map(dbPasswordInfo => (dbPasswordInfo.hasher, dbPasswordInfo.password, dbPasswordInfo.salt)).
//       update((authInfo.hasher, authInfo.password, authInfo.salt))
  
//   /**
//    * Finds the auth info which is linked with the specified login info.
//    *
//    * @param loginInfo The linked login info.
//    * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
//    */
//   def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
//     db.run(passwordInfoQuery(loginInfo).result.headOption).map { dbPasswordInfoOption =>
//       dbPasswordInfoOption.map(dbPasswordInfo => 
//         PasswordInfo(dbPasswordInfo.hasher, dbPasswordInfo.password, dbPasswordInfo.salt))
//     }
//   }

//   /**
//    * Adds new auth info for the given login info.
//    *
//    * @param loginInfo The login info for which the auth info should be added.
//    * @param authInfo The auth info to add.
//    * @return The added auth info.
//    */
//   def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
//     db.run(addAction(loginInfo, authInfo)).map(_ => authInfo)

//   /**
//    * Updates the auth info for the given login info.
//    *
//    * @param loginInfo The login info for which the auth info should be updated.
//    * @param authInfo The auth info to update.
//    * @return The updated auth info.
//    */
//   def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = 
//     db.run(updateAction(loginInfo, authInfo)).map(_ => authInfo)

//   /**
//    * Saves the auth info for the given login info.
//    *
//    * This method either adds the auth info if it doesn't exists or it updates the auth info
//    * if it already exists.
//    *
//    * @param loginInfo The login info for which the auth info should be saved.
//    * @param authInfo The auth info to save.
//    * @return The saved auth info.
//    */
//   def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
//     val query = loginInfoQuery(loginInfo).joinLeft(slickPasswordInfos).on(_.id === _.loginInfoId)
//     val action = query.result.head.flatMap {
//       case (dbLoginInfo, Some(dbPasswordInfo)) => updateAction(loginInfo, authInfo)
//       case (dbLoginInfo, None) => addAction(loginInfo, authInfo)
//     }
//     db.run(action).map(_ => authInfo)
//   }

//   /**
//    * Removes the auth info for the given login info.
//    *
//    * @param loginInfo The login info for which the auth info should be removed.
//    * @return A future to wait for the process to be completed.
//    */
//   def remove(loginInfo: LoginInfo): Future[Unit] =
//     db.run(passwordInfoSubQuery(loginInfo).delete).map(_ => ())
// }
