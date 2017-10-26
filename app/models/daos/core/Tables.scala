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
object Tables {
  private val cfg = ConfigFactory.load()
  val userkeyspace = cfg.getString("cassandra.userkeyspace")
  val shopkeyspace = cfg.getString("cassandra.shopkeyspace")
  val globalkeyspace = cfg.getString("cassandra.globalkeyspace")
  val privatekeyspace = cfg.getString("cassandra.privatekeyspace")
  val admintable = cfg.getString("cassandra.admintable")
  val usertable = cfg.getString("cassandra.usertable")
  val hinttable = cfg.getString("cassandra.hinttable")
  val shoptable = cfg.getString("cassandra.shoptable")
  val posttable = cfg.getString("cassandra.posttable")
  val feedtable = cfg.getString("cassandra.feedtable")
  val hiddenfeedtable = cfg.getString("cassandra.hiddenfeedtable")
  val jwttable = cfg.getString("cassandra.jwttable")
  val profiletable = cfg.getString("cassandra.profiletable")
  val paymenttable = cfg.getString("cassandra.paymenttable")
  val phototable = cfg.getString("cassandra.phototable")
  //val shoptemplatetable = cfg.getString("cassandra.shoptemplatetable")
  //val feedtemplatetable = cfg.getString("cassandra.feedtemplatetable")
  val shoppositiontable = cfg.getString("cassandra.shoppositiontable")
  val shopreviewtable = cfg.getString("cassandra.shopreviewtable")
  val feedreviewtable = cfg.getString("cassandra.feedreviewtable")
}
