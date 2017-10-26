package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

//Scala Cassandra model
case class Shop(
  shopid: String, //UUID as String
  shopcategory: Set[String],
  shopoperators: Set[String], //UUIDs as strings
  shoptitle: Option[String],
  shopsummary: Option[String],
  shopabout: Option[String],
  shopaddress: Option[String],
  shopphone: Option[String],
  shopemail: Option[String],
  lat: Option[String],
  lng: Option[String],
  region: Option[String],
  shoppics: Set[String],
  shopfollowers: Set[String],
  shoplikes: Set[String],
  shopratings: Set[String],
  shoptemplate: Option[String],
  created: String)

object Shop{
    def buildShp(s: Shop): Shp = {
      Shp(
        s.shopid,
        s.shopid,
        s.shoptitle.getOrElse("null"),
        s.shopcategory.mkString(", "),
       "SHOP",
       "ACTIVITY",
       s.shopsummary.getOrElse("null"),
       s.shopabout.getOrElse("null"),
       s.shopemail.getOrElse("null"),
       s.shopphone.getOrElse("null"),
       s.shopaddress.getOrElse("null"),
       s.lat.getOrElse("null"),
       s.lng.getOrElse("null"),
       s.region.getOrElse("null"),
       s.shoppics.mkString(", "),
       s.shoplikes.size.toString, //likecount
       false, //isliked
       s.shopratings.size.toString, //starcount
       {((s.shopratings.map{x=> x.split(":").last.toInt}).reduceLeft((x,y) => x + y)) / (s.shopratings.size)}.toString,
       false, //israted
       s.shopfollowers.size.toString,
       false, //isfollowing
       "highlight",
       s.shoptemplate.getOrElse("default"),
       s.created)
    }

    def buildClientShp(s: Shop, u: User): Shp = {
      Shp(
        s.shopid,
        s.shopid,
        s.shoptitle.getOrElse("null"),
        s.shopcategory.mkString(", "),
       "SHOP",
       "ACTIVITY",
       s.shopsummary.getOrElse("null"),
       s.shopabout.getOrElse("null"),
       s.shopemail.getOrElse("null"),
       s.shopphone.getOrElse("null"),
       s.shopaddress.getOrElse("null"),
       s.lat.getOrElse("null"),
       s.lng.getOrElse("null"),
       s.region.getOrElse("null"),
       s.shoppics.mkString(", "),
       s.shoplikes.size.toString, //likecount
       u.shoplikes.contains(s.shopid), //isliked
       s.shopratings.size.toString, //starcount
       {((s.shopratings.map{x=> x.split(":").last.toInt}).reduceLeft((x,y) => x + y)) / (s.shopratings.size)}.toString,
       s.shopratings.contains(u.userid), //israted
       s.shopfollowers.size.toString,
       u.followings.contains(s.shopid), //isfollowing
       "highlight",
       s.shoptemplate.getOrElse("default"),
       s.created)
    }

    def copyShop(sp: Shop, s:Shp): Shop = {
      Shop(
        sp.shopid,
        s.category.split(",").toSet,
        sp.shopoperators,
        Some(s.name),
        Some(s.summary),
        Some(s.about),
        Some(s.address),
        Some(s.phone),
        Some(s.email),
        Some(s.lat),
        Some(s.lng),
        Some(s.region),
        s.picurl.split(",").toSet,
        sp.shopfollowers,
        sp.shoplikes,
        sp.shopratings,
        Some(s.template),
        s.created
      )
    }

implicit lazy val shopFormat = Jsonx.formatCaseClass[Shop]

}

case class  Shp(
  id: String,
  shopid: String,
  name: String,
  category: String,
  typ: String,
  activity: String,
  summary: String,
  about: String,
  email: String,
  phone: String,
  address: String,
  lat: String,
  lng: String,
  region: String,
  picurl: String,
  likecount: String,
  isliked: Boolean,
  starcount: String,
  staraverage: String,
  israted: Boolean,
  followcount: String,
  isfollowing: Boolean,
  highlight: String,
  template: String,
  created: String)

object Shp{
  implicit lazy val shpFormat = Jsonx.formatCaseClass[Shp]
}

// token, category, activity, owners
