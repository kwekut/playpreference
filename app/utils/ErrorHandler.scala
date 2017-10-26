package utils

import javax.inject.Inject

import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import controllers.routes
import play.api.http.DefaultHttpErrorHandler
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{ Result, RequestHeader }
import play.api.routing.Router
import play.api.{ OptionalSourceMapper, Configuration }

import scala.concurrent.Future

class ErrorHandler @Inject() (
  env: play.api.Environment,
  config: Configuration,
  sourceMapper: OptionalSourceMapper,
  router: javax.inject.Provider[Router])
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
  with SecuredErrorHandler {

  override def onNotAuthenticated(implicit request: RequestHeader) = {
    (Future.successful(Unauthorized("No access")))
  }

  override def onNotAuthorized(implicit request: RequestHeader) = {
    (Future.successful(Unauthorized("No access")))
  }
}