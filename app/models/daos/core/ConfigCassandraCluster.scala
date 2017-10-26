package models.daos.core

import com.datastax.driver.core.{ProtocolOptions, Cluster}
//import com.datastax.driver.CassandraOnMesos
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import akka.actor.ActorSystem
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import play.api.Logger

//object CassandraCluster extends ConfigCassandraCluster{}

object CassandraCluster {

  private val cfg = ConfigFactory.load()
  private val port = cfg.getInt("cassandra.port")
  private val hosts = cfg.getStringList("cassandra.hosts")
  private val cpoints = cfg.getInt("cassandra.numberOfContactPoints")
  private val httpServerBaseUri = cfg.getString("cassandra.httpServerBaseUri")

  lazy val cluster: Cluster =
    Cluster.builder().
      addContactPoints(hosts: _*).
      withCompression(ProtocolOptions.Compression.SNAPPY).
      withPort(port).
      build()
   
  // lazy val cluster: Cluster =
  //   CassandraOnMesos.forClusterBuilder(Cluster.builder())
  //       .withApiEndpoint(httpServerBaseUri)
  //       .withNumberOfContactPoints(cpoints)
  //       .build();
}


   

