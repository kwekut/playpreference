package modules

import com.google.inject.{ AbstractModule, Provides }
import play.api.libs.concurrent.AkkaGuiceSupport
import net.codingwell.scalaguice.ScalaModule
import play.api.Play
import play.api.Play.current
import play.Logger
import actors.IndexErrorActor
import actors.IndexAbuseActor
import actors.UpdatePreferenceActor
import actors.IndexPreferenceActor
import actors.IndexShopActor
import actors.IndexFeedActor
import actors.ErrorReporterActor
import actors.CassandraReadActor
import actors.CassandraWriteActor
import models.services.{ UserService, UserServiceImpl }
import models.services.{CassandraService,CassandraServiceImpl}
import models.daos._

class ActorModule extends AbstractModule with AkkaGuiceSupport with ScalaModule {

  def configure() {
    Logger.debug("Binding actor implementations.")
    bind[UserService].to[UserServiceImpl]
    bind[UserDAO].to[UserDAOImpl]
    bind[CassandraService].to[CassandraServiceImpl]
    bindActor[UpdatePreferenceActor]("updatepreference-actor")
    bindActor[IndexPreferenceActor]("indexpreference-actor")
    bindActor[IndexErrorActor]("indexerror-actor")
    bindActor[IndexAbuseActor]("indexabuse-actor")
    bindActor[IndexShopActor]("indexshop-actor")
    bindActor[IndexFeedActor]("indexfeed-actor") 
    bindActor[ErrorReporterActor]("error-actor")
    bindActor[CassandraReadActor]("cassandraread-actor")
    bindActor[CassandraWriteActor]("cassandrawrite-actor")   
    //bind(classOf[ElasticClient]).asEagerSingleton()
  }

}
