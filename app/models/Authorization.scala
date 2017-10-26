package models

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.api.Authorization
import play.api.i18n._
import play.api.mvc.Request

import scala.concurrent.Future
// Not used yet
// case class WithRole(role: String) extends Authorization[User, JWTAuthenticator] {
//   override def isAuthorized[B](user: User, authenticator: JWTAuthenticator)(
//     implicit request: Request[B], messages: Messages) = {
    
//     Future.successful(user.roles match {
//       case list: Set[String] => list.contains(role)
//       case _ => false
//     })
//   }
// }

// case class WithWorkership(shopid: String) extends Authorization[User, JWTAuthenticator] {
//   override def isAuthorized[B](user: User, authenticator: JWTAuthenticator)(
//     implicit request: Request[B], messages: Messages) = {
    
//     Future.successful(user.shopoperator match {
//       case Some(shopidd) => shopid == shopidd
//       case None => false
//     })
//   }
// }

// case class WithOwnership(shopid: String, role: String) extends Authorization[User, JWTAuthenticator] {
//   override def isAuthorized[B](user: User, authenticator: JWTAuthenticator)(
//     implicit request: Request[B], messages: Messages) = {
    
//     Future.successful(
//       user.shopoperator match {
//         case Some(shopid) => 
//             user.roles match {
//               case rlist: Set[String] => rlist.contains(role)
//               case _ => false
//             }
//         case None => false
//       }
//     )
//   }
// }

// case class WithOwnership(shopid: String, role: String) extends Authorization[User, JWTAuthenticator] {
//   override def isAuthorized[B](user: User, authenticator: JWTAuthenticator)(
//     implicit request: Request[B], messages: Messages) = {
    
//     Future.successful(
//       user.shopoperator match {
//         case olist: Set[String] => 
//             user.roles match {
//               case rlist: Set[String] => olist.contains(shopid) && rlist.contains(role)
//               case _ => false
//             }
//         case _ => false
//       }
//     )
//   }

  // override def onNotAuthorized(request: RequestHeader): Option[Future[Result]] = {
  //   Some(Future.successful(Forbidden("Not authorized")))
  // }

  // def myAction = SecuredAction(!WithRole("admin")) { implicit request =>
  // // Ok(views.html.index(request.identity))
  // }