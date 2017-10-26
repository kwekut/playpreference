package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
//import com.mohiva.play.silhouette.impl.daos.AuthenticatorDAO
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator._
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.api.StorableAuthenticator
import com.mohiva.play.silhouette.impl.exceptions._
import com.mohiva.play.silhouette.api.exceptions._
import com.mohiva.play.silhouette.api.repositories._
import com.mohiva.play.silhouette.api.crypto.AuthenticatorEncoder
import com.mohiva.play.silhouette.api.services.AuthenticatorService._
import com.mohiva.play.silhouette.impl.providers._
import scala.util.control.Exception
import models.User
import org.joda.time.LocalDateTime
import java.util.UUID
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import play.api.Configuration
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc._
import scala.util.Try
import scala.concurrent.Future
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }
import models._

trait NewAuthenticatorService[T <: Authenticator] extends ExecutionContextProvider {
  def create(loginInfo: LoginInfo): Future[T]
  def retrieve(token: String): Future[Option[T]]
  def init(authenticator: T): Future[String]
  def touch(authenticator: T): Either[T, T]
  def update(authenticator: T): Future[String]
  def renew(authenticator: T): Future[String]
  def discard(authenticator: T): Future[String]
}

// object NewAuthenticatorService {
//   val CreateError = "[Silhouette][%s] Could not create authenticator for login info: %s"
//   val DiscardError = "[Silhouette][%s] Could not discard authenticator: %s"
// }

class NewJWTAuthenticatorService(
  settings: JWTAuthenticatorSettings,
  repository: Option[AuthenticatorRepository[JWTAuthenticator]],
  //repository: Option[AuthenticatorDAO[JWTAuthenticator]],
  authenticatorEncoder: AuthenticatorEncoder,
  idGenerator: IDGenerator,
  clock: Clock)(implicit val executionContext: ExecutionContext)
  extends NewAuthenticatorService[JWTAuthenticator]
  with Logger {

  val ID = "jwt-authenticator"

  def create(loginInfo: LoginInfo): Future[JWTAuthenticator] = {
    idGenerator.generate.map { id =>
      val now = clock.now
      JWTAuthenticator(
        id = id,
        loginInfo = loginInfo,
        lastUsedDateTime = now,
        expirationDateTime = now + settings.authenticatorExpiry,
        idleTimeout = settings.authenticatorIdleTimeout
      )
    }.recover {
      case e => throw new AuthenticatorCreationException(CreateError.format(ID, loginInfo), e)
      //case e =>  throw new Exception("Create Error")
    }
  }

  def retrieve(token: String): Future[Option[JWTAuthenticator]] = {
    unserialize(token, authenticatorEncoder, settings) match {
      case Success(authenticator) => repository.fold(Future.successful(Option(authenticator)))(_.find(authenticator.id))
      case Failure(e) =>  Future.successful(None)
    }
  }

  def init(authenticator: JWTAuthenticator): Future[String] = {
    repository.fold(Future.successful(authenticator))(_.add(authenticator)).map { a =>
      serialize(a, authenticatorEncoder, settings)
    }.recover {
      case e => throw new AuthenticatorInitializationException(InitError.format(ID, authenticator), e)
      //case e => "Init Error"
    }
  }

  def embed(token: String): Future[String] = {
    Future(s"""$settings.fieldName -> $token""")
  }

  def touch(authenticator: JWTAuthenticator): Either[JWTAuthenticator, JWTAuthenticator] = {
    if (authenticator.idleTimeout.isDefined) {
      Left(authenticator.copy(lastUsedDateTime = clock.now))
    } else {
      Right(authenticator)
    }
  }

  def update(authenticator: JWTAuthenticator): Future[String] = {
    repository.fold(Future.successful(authenticator))(_.update(authenticator)).map { a =>
      serialize(a, authenticatorEncoder, settings)
    }.recover {
      case e => throw new AuthenticatorUpdateException(UpdateError.format(ID, authenticator), e)
      //case e => "Update Error"
    }
  }

  def renew(authenticator: JWTAuthenticator): Future[String] = {
    repository.fold(Future.successful(()))(_.remove(authenticator.id)).flatMap { _ =>
      create(authenticator.loginInfo).map(_.copy(customClaims = authenticator.customClaims)).flatMap(init)
    }.recover {
      case e => throw new AuthenticatorRenewalException(RenewError.format(ID, authenticator), e)
    }
  }

  def discard(authenticator: JWTAuthenticator): Future[String] = {
    repository.fold(Future.successful(()))(_.remove(authenticator.id)).map { _ =>
      ("Discard Success")
    }.recover {
      case e => throw new AuthenticatorDiscardingException(DiscardError.format(ID, authenticator), e)
      //case e => "Discard Error"
    }
  }
}

// object NewJWTAuthenticatorService {
//   val ID = "jwt-authenticator"
//   val InvalidJWTToken = "[Silhouette][%s] Error on parsing JWT token: %s"
//   val JsonParseError = "[Silhouette][%s] Cannot parse Json: %s"
//   val UnexpectedJsonValue = "[Silhouette][%s] Unexpected Json value: %s"
//   val OverrideReservedClaim = "[Silhouette][%s] Try to overriding a reserved claim `%s`; list of reserved claims: %s"
//   val ReservedClaims = Seq("jti", "iss", "sub", "iat", "exp")
// }


// case class JWTAuthenticatorSettings(
//   fieldName: String = "X-Auth-Token",
//   requestParts: Option[Seq[RequestPart.Value]] = Some(Seq(RequestPart.Headers)),
//   issuerClaim: String = "play-silhouette",
//   authenticatorIdleTimeout: Option[FiniteDuration] = None,
//   authenticatorExpiry: FiniteDuration = 12 hours,
//   sharedSecret: String
// )