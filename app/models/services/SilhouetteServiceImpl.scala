package models.services

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator._
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.api.StorableAuthenticator
import com.mohiva.play.silhouette.impl.exceptions._
import com.mohiva.play.silhouette.api.exceptions._
import com.mohiva.play.silhouette.api.repositories._
import com.mohiva.play.silhouette.impl.providers._
import scala.util.control.Exception
import models.User
import org.joda.time.LocalTime
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.LocalDateTime
import java.util.UUID
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import play.api.Configuration
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc._
import play.api.Logger
import utils.{ DefaultEnv }
import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.duration._
import models.daos._
import models.mail._
import models._

//The credentials auth service for socket Login/Register.

class SilhouetteServiceImpl @Inject() (
  val messagesApi: MessagesApi,
  val env: Silhouette[DefaultEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  authenticatorService: NewAuthenticatorService[JWTAuthenticator],
  credentialsProvider: CredentialsProvider,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  idGenerator: IDGenerator,
  configuration: Configuration,
  clock: Clock) extends SilhouetteService {

  val logger = Logger(this.getClass())
  val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")

  def signUp(email: String, password: String, username: String, firstname: String, lastname: String,
      homeaddress: String, officeaddress: String, roamingaddress: String, phone: String, timezone: String,
      securityquestion1: String, securityanswer1: String, securityquestion2: String, securityanswer2: String,
      securityquestion3: String, securityanswer3: String): Future[RequestReturn] = {
      logger.info("SilhouetteService signup")
      Logger.info("SilhouetteService signup")
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
      val loginInfo = LoginInfo("credentials", email)
      val deviceid = UUID.randomUUID().toString
      userService.retrieve(loginInfo).flatMap {
        case Some(user) =>
          logger.info("SilhouetteService signup user exists")
          Logger.info("SilhouetteService signup user exists")
          Future.successful(RequestReturn("error", "user exists", "devicetoken", "token", "username", "email"))
        case None =>
          logger.info("SilhouetteService signup new user")
          Logger.info("SilhouetteService signup new user")
          val authInfo = passwordHasher.hash(password)
          val profile = CommonSocialProfile(
              loginInfo = loginInfo,
              firstName = Some(firstname),
              lastName = Some(lastname),
              fullName = Some(firstname + " " + lastname),
              email = Some(email)
          )

          val quesnans = Some(securityquestion1.toLowerCase+"="+securityanswer1.toLowerCase+" : "+securityquestion2.toLowerCase+"="+securityanswer2.toLowerCase+" : "+securityquestion3.toLowerCase+"="+securityanswer3.toLowerCase)
          val user = User.newUser.copy(
              userid = UUID.randomUUID.toString,
              profiles = Seq(loginInfo),
              firstname = Some(firstname),
              lastname = Some(lastname),
              fullname = Some(firstname + " " + lastname),
              username = Some(username),
              email = Some(email),
              phone = Some(phone),
              homeaddress = Some(homeaddress),
              officeaddress = Some(officeaddress),
              roamingaddress = Some(roamingaddress),
              paymentsummary = List(),
              alerttoken = Set(),
              feedpreferences = List(),
              shoppreferences = List(),
              feedlikes = List(),
              shoplikes = List(),
              feedhistory = List(),
              shophistory = List(),
              roles = Set("user"),
              shopoperator = None,
              followings = Set(),
              avatarURL = None,
              expiredtoken = None,
              securitythreat = List(),
              securityquestions = quesnans,
              dailypurchaselimit = Some("500"),
              pertransactionlimit = Some("100"),
              dailycancellimit = Some("5"),
              dailycancelaccumcount = None,
              dailyorderaccumamount = None,
              timezone = timezone,
              created = new LocalDateTime().toString
          )
          //Save current token forr 2fa - it becomes the expiredtoken for logins
          //NB- only usr object goes to client, not user object
          for {
            avatar <- avatarService.retrieveURL(email)
            authInfo <- authInfoRepository.add(loginInfo, authInfo)
            authenticator <- authenticatorService.create(loginInfo)
            token <- authenticatorService.init(authenticator)
            user <- userService.create(profile.copy(avatarURL = avatar), user.copy(avatarURL=avatar, expiredtoken=Some(deviceid)))
          } yield {
            logger.info("SilhouetteService signup save new user")
            Logger.info("SilhouetteService signup save new user")
            Try{SendToEmail(
                  "Kaosk - Successful signup",
                  s"You have successfully signed up to the KAOSK app at $date",
                user.email.get).send}.toOption
            (RequestReturn("sucess", "successful authenticate", deviceid, token, user.username.getOrElse("Username not yet set"), user.email.getOrElse("Email no yet set")))
          }
      }
  }

  // Only block user's who are high security risk
  def changePassword(email: String, password: String,
    securityquestion1: String, securityanswer1: String,
    securityquestion2: String, securityanswer2: String,
    securityquestion3: String, securityanswer3: String): Future[RequestReturn] = {
      logger.info("SilhouetteService changePassword")
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
      val loginInfo = LoginInfo("credentials", email)
      val deviceid = UUID.randomUUID().toString
      userService.retrieve(loginInfo).flatMap {
        case None =>
          Future.successful(RequestReturn("error", "user doesnt exist", "devicetoken", "no-token", "no-username", "no-email"))
        case Some(user) =>
        if (user.securitythreat.length > 5) {
          Future (RequestReturn("error", "this account has been blocked", "devicetoken", "no-token", user.username.getOrElse("no-username"), user.email.getOrElse("no-email")))
        }else{
          //To-do Flesh up the if logic
          val quesnans = Some(securityquestion1.toLowerCase+"="+securityanswer1.toLowerCase+" : "+securityquestion2.toLowerCase+"="+securityanswer2.toLowerCase+" : "+securityquestion3.toLowerCase+"="+securityanswer3.toLowerCase)
          Logger.info(quesnans.get.filterNot((x: Char) => x.isWhitespace))
          Logger.info(user.securityquestions.get.filterNot((x: Char) => x.isWhitespace))
          if(user.securityquestions.get.filterNot((x: Char) => x.isWhitespace) == quesnans.get.filterNot((x: Char) => x.isWhitespace)){
            val authInfo = passwordHasher.hash(password)
            // val profile = CommonSocialProfile(
            //     loginInfo = loginInfo,
            //     firstName = user.firstname,
            //     lastName = user.lastname,
            //     fullName = user.fullname,
            //     email = user.email
            // )
            for {
              //avatar <- avatarService.retrieveURL(email)
              //user <- userService.create(profile.copy(avatarURL = avatar), user.copy(avatarURL = avatar))
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authenticator <- authenticatorService.create(loginInfo)
              token <- authenticatorService.init(authenticator)
              user <- userService.save(user.copy(expiredtoken=Some(deviceid)),true)
            } yield {
                Try{SendToEmail(
                      "Kaosk - Successfully changed password",
                      s"You successfully changed your password for the KAOSK app at $date",
                user.email.get).send}.toOption
                (RequestReturn("sucess", "successful authenticate", deviceid, token, user.username.getOrElse("no-username"), user.email.getOrElse("no-email")))
            }
          } else {
            Future.successful(RequestReturn("error", "security answers did not match", "devicetoken", "token", "username", "email"))
          }
        }
      }
  }
  // Only block user's who are high security risk
  def getQuestion(email: String): Future[RequestReturn] = {
    logger.info("SilhouetteService getQuestion")
    val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
    val loginInfo = LoginInfo("credentials", email)
      userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            if (user.securitythreat.length > 5) {
                Try{SendToEmail(
                  "Kaosk - Your account has been blocked",
                  "You cannot take any action on an account that has been blocked",
                user.email.get).send}.toOption
              Future (RequestReturn("error", "your account has been blocked", "devicetoken", "device", user.username.getOrElse("no username"), user.email.getOrElse("no email")))
            }else{
                Try{SendToEmail(
                  "Kaosk - Successfully requested a password change",
                  s"You requested a change to your password for the KAOSK app at $date",
                user.email.get).send}.toOption
              Future (RequestReturn("sucess", "successful authenticate", "devicetoken", user.securityquestions.getOrElse("no question"), user.username.getOrElse("no username"), user.email.getOrElse("no email")))
            }
          case None =>
            Future{RequestReturn("error", "couldnt find user", "devicetoken", "token", "username", "email")}
        }
  }

  def authenticate(email: String, password: String, expiredtoken: String, rememberMe: Boolean)
                                                    : Future[RequestReturn] = {
  logger.info("SilhouetteService authenticate")
      val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
      val random = UUID.randomUUID().toString
      credentialsProvider.authenticate(Credentials(email, password)).flatMap { loginInfo =>
        userService.retrieve(loginInfo).flatMap {
          case Some(user) => authenticatorService.create(loginInfo).map {
            case authenticator if rememberMe =>
            Logger.info("tok1: "+expiredtoken)
            Logger.info("tok2: "+user.expiredtoken.getOrElse("none"))
              val c = configuration.underlying
              authenticator.copy(
                expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout")
              )
            case authenticator => authenticator
          }.flatMap { authenticator =>
            authenticatorService.init(authenticator).map { token =>
              if (user.securitythreat.length > 5) {
                Try{SendToEmail(
                    "Kaosk - Your account has been blocked",
                    "Your account has been blocked due to suspicious activities on your part.",
                user.email.get).send}.toOption
                (RequestReturn("error", "your account has been blocked", "devicetoken", "no token", user.username.getOrElse("Username not yet set"), user.email.getOrElse("No email")))
              //2FA, login details + expired token on device
              }else if (user.expiredtoken.getOrElse(random) != expiredtoken) {
                Try{SendToEmail(
                    "Kaosk - Your authentication is expired",
                    "Your authentication is expired, please log out and login again to continue.",
                user.email.get).send}.toOption
                (RequestReturn("error", "your authentication has expired - click renew authentication", "devicetoken", "no token", user.username.getOrElse("Username not yet set"), user.email.getOrElse("No email")))
              }else{
                Try{SendToEmail(
                    "Kaosk - Successful signin",
                    s"Welcome back to Kaosk. You succesfully signed at $date",
                user.email.get).send}.toOption
                //token(is expiredtoken) for 2fa auth.
                (RequestReturn("sucess", "successful authenticate", expiredtoken, token, user.username.getOrElse("no-username"), user.email.getOrElse("no-email")))
              }
            }
          }
          case None => Future{RequestReturn("error", "couldnt find user", "devicetoken", "token", "username", "email")}
        }
      }
      // .recover {
      //   case e: ProviderException => RequestReturn("error", "invalid.credentials", "devicetoken", "token", "username", "email")
      // }
  }

  //Authorise and update token/save expired to user/send new to client
  // set in conf - authenticator.authenticatorIdleTimeout = None, prevents renew duplication
  def authorize(token: String): Future[RequestReturn] = {
    logger.info("SilhouetteService authorize")
    authenticatorService.retrieve(token) flatMap {

		case Some(authenticator) =>
      userService.retrieve(authenticator.loginInfo) flatMap {
        case Some(user) =>
          authenticatorService.renew(authenticator) flatMap { token =>
            userService.save(user.copy(expiredtoken=Some(token)),true) map {x=>
              (RequestReturn("sucess", "successful authenticate", "devicetoken", token, x.username.getOrElse("Username not yet set"), x.email.getOrElse("No email")))
            }
          }
        case None => Future{RequestReturn("error", "couldnt find user", "devicetoken", "token", "username", "email")}
      }

    //RequestReturn("sucess", "Device is authorized", "devicetoken", token, "username", "email")

		case None => Future{RequestReturn("error", "bad token", "devicetoken", "token", "username", "email")}
    }
  }

  //redundant
  def retrieve(token: String): Future[Option[JWTAuthenticator]] = {
    logger.info("SilhouetteService retrieve")
    authenticatorService.retrieve(token)
  }

  def signOut(token: String): Future[RequestReturn] = {
    logger.info("SilhouetteService signOut")
    authenticatorService.retrieve(token) flatMap {
		case Some(authenticator) =>
		    authenticatorService.discard(authenticator) map {
		    	case "Discard Sucess" => RequestReturn("sucess", "Signed Out", "devicetoken", "token", "username", "email")
		    	case "Discard Error" => RequestReturn("error", "Could not sign out", "devicetoken", "token", "username", "email")
		    	case _ =>	RequestReturn("error", "Could not log out", "devicetoken", "token", "username", "email")
		    }
		case None => Future{RequestReturn("error", "bad token", "devicetoken", "token", "username", "email")}
    }


  }

}
