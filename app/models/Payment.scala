package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx
//(paymentid#typ#Povider:brand#last4#expyr)
//(paymentid#typ:Povider=summary) = User/paymentsummary
//typ = bank/card
//Userid & Payment id compound cassandra
case class Payment(
  paymentid: String,
  userid: String,
  activity: String,
  rank: Int,
  typ: String,
  provider: String,
  summary: String,
  fee_rate: String,
  currency: String,
  publishablekey: Option[String],
  funding_url: Option[String],
  customer_url: Option[String],
  ondemandauthorization: Option[String],
  access_token: Option[String],
  account_id: Option[String],
  scope: Option[String],
  expires_in: Option[String],
  created: String
) 

object Payment{
  def buildPymt(u: Payment): Pymt = {
    Pymt( 
      u.paymentid,
      u.userid,
      u.rank,
      u.typ,
      "LISTPAYMENTS",
      u.provider,
      u.summary,
      u.fee_rate,
      u.currency,
      u.created)
  }

  def updateUser(u: Payment, r: Pymt): Payment = {
    Payment(
      u.activity,
      u.paymentid,
      u.userid,
      r.rank,
      r.typ,
      u.provider,
      r.summary,
      r.fee_rate,
      u.currency,
      u.publishablekey,
      u.funding_url,
      u.customer_url,
      u.ondemandauthorization,
      u.access_token,
      u.account_id,
      u.scope,
      u.expires_in,
      u.created
    )
  }
  implicit lazy val lnksFormat = Jsonx.formatCaseClass[Payment]
}


case class  Pymt(
  paymentid: String,
  userid: String,
  rank: Int,
  typ: String,
  activity: String,
  provider: String,
  summary: String,
  fee_rate: String,
  currency: String,
  created: String)

object Pymt{
  implicit lazy val usrFormat = Jsonx.formatCaseClass[Pymt]
}