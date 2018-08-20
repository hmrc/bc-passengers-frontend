package models

import play.api.libs.json.{Format, Json}

object Calculation {
  implicit val formats = Json.format[Calculation]
}

object Metadata {
  implicit val formats = Json.format[Metadata]
}

object Item {
  implicit val formats = Json.format[Item]
}

object Band {
  implicit val formats = Json.format[Band]
}

object Alcohol {
  implicit val formats = Json.format[Alcohol]
}

object Tobacco {
  implicit val formats = Json.format[Tobacco]
}

object OtherGoods {
  implicit val formats = Json.format[OtherGoods]
}

object CalculatorResponse {
  implicit val formats = Json.format[CalculatorResponse]
}

case class Calculation(excise: String, customs: String, vat: String, allTax: String)
case class Metadata(description: String, cost: String, currency: String)
case class Item(rateId: String, purchaseCost: String, noOfUnits: Option[Int], weightOrVolume: Option[BigDecimal], calculation: Calculation, metadata: Metadata)
case class Band(code: String, items: List[Item], calculation: Calculation)
case class Alcohol(bands: List[Band], calculation: Calculation)
case class Tobacco(bands: List[Band], calculation: Calculation)
case class OtherGoods(bands: List[Band], calculation: Calculation)
case class CalculatorResponse(alcohol: Option[Alcohol], tobacco: Option[Tobacco], otherGoods: Option[OtherGoods], calculation: Calculation)
