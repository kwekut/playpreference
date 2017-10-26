package models.services

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import models._
import scala.concurrent.Future

/**
 * Give access to the user object.
 */
trait CassandraService {

  def findUsers: Future[List[User]]

  def findFeeds: Future[List[Feed]]

  def findShops: Future[List[Shop]]

  def saveUser(user: User): Future[User]
}
