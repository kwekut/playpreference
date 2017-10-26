package models.services

import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import play.api.mvc._
import models._
import scala.concurrent.Future


trait SilhouetteService  {
  
  def signUp(email: String, password: String, username: String, firstname: String, lastname: String, 
      homeaddress: String, officeaddress: String, roamingaddress: String, phone: String, timezone: String,
      securityquestion1: String, securityanswer1: String, securityquestion2: String, securityanswer2: String,
      securityquestion3: String, securityanswer3: String): Future[RequestReturn]

  def changePassword(email: String, password: String, 
    securityquestion1: String, securityanswer1: String,
    securityquestion2: String, securityanswer2: String, 
    securityquestion3: String, securityanswer3: String): Future[RequestReturn]

  def getQuestion(email: String): Future[RequestReturn]

  def authenticate(email: String, password: String, 
    expiredtoken: String, rememberMe: Boolean): Future[RequestReturn]

  def authorize(token: String): Future[RequestReturn]

  def signOut(token: String): Future[RequestReturn]

}
