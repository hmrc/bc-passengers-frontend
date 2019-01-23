package models

import play.api.libs.json.Json

object Calculation {
  implicit val formats = Json.format[Calculation]
}

object ExchangeRate {
  implicit val formats = Json.format[ExchangeRate]
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
case class ExchangeRate(rate: String, date: String)
case class Metadata(description: String, declarationMessageDescription: String, cost: String, currency: Currency, country: Country, exchangeRate: ExchangeRate)
case class Item(rateId: String, purchaseCost: String, noOfUnits: Option[Int], weightOrVolume: Option[BigDecimal], calculation: Calculation, metadata: Metadata)
case class Band(code: String, items: List[Item], calculation: Calculation)

case class Alcohol(bands: List[Band], calculation: Calculation)
case class Tobacco(bands: List[Band], calculation: Calculation)
case class OtherGoods(bands: List[Band], calculation: Calculation)

case class CalculatorResponse(alcohol: Option[Alcohol], tobacco: Option[Tobacco], otherGoods: Option[OtherGoods], calculation: Calculation, withinFreeAllowance: Boolean) {


  def allItemsUseGBP: Boolean = {
    val currencies = alcohol.toList.flatMap(_.bands.flatMap(_.items)).map(_.metadata.currency.code) ++
      tobacco.toList.flatMap(_.bands.flatMap(_.items)).map(_.metadata.currency.code) ++
      otherGoods.toList.flatMap(_.bands.flatMap(_.items)).map(_.metadata.currency.code)

    currencies.forall(_ == "GBP")
  }

  def getItemsWithTaxToPay: List[Item] = {

    val alcoholItems = for(typ <- alcohol.toList; items <- typ.bands.map(_.items); item <- items if BigDecimal(item.calculation.allTax) > 0) yield item
    val tobaccoItems = for(typ <- tobacco.toList; items <- typ.bands.map(_.items); item <- items if BigDecimal(item.calculation.allTax) > 0) yield item
    val otherGoodsItems = for(typ <- otherGoods.toList; items <- typ.bands.map(_.items); item <- items if BigDecimal(item.calculation.allTax) > 0) yield item

    val allItems = alcoholItems ++ tobaccoItems ++ otherGoodsItems

    allItems.filter(item => BigDecimal(item.calculation.allTax) > 0)
  }


  def asDto(applySorting: Boolean): CalculatorResponseDto = {

    val alcoholItems = this.alcohol.map(_.bands.flatMap(b => b.items)).getOrElse(Nil)
    val tobaccoItems = this.tobacco.map(_.bands.flatMap(b => b.items)).getOrElse(Nil)
    val otherGoodsItems = this.otherGoods.map(_.bands.flatMap(b => b.items)).getOrElse(Nil)


    val items = if (applySorting) {
      (alcoholItems ++ tobaccoItems ++ otherGoodsItems).sortBy(item => BigDecimal(item.calculation.allTax) != BigDecimal(0.00))
    } else {
      alcoholItems ++ tobaccoItems ++ otherGoodsItems
    }

    CalculatorResponseDto(items, this.calculation, allItemsUseGBP)
  }
}
