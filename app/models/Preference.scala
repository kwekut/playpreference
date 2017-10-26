package models

import java.util.UUID
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx
import scala.collection.immutable.ListMap
//http://markusjais.com/the-groupby-method-from-scalas-collection-library/
//https://www.safaribooksonline.com/library/view/scala-cookbook/9781449340292/ch11s23.html
//http://stackoverflow.com/questions/2972871/how-to-sort-a-scala-collection-mapjava-lang-string-int-by-its-values
//https://github.com/xdotai/play-json-extensions
////////////////Keywords////////////
// caucasian, afghan, african, african-american, 
// italian, chinese, korean, indian, vietnamese, mexican, middle-eastern 
// children, advanced-age, adult, young-adult, male, female, bisexual, 
// bluecolor, whitecolor, metrosexual, millinial, baby-boomers,
// gen-x, gen-y, gen-z,
// gay, geeky, hippy, avantgarde, 
// christian, moslem, hindu, buddist, non-denom, 
// sport, luxury, out, sea, nature, hitech 
// club, pub, restaurant, grocery, nails-tan-massage, clothing,
// coffee, bakery, gym, cleaners, 
// town, city, zip
case class AddKmsgPreference(user: User, kmsg: KafkaMessage)
case class AddShopPreference(user: User, shop: Shop, activity: String, id: String, date: String)
case class AddFeedPreference(user: User, feed: Feed, activity: String, id: String, date: String)
case class AddRevPreference(user: User, rev: Review, id: String, date: String)

case class Preference(
  userid: String, 
  shoporcust: String, 
  transactionids: List[String], 
  expiry: List[String], 
  keywords: List[String], 
  shopids: List[String], 
  shopnames: List[String], 
  customerids: List[String], 
  productids: List[String], 
  productnames: List[String], 
  spend: List[Int],
  discount: List[String], 
  ratings: List[String], 
  locations: List[String], 
  typs: List[String], 
  activities: List[String], 
  suggestedshops: List[String], 
  suggestedcustomers: List[String],
  created: String)

//activityids: List[String] traces every click you make
//val queryString = updatedMap.map(pair => pair._1+"="+pair._2).mkString("?","&","")
object Preference{
  implicit lazy val preferenceFormat = Jsonx.formatCaseClass[Preference]
}
//maptop50product: List[String] - product and freq

case class Pref(
  userid: String, 
  shoporcust: String, 
  transactionids: List[String], 
  flattransactionids: String,
  prdid2typ2exp: List[String],
  keywords: List[String], 
  flattop50keywords: String, 
  flatkeywords: String, 
  shopids: List[String], 
  shopscount: Int, 
  flattop25shopids: String, 
  flatshopids: String, 
  shopnames: List[String], 
  maptop50shopnames: List[String], 
  flattop50shopnames: String, 
  flatshopnames: String,
  customerids: List[String], 
  customerscount: Int, 
  flattop25customerids: String, 
  flatcustomerids: String,
  productids: List[String], 
  top10productids: List[String], 
  productscount: Int, 
  flattop50productids: String, 
  flatproductids: String,
  productnames: List[String], 
  maptop100productnames: List[String], 
  flattop100productnames: String, 
  flatproductnames: String,
  spend: List[Int], 
  spendcount: Int, 
  averagespend: Int,
  discount: List[String],
  discountcount: Int,
  averagediscount: Int,
  ratings: List[String],
  ratingscount: Int,
  averageratings: Int,
  locations: List[String], 
  locationcount: Int, 
  flattoplocation: String, 
  flattop5locations: String,
  typs: List[String], 
  typscount: Int, 
  maptop25typs: List[String], 
  flattop25typs: String,
  activities: List[String], 
  activitiescount: Int, 
  maptop25activities: List[String], 
  flattop25activities: String,
  suggestedshops: List[String], 
  flatsuggestedshops: String,
  suggestedcustomers: List[String], 
  flatsuggestedcustomers: String,
  created: String)

object Pref {
  implicit lazy val prefFormat = Jsonx.formatCaseClass[Pref]

