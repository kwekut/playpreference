package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx


// Common Json structure to client
// Must match client model structures
case class Feed(
    feedid: String,
    shopid: String,
    category: Set[String],
    typ: String,
    activity: String,
    shopname: String,
    title: Option[String],
    summary: Option[String],
    detail: Option[String],
    options: Set[String],
    currency: String,
    price: String,
    discount: String,
    deliverycost: String,
    expiry: String,
    picurl: Set[String],
    likes: Set[String],
    ratings: Set[String],
    lat: String,
    lng: String,
    region: String,
    template: String = "default",
    created: String = LocalDateTime.now.toString)

object Feed{
    //used by adminapp
    def buildMsg(m: Feed): Msg = {
      Msg(
            m.feedid, //id
            m.feedid,
            m.shopid,
            "userid", //userid
            "chargeid", //chargeid
            m.typ,
            m.activity,
            m.shopname,
            "username", //m.username
            m.category.mkString(","),
            m.title.getOrElse("Title goes here"),
            m.summary.getOrElse("Summary goes here"),
            m.detail.getOrElse("Detail goes here"),
            m.options.mkString(","),
            m.currency,
            m.price,
            m.discount,
            m.deliverycost,
            "deliverystatus", //deliverystatus
            "deliveryoption", //deliveryoption
            "deliveryaddress", //deliveryaddress
            m.expiry,
            m.picurl.mkString(","),
            "fdback", //fdback
            "read", //read
            "likecount", //likecount
            false, //isliked
            "starcount", //starcount
        	  "staraverage",
            false, //israted
            false, //isfollowing
            m.lat,
            m.lat,
            m.region,
            "parentids", //parentids
            "parentid", //parentid
            "parenttitle", //parenttitle
            "parentdetail", //parentdetail
            "parentoption", //parentoption
            "parentexpiry", //parentexpiry
            "false", //highlight
            m.template,
            m.created)
    }
    //used by homeapp
    def buildClientMsg(m: Feed, u: User): Msg = {
      Msg(
            m.feedid, //id
            m.feedid,
            m.shopid,
            u.userid, //userid
            "chargeid", //chargeid
            m.typ,
            m.activity,
            m.shopname,
            u.username.getOrElse("No username"), //m.username
            m.category.mkString(","),
            m.title.getOrElse("Title goes here"),
            m.summary.getOrElse("Summary goes here"),
            m.detail.getOrElse("Detail goes here"),
            m.options.mkString(","),
            m.currency,
            m.price,
            m.discount,
            m.deliverycost,
            "deliverystatus", //deliverystatus
            "deliveryoption", //deliveryoption
            "deliveryaddress", //deliveryaddress
            m.expiry,
            m.picurl.mkString(","),
            "fdback", //fdback
            "read", //read
            m.likes.size.toString, //likecount
            u.feedlikes.contains(m.feedid), //isliked
            m.ratings.size.toString, //starcount
        	  {((m.ratings.map{x=> x.split(":").last.toInt}).reduceLeft((x,y) => x + y))/(m.ratings.size)}.toString,
            u.ratings.contains(m.feedid), //israted
            u.followings.contains(m.shopid), //isfollowing
            m.lat,
            m.lat,
            m.region,
            "parentids", //parentids
            "parentid", //parentid
            "parenttitle", //parenttitle
            "parentdetail", //parentdetail
            "parentoption", //parentoption
            "parentexpiry", //parentexpiry
            "false", //highlight
            m.template,
            m.created)
    }

    def copyFeed(old: Feed, nw: Msg): Feed = {
      Feed(
            old.feedid,
            nw.shopid,
            old.category.toSet ++ nw.category.split(",").toSet,
            nw.typ,
            nw.activity,
            nw.shopname,
            Some(nw.title),
            Some(nw.summary),
            Some(nw.detail),
            old.options.toSet ++ nw.options.split(",").toSet,
            nw.currency,
            nw.price,
            nw.discount,
            nw.deliverycost,
            nw.expiry,
            old.picurl.toSet ++ nw.picurl.split(",").toSet,
            old.likes.toSet,
            old.ratings.toSet,
            nw.lat,
            nw.lng,
            nw.region,
            nw.template,
            nw.created
        )
    }

    def createFeed(nw: Msg): Feed = {
      Feed(
            nw.id,
            nw.shopid,
            nw.category.split(",").toSet,
            nw.typ,
            nw.activity,
            nw.shopname,
            Some(nw.title),
            Some(nw.summary),
            Some(nw.detail),
            nw.options.split(",").toSet,
            nw.currency,
            nw.price,
            nw.discount,
            nw.deliverycost,
            nw.expiry,
            nw.picurl.split(",").toSet,
            Set(),
            Set(),
            nw.lat,
            nw.lng,
            nw.region,
            nw.template,
            nw.created
        )
    }

  implicit lazy val feedFormat = Jsonx.formatCaseClass[Feed]
  //implicit val feedReads = Json.reads[Feed]
  //implicit val feedWrites = Json.writes[Feed]
}
//Mail, Event, Apply, Order, Book, Draft, Promo, Menu, Reserve, Post
case class Msg(
    id: String,
    feedid: String,
    shopid: String,
    userid: String,
    chargeid: String,
    typ: String,
    activity: String,
    shopname: String,
    username: String,
    category: String,
    title: String,
    summary: String,
    detail: String,
    options: String,
    currency: String,
    price: String,
    discount: String,
    deliverycost: String,
    deliverystatus: String,
    deliveryoption: String,
    deliveryaddress: String,
    expiry: String,
    picurl: String,
    fdback: String,
    read: String,
    likecount: String,
    isliked: Boolean,
    starcount: String,
    staraverage: String,
    israted: Boolean,
    isfollowing: Boolean,
    lat: String,
    lng: String,
    region: String,
    parentids: String,
    parentid: String,
    parenttitle: String,
    parentdetail: String,
    parentoption: String,
    parentexpiry: String,
    highlight: String,
    template: String,
    created: String)

object Msg {
  implicit lazy val msgFormat = Jsonx.formatCaseClass[Msg]

  def toQRMsg(u: Msg): QRMsg = {
    QRMsg(
        u.id,
        u.feedid,
        u.shopid,
        u.userid,
        u.typ,
        u.activity,
        u.shopname,
        u.title,
        u.options,
        u.currency,
        u.price,
        u.discount,
        u.deliverycost,
        u.deliveryoption,
        u.deliveryaddress,
        u.expiry,
        u.lat,
        u.lng,
        u.region,
        u.created
      )
  }
}

//QR messages are smaller for creating QRs.
case class QRMsg(
    id: String,
    feedid: String,
    shopid: String,
    userid: String,
    typ: String,
    activity: String,
    shopname: String,
    title: String,
    options: String,
    currency: String,
    price: String,
    discount: String,
    deliverycost: String,
    deliveryoption: String,
    deliveryaddress: String,
    expiry: String,
    lat: String,
    lng: String,
    region: String,
    created: String)

object QRMsg {
  implicit lazy val qrmsgFormat = Jsonx.formatCaseClass[QRMsg]
}
// Draft-Mail, Draft-Promo, Draft-Event, Draft-Order, Draft-Reservation
// Mail - sender, to, message, subject, created, fdback,
// Promo - user, shop, message, expiry, price, summary, category, created
// Event - shop, message, expiry, price, summary, category, created
// Order - user, shop, fdback,
