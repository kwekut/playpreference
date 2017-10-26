package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import java.util.{Properties, UUID}
import scala.concurrent.duration._

//Used by client 
case class AdminKeys(
  hex: String,
  dwollascripturl: String,  //"https://cdn.dwolla.com/1/dwolla.js"
  dwollaiavtoken: String,  //'8zN400zyPUobbdmeNfhTGH2Jh5JkFREJa9YBI8SLXp0ERXNTMT'
  dwollacustomerinittoken: String,
  stripescripturl: String, //"https://checkout.stripe.com/checkout.js"
  stripepublishablekey: String, //'pk_test_6pRNASCoBOKtIshFeQd4XMUh'
  companylogourl: String, //'https://stripe.com/img/documentation/checkout/marketplace.png'
  companyname: String, //kaosk
  cloudinarycloudname: String, //"kaosk"
  cloudinaryfeedpreset: String, //'kaosk_preset'
  cloudinaryshoppreset: String, //'kaosk_preset'
  firebaseapiKey: String, // "AIzaSyBin3GLXzuhDXY337XojJNntYZKw9SJTm4",
  firebaseauthDomain: String, // "kaosk-cf6dd.firebaseapp.com",
  firebasedatabaseURL: String, // "https://kaosk-cf6dd.firebaseio.com",
  firebasestorageBucket: String, // "kaosk-cf6dd.appspot.com",
  firebasemessagingSenderId: String, // "102296110163"
  geocodekey: String //"http://maps.google.com/maps/api/js?sensor=false"
  ) 

object AdminKeys{
  implicit lazy val lnksFormat = Jsonx.formatCaseClass[AdminKeys]

  val c = ConfigFactory.load()
  c.checkValid(ConfigFactory.defaultReference(), "merchant.client")
    
  private val hex = c.getString("merchant.client.hex")
  private val dwollascripturl = c.getString("merchant.client.dwollascripturl") 
  private val dwollaiavtoken = c.getString("merchant.client.dwollaiavtoken") 
  private val dwollacustomerinittoken = c.getString("merchant.client.dwollacustomerinittoken") 
  private val stripescripturl = c.getString("merchant.client.stripescripturl") 
  private val stripepublishablekey = c.getString("merchant.client.stripepublishablekey") 
  private val companylogourl = c.getString("merchant.client.companylogourl") 
  private val companyname = c.getString("merchant.client.companyname") 
  private val cloudinarycloudname = c.getString("merchant.client.cloudinarycloudname") 
  private val cloudinaryfeedpreset = c.getString("merchant.client.cloudinaryfeedpreset") 
  private val cloudinaryshoppreset = c.getString("merchant.client.cloudinaryshoppreset") 
  private val firebaseapiKey = c.getString("merchant.client.firebaseapiKey") 
  private val firebaseauthDomain = c.getString("merchant.client.firebaseauthDomain") 
  private val firebasedatabaseURL = c.getString("merchant.client.firebasedatabaseURL") 
  private val firebasestorageBucket = c.getString("merchant.client.firebasestorageBucket") 
  private val firebasemessagingSenderId = c.getString("merchant.client.firebasemessagingSenderId")
  private val geocodekey = c.getString("merchant.client.geocodekey") 
  
  val adminkeys = AdminKeys(hex,dwollascripturl,dwollaiavtoken,
  dwollacustomerinittoken,stripescripturl,stripepublishablekey,
  companylogourl,companyname,cloudinarycloudname,cloudinaryfeedpreset,
  cloudinaryshoppreset,firebaseapiKey,firebaseauthDomain,firebasedatabaseURL,
  firebasestorageBucket,firebasemessagingSenderId,geocodekey)

}
