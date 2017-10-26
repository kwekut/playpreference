package models

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.inject.{Inject, Singleton}
import org.abstractj.kalium.crypto.Random
import org.abstractj.kalium.crypto._
import play.api.Configuration
import play.api.libs.Crypto
import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import play.api.libs.concurrent.Execution.Implicits._
import java.util.ArrayList
import java.util.Arrays
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx
import com.typesafe.config.ConfigFactory
import scala.util.Try
import scala.util.{Success, Failure}
import java.util.UUID
import scala.concurrent.Future
import models.mail._


object Encryptor {
  private val cong = ConfigFactory.load()
  private val sHex: String = cong.getString("crypto.secret")
  private val uHex: String = cong.getString("crypto.usersecret")
  private val pHex: String = cong.getString("crypto.profilesecret")
  private val jHex: String = cong.getString("crypto.jwtsecret")
  private val qHex: String = cong.getString("crypto.qrsecret")
  private val pyHex: String = cong.getString("crypto.paymentsecret")

  class Nonce(val raw: Array[Byte]) extends AnyVal
  private val random = new Random()

  def createNonce(): Nonce = {
    import org.abstractj.kalium.NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES
    new Nonce(random.randomBytes(CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES))
  }

  def nonceFromBytes(data: Array[Byte]): Nonce = {
    import org.abstractj.kalium.NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES
    if (data == null || data.length != CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES) {
      throw new IllegalArgumentException("This nonce has an invalid size: " + data.length)
    }
    new Nonce(data)
  }
}



case class Encryptor(stringData: String) {
  import Encryptor._
  private val random = new SecureRandom()
  private def encoder = org.abstractj.kalium.encoders.Encoder.HEX
  private val logger = Logger(this.getClass)

  // utility method for when we're showing off secret key without saving confidential info...
  def newSecretKey(): String = {
    // Key must be 32 bytes for secretbox
    import org.abstractj.kalium.NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_KEYBYTES
    val buf = new Array[Byte](CRYPTO_SECRETBOX_XSALSA20POLY1305_KEYBYTES)
    random.nextBytes(buf)
    encoder.encode(buf)
  }

  private def box(secretKey: Array[Byte]) = {
    new org.abstractj.kalium.crypto.SecretBox(secretKey)
  }
//Takes a json string
  private def encrypt(stringData: String, nonce: Nonce, box: SecretBox): String = {
    Logger.info("encrypt")
    Logger.info("encrypt:"+ stringData)
    val nonceHex = encoder.encode(nonce.raw)
    val rawData = stringData.getBytes(StandardCharsets.UTF_8)
    val cipherText = box.encrypt(nonce.raw, rawData)
    val cipherHex = encoder.encode(cipherText)
    Json.toJson(Map("nonce" -> nonceHex, "c" -> cipherHex)).toString
  }

//Returns json string
  private def decrypt(stringData: String, box: SecretBox): Option[String] = {
    Logger.info("decrypt")
    Logger.info("decrypt:"+ stringData)
    Json.parse(stringData).asOpt[Map[String,String]] map { data=>
    val nonceHex = data("nonce")
    val cipherTextHex = data("c")
    val nonce = nonceFromBytes(encoder.decode(nonceHex))
    val cipherText = encoder.decode(cipherTextHex)
    val rawData = box.decrypt(nonce.raw, cipherText)
    new String(rawData, StandardCharsets.UTF_8)
    }
  }


//Takes a json string
  def userEncrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // val nonce = createNonce()
    // secretBox map {sox=>  encrypt(stringData, nonce, sox)  } 
    Some(stringData)
  }
  def profileEncrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // val nonce = createNonce()
    // secretBox map { sox =>  encrypt(stringData, nonce, sox)  } 
    Some(stringData)
  }
  def jwtEncrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // val nonce = createNonce()
    // secretBox map { sox =>  encrypt(stringData, nonce, sox)  } 
    Some(stringData)
  }
  def secretEncrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // val nonce = createNonce()
    // secretBox map { sox =>  encrypt(stringData, nonce, sox)  } 
    Logger.info("secretEncrypt")
    Logger.info("secretEncrypt: " + stringData)
    Some(stringData)
  }
  def qrEncrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // val nonce = createNonce()
    // secretBox map { sox =>  encrypt(stringData, nonce, sox)  } 
    Some(stringData)
  }
  def paymentEncrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // val nonce = createNonce()
    // secretBox map { sox =>  encrypt(stringData, nonce, sox)  } 
    Some(stringData)
  }


  def userDecrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // secretBox map { sox =>  decrypt(stringData, sox).get }
    Some(stringData)
  }
  def paymentDecrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // secretBox map { sox =>  decrypt(stringData, sox).get  }
    Some(stringData)
  }
  def qrDecrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // secretBox map { sox =>  decrypt(stringData, sox).get  }
    Some(stringData)
  }
  def secretDecrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // secretBox map { sox =>  decrypt(stringData, sox).get  }
    Some(stringData)
  }
  def jwtDecrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // secretBox map { sox =>  decrypt(stringData, sox).get  }
    Some(stringData)
  }
  def profileDecrypt = {
    // val secretBox = Try{ new org.abstractj.kalium.crypto.SecretBox(encoder.decode(uHex)) }.toOption
    // secretBox map { sox =>  decrypt(stringData, sox).get  }
    Some(stringData)
  }
  
  // def userEncrypt = Encrypt
  // def profileEncrypt = Encrypt
  // def jwtEncrypt = Encrypt
  // def secretEncrypt = Encrypt
  // def qrEncrypt = Encrypt
  // def paymentEncrypt = Encrypt

  // def Encrypt = {
  //   val nonce = createNonce()
  //   val nonceHex = encoder.encode(nonce.raw)
  //   val newKey = newSecretKey()
  //   val complexResponse = ws.url(vaultUrl)
  //     .withRequestTimeout(10000.millis)
  //     .withQueryString("nonceHex" -> nonceHex, "key" -> newKey)
  //     .get()
  //   val res = complexResponse map { response => 
  //       (response.json \ "key").asOpt[String]
  //   }
  //   val encoder = org.abstractj.kalium.encoders.Encoder.HEX
  //   val secretBox = Try{new org.abstractj.kalium.crypto.SecretBox(encoder.decode(res.get))}.toOption
  //   secretBox map { sox => encrypt(stringData, nonce, sox) }
  // }


  // def userDecrypt = Decrypt
  // def paymentDecrypt = Decrypt
  // def qrDecrypt = Decrypt
  // def secretDecrypt = Decrypt
  // def jwtDecrypt = Decrypt
  // def profileDecrypt = Decrypt

  // def Decrypt = {
  //   val nonceHex = Json.parse(stringData).asOpt[Map[String,String]] map { data=>
  //    data("nonceHex")
  //   }
  //   val complexResponse = ws.url(vaultUrl)
  //     .withRequestTimeout(10000.millis)
  //     .withQueryString("nonceHex" -> nonceHex.get)
  //     .get()
  //   val res = complexResponse map { response => 
  //      (response.json \ "key").asOpt[String]
  //   }
  //    val encoder = org.abstractj.kalium.encoders.Encoder.HEX
  //    val secretBox = Try{new org.abstractj.kalium.crypto.SecretBox(encoder.decode(res.get))}.toOption
  //    secretBox map { sox => decrypt(stringData, sox) }
  // }


}