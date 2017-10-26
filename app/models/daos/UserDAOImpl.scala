package models.daos

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
class UserDAOImpl @Inject() ( @Named("cassandrawrite-actor") cassWriteActor: ActorRef,
                                @Named("cassandraread-actor") cassReadActor: ActorRef) extends UserDAO {
  import models.daos.core.cassandra.resultset._
  implicit val timeout: Timeout = 5.seconds
  val logger = Logger(this.getClass())

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = {
    logger.info("UserDAOImpl Findby loginInfo:" + loginInfo.toString)
    val keyspace = userkeyspace
    val table = usertable
    val email = loginInfo.providerKey
        val conditions = s"email='$email'"
        val columns = "payload"
        val usercolumn = cassReadActor ? FindColumn(keyspace, table, columns, conditions) 
        usercolumn.asInstanceOf[Future[List[PayloadColumn]]] map {prof => 
          logger.info("UserDAOImpl Findby loginInfo: return")
          logger.info("UserDAOImpl Findby loginInfo: return" + prof.toString)
          val user = {prof.headOption map (x=> x.payload collect(jcryptuserTOsuserPF))}.flatten
          logger.info("UserDAOImpl Findby loginInfo: user return:" + user.toString)
          if (user.isDefined) {
            logger.info("UserDAOImpl Findby loginInfo: user is defined")
              if ((user.get.profiles.filter(_.providerID == loginInfo.providerID)).nonEmpty) {
                  (user)
              } else {
                  (None)
              } 
          } else {
            logger.info("UserDAOImpl Findby loginInfo:user not defined")
            (None)
          }         
        }
  }

  def find(email: String) = {
    logger.info("UserDAOImpl Findby email:" + email.toString)
    val keyspace = userkeyspace
    val table = usertable
    val conditions = s"email='$email'"
    val columns = "payload"
    val usercolumn = cassReadActor ? FindColumn(keyspace, table, columns, conditions) 
    usercolumn.asInstanceOf[Future[List[PayloadColumn]]] map {prof =>
      {prof.headOption map (x=> x.payload collect(jcryptuserTOsuserPF))}.flatten
    }
  }
  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID) = {
    logger.info("UserDAOImpl Findby userid")
    val keyspace = userkeyspace
    val table = usertable
    val userid = userID.toString
    val conditions = s"userid='$userid'"
    val columns = "payload"
    val usercolumn = cassReadActor ? FindColumn(keyspace, table, columns, conditions) 
    usercolumn.asInstanceOf[Future[List[PayloadColumn]]] map {prof => 
      {prof.headOption map (x=> x.payload collect(jcryptuserTOsuserPF))}.flatten
    }
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User, update: Boolean = false) = {
    logger.info("UserDAOImpl Save:" + user.toString)
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

  def save(user: User) = {
    logger.info("UserDAOImpl Save:" + user.toString)
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
    // val dbUser = DBUser(user.userID.toString, user.firstName, user.lastName, user.fullName, user.email, user.avatarURL)
    // val dbLoginInfo = DBLoginInfo(None, user.loginInfo.providerID, user.loginInfo.providerKey)
    // // We don't have the LoginInfo id so we try to get it first.
    // // If there is no LoginInfo yet for this user we retrieve the id on insertion.    
    // val loginInfoAction = {
    //   val retrieveLoginInfo = slickLoginInfos.filter(
    //     info => info.providerID === user.loginInfo.providerID &&
    //     info.providerKey === user.loginInfo.providerKey).result.headOption
    //   val insertLoginInfo = slickLoginInfos.returning(slickLoginInfos.map(_.id)).
    //     into((info, id) => info.copy(id = Some(id))) += dbLoginInfo
    //   for {
    //     loginInfoOption <- retrieveLoginInfo
    //     loginInfo <- loginInfoOption.map(DBIO.successful(_)).getOrElse(insertLoginInfo)
    //   } yield loginInfo
    // }
    // // combine database actions to be run sequentially
    // val actions = (for {
    //   _ <- slickUsers.insertOrUpdate(dbUser)
    //   loginInfo <- loginInfoAction
    //   _ <- slickUserLoginInfos += DBUserLoginInfo(dbUser.userID, loginInfo.id.get)
    // } yield ()).transactionally
    // // run actions and return user afterwards
    // db.run(actions).map(_ => user)

}

