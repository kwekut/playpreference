package models.services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.mohiva.play.silhouette.api.AuthInfo
import models.{User, Profile}
import models.daos._
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time.LocalDateTime
import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (userDAO: UserDAO) extends UserService {

  /**
   * Retrieves a user that matches the specified login info.
   */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  def retrieve(email: String): Future[Option[User]] = userDAO.find(email)

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User, update: Boolean) = userDAO.save(user)


  //If User with this profile's(loginInfo) exists, add/replace profiles link to User and update user 
  //User account being changed must belong to current User
  //If no User with this profile's(loginInfo) exists, then save profile, then update current User to link to it
  def create[A <: AuthInfo](profile: CommonSocialProfile, newuser: User): Future[User] = {
    userDAO.find(profile.loginInfo).flatMap {
      case Some(existingUser) =>
           val u = existingUser.copy(
            profiles = existingUser.profiles.filterNot(_.providerID == profile.loginInfo.providerID) :+ profile.loginInfo
          )
        save(u, update = true)

      case None =>     
        userDAO.find(profile.email.getOrElse("none")).flatMap {
          case Some(existingUser) => 
              val u = existingUser.copy(
                profiles = existingUser.profiles.filterNot(_.providerID == profile.loginInfo.providerID) :+ profile.loginInfo
              )
              save(u, update = true)
          case None =>  
              val u = newuser.copy(
                profiles = Seq(profile.loginInfo),
                created = new LocalDateTime().toString
              )
              save(u, update = false)

        }
    }
  }


}