    def addPreference(k: Pref, m: Preference): Pref = {
        Pref(
            (k.userid), 
            (k.shoporcust),
            (k.transactionids ++ m.transactionids),
            (k.transactionids ++ m.transactionids).mkString(" "),
            ((k.prdid2typ2exp) ++ List(List(m.productids.head.toString, m.typs.head.toString, m.expiry.head.toString).mkString(" "))),
            (k.keywords ++ m.keywords), 
            ListMap((k.keywords ++ m.keywords).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(50).keys.mkString(" "),
            (k.keywords ++ m.keywords).mkString(" "), 
            (k.shopids ++ m.shopids),
            (k.shopids ++ m.shopids).size,
            ListMap((k.shopids ++ m.shopids).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(15).keys.mkString(" "),
            (k.shopids ++ m.shopids).mkString(" "),
            (k.shopnames ++ m.shopnames),
            (k.shopnames ++ m.shopnames).groupBy(x=>x).mapValues(_.size).toList.sortBy(_._2).takeRight(50)map(x=> x.toString),
            ListMap((k.shopnames ++ m.shopnames).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(50).keys.mkString(" "),
            (k.shopnames ++ m.shopnames).mkString(" "),
            (k.customerids ++ m.customerids),
            (k.customerids ++ m.customerids).size,
            ListMap((k.customerids ++ m.customerids).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(25).keys.mkString(" "), 
            (k.customerids ++ m.customerids).mkString(" "), 
            (k.productids ++ m.productids),
            ListMap((k.productids ++ m.productids).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(10).keys.toList,
            (k.productids ++ m.productids).size, 
            ListMap((k.productids ++ m.productids).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(50).keys.mkString(", "),
            (k.productids ++ m.productids).mkString(" "),
            (k.productnames ++ m.productnames),
            (k.productnames ++ m.productnames).groupBy(x=>x).mapValues(_.size).toList.sortBy(_._2).takeRight(100)map(x=> x.toString),
            ListMap((k.productnames ++ m.productnames).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(100).keys.mkString(", "),
            (k.productnames ++ m.productnames).mkString(" "),            
            (k.spend ++ m.spend),
            (k.spend ++ m.spend).size, 
            (k.spend ++ m.spend).sum/(k.spend ++ m.spend).size,
            (k.discount ++ m.discount),
            (k.discount ++ m.discount).size, 
            ((k.discount ++ m.discount)map((s: String) => s.toInt)).sum/(k.discount ++ m.discount).size, 
            (k.ratings ++ m.ratings),
            (k.ratings ++ m.ratings).size, 
            ((k.ratings ++ m.ratings)map((s: String) => s.toInt)).sum/(k.spend ++ m.spend).size,           
            (k.locations ++ m.locations),
            (k.locations ++ m.locations).size,
            ListMap((k.locations ++ m.locations).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(1).keys.mkString(","),
            ListMap((k.locations ++ m.locations).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(3).keys.mkString(","),
            (k.typs ++ m.typs),
            (k.typs ++ m.typs).size,
            (k.typs ++ m.typs).groupBy(x=>x).mapValues(_.size).toList.sortBy(_._2).takeRight(25)map(x=> x.toString),
            ListMap((k.typs ++ m.typs).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(25).keys.mkString(" "),
            (k.activities ++ m.activities),
            (k.activities ++ m.activities).size,
            (k.activities ++ m.activities).groupBy(x=>x).mapValues(_.size).toList.sortBy(_._2).takeRight(25)map(x=> x.toString),
            ListMap((k.activities ++ m.activities).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(25).keys.mkString(" "),
            k.suggestedshops,
            k.suggestedshops.mkString(" "), 
            k.suggestedcustomers, 
            k.suggestedcustomers.mkString(" "),           
            k.created
        )
    } 

    def newPref(m: Preference): Pref = {
        Pref(
            (m.userid), 
            (m.shoporcust),
            (m.transactionids),
            (m.transactionids).mkString(" "),
            List(List(m.productids.head, m.typs.head, m.expiry.head).mkString(" ")),
            (m.keywords), 
            ListMap((m.keywords).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(50).keys.mkString(" "),
            (m.keywords).mkString(" "), 
            (m.shopids),
            (m.shopids).size,
            ListMap((m.shopids).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(50).keys.mkString(" "),
            (m.shopids).mkString(" "),
            (m.shopnames),
            (m.shopnames).groupBy(x=>x).mapValues(_.size).toList.sortBy(_._2).takeRight(50)map(x=> x.toString),
            ListMap((m.shopnames).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(50).keys.mkString(" "),
            (m.shopnames).mkString(" "),
            (m.customerids),
            (m.customerids).size,
            ListMap((m.customerids).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(25).keys.mkString(" "),  
            (m.customerids).mkString(" "), 
            (m.productids),
            ListMap((m.productids).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(10).keys.toList,
            (m.productids).size,
            ListMap((m.productids).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(50).keys.mkString(" "),
            (m.productids).mkString(" "),
            (m.productnames),
            (m.productnames).groupBy(x=>x).mapValues(_.size).toList.sortBy(_._2).takeRight(100)map(x=> x.toString),
            ListMap((m.productnames).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(100).keys.mkString(" "),
            (m.productnames).mkString(" "),            
            (m.spend),
            (m.spend).size,
            (m.spend).sum/(m.spend).size,
            (m.discount),
            (m.discount).size,
            ((m.discount)map((s: String) => s.toInt)).sum/(m.discount).size,
            (m.ratings),
            (m.ratings).size,
            ((m.ratings)map((s: String) => s.toInt)).sum/(m.ratings).size,
            (m.locations),
            (m.locations).size,
            ListMap((m.locations).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(1).keys.mkString(","),
            ListMap((m.locations).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(3).keys.mkString(","),
            (m.typs),
            (m.typs).size,
            (m.typs).groupBy(x=>x).mapValues(_.size).toList.sortBy(_._2).takeRight(25)map(x=> x.toString),
            ListMap((m.typs).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(25).keys.mkString(" "),
            (m.activities),
            (m.activities).size,
            (m.activities).groupBy(x=>x).mapValues(_.size).toList.sortBy(_._2).takeRight(25)map(x=> x.toString),
            ListMap((m.activities).groupBy(x=>x).mapValues(_.size).toSeq.sortBy(_._2):_*).takeRight(25).keys.mkString(" "),
            m.suggestedshops,
            m.suggestedshops.mkString(" "), 
            m.suggestedcustomers, 
            m.suggestedcustomers.mkString(" "),           
            m.created
      )
    } 
}

//http://markusjais.com/the-groupby-method-from-scalas-collection-library/
//https://www.safaribooksonline.com/library/view/scala-cookbook/9781449340292/ch11s23.html
//http://stackoverflow.com/questions/2972871/how-to-sort-a-scala-collection-mapjava-lang-string-int-by-its-values
//val modeValue = l.groupBy(i=>i).mapValues(_.size).maxBy(_._2)._2
//val top3 = l.groupBy(x=>x).mapValues(_.size).sortBy(_._2).takeRight(3).keys.mkString(", "),


////////////Birds of the same feathers/////////
//userid, userage, usergender, userrace, userpreference, userlocation, user, 
//activitydate, activitytype, activitylocation, activityspend, activityvendor, activityurl, activitykeywords, activityduration, activitypreceding, activityfollowing
//vendingdate, vendortype, venderlocation, vendoramount,
//Customer habbits/activity/spend/Customer favs shops/prods/ Related favs
//Race-Demo-Activness-Location-Popularity-Gender
//Most spending clients/Most active clients

//Get Users processed prefernce
//Search for users with similar preferences
//Get shops list of users with similar pref that are not on users list (intersection)
//Get shops that also have processed kw
 

//Tally keywords for user. Add shop keywords to users
//Take keywords & tally at 3 am

//{activity, typ, user, url, shop, product, qty, amount, comments}




//https://www.elastic.co/guide/en/elasticsearch/guide/1.x/match-multi-word.html
//Multiword Queries
//Document 4 is the most relevant because it contains "brown" twice and "dog" once.






