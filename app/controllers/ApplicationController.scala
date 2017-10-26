package controllers

import play.api._
import play.api.mvc._
import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import javax.inject._
import com.google.inject.name.Named
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import actors.IndexPreferenceActor._
import actors.UpdatePreferenceActor._
import models.mail._
import models._
import java.lang.Runtime

@Singleton
class ApplicationController @Inject() (
  @Named("indexpreference-actor") indexPrefActor: ActorRef,
  @Named("updatepreference-actor") updatePrefActor: ActorRef)
              (implicit exec: ExecutionContext) extends Controller {

  implicit lazy val timeout: Timeout = 10.seconds  

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def addPreferenceIndex = Action.async { implicit request =>
    val reply: Future[Option[String]] = ask(indexPrefActor, CreateIndexPreferenceDB).mapTo[Option[String]] recover {
      case e: Exception => Some(e.getMessage)
    }
    reply flatMap {
      case Some(x) => Future.successful(Ok(x))
      case None => Future.successful(Ok("Failure"))
    }
  }

// check all actors, weakest actors response time determines the response time
  def health = Action.async { implicit request =>
    val elastic: Future[String] = ask(updatePrefActor, HealthCheck).mapTo[String]
    val pref: Future[String] = ask(indexPrefActor, HealthCheck).mapTo[String] 
	  val reply: Future[Option[String]] = (
  		for {
  				sp <- pref
          el <- elastic
  			} yield Some(sp + el)
	  )
    reply flatMap {
      case Some(x) => Future.successful(Ok(x))
      case None => Future.successful(InternalServerError("Oops"))
    }
  }
}
