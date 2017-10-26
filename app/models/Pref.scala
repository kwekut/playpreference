// http://stackoverflow.com/questions/23571677/22-fields-limit-in-scala-2-11-play-framework-2-3-case-classes-and-functions
// object Pref {
//   implicit val prfReads: Reads[Pref] = (
//     (JsPath \ "userid").read[String] and
//     (JsPath \ "typ").read[String] and
//     (JsPath \ "keywords").read[List[String]] and
//     (JsPath \ "top50keywords").read[List[String]] and
//     (JsPath \ "flatkeywords").read[String] and
//     (JsPath \ "shopids").read[List[String]] and
//     (JsPath \ "shopscount").read[Int] and
//     (JsPath \ "top25shopids").read[String] and
//     (JsPath \ "flatshopids").read[String] and
//     (JsPath \ "shopnames").read[List[String]] and
//     (JsPath \ "top10shopnames").read[String] and
//     (JsPath \ "flatshopnames").read[String] and
//     (JsPath \ "customerids").read[List[String]] and
//     (JsPath \ "customerscount").read[Int] and
//     (JsPath \ "flatcustomerids").read[String] and
//     (JsPath \ "productids").read[List[String]] and
//     (JsPath \ "productscount").read[Int] and
//     (JsPath \ "top10productids").read[String] and
//     (JsPath \ "flatproductids").read[String] and
//     (JsPath \ "productnames").read[List[String]] and
//     (JsPath \ "top10productnames").read[String] and
//     (JsPath \ "flatproductnames").read[String] and
//     (JsPath \ "spend").read[List[Int]] and
//     (JsPath \ "spendcount").read[Int] and
//     (JsPath \ "averagespend").read[Int] and
//     (JsPath \ "locations").read[List[String]] and
//     (JsPath \ "locationcount").read[Int] and
//     (JsPath \ "toplocation").read[String] and
//     (JsPath \ "top5locations").read[String] and
//     (JsPath \ "activities").read[List[String]] and 
//     (JsPath \ "activitiescount").read[Int] and
//     (JsPath \ "top5activities").read[String] and   
//     (JsPath \ "suggestedshops").read[List[String]] and
//     (JsPath \ "flatsuggestedshops").read[String] and
//     (JsPath \ "suggestedcustomers").read[List[String]] and
//     (JsPath \ "flatsuggestedcustomers").read[String] and
//     (JsPath \ "created").read[String]
//   )(Pref.apply _)

//   // val name = (json \ "name").as[String]

//   // def writes(resident: Resident) = Json.obj(
//   //   "name" -> resident.name,
//   //   "age" -> resident.age,
//   //   "role" -> Json.arr(resident.role)
//   // )  
// //implicit val prfWrites: Writes[Pref] = (
//   def writes(p: Pref) = Json.obj(  
//     "userid" -> p.userid,
//     "typ" -> p.typ,
//     "keywords" -> p.keywords,
//     "top50keywords" -> p.top50keywords,
//     "flatkeywords" -> p.flatkeywords,
//     "shopids" -> Json.arr(p.shopids),
//     "shopscount" -> p.shopscount,
//     "top25shopids" -> p.
//     "flatshopids" -> p.
//     "shopnames" -> p.
//     "top10shopnames" -> p.
//     "flatshopnames" -> p.
//     "customerids" -> p.
//     "customerscount" -> p.
//     "flatcustomerids" -> p.
//     "productids" -> p.
//     "productscount" -> p.
//     "top10productids" -> p.
//     "flatproductids" -> p.
//     "productnames" -> p.
//     "top10productnames" -> p.
//     "flatproductnames" -> p.
//     "spend" -> p.
//     "spendcount" -> p.
//     "averagespend" -> p.
//     "locations" -> p.
//     "locationcount" -> p.
//     "toplocation" -> p.
//     "top5locations" -> p.
//     "activities" -> p. 
//     "activitiescount" -> p.
//     "top5activities" -> p.   
//     "suggestedshops" -> p.
//     "flatsuggestedshops" -> p.
//     "suggestedcustomers" -> p.
//     "flatsuggestedcustomers" -> p.
//     "created" -> p.
//   )