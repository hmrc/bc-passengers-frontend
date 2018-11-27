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
case class CalculatorResponse(alcohol: Option[Alcohol], tobacco: Option[Tobacco], otherGoods: Option[OtherGoods], calculation: Calculation) {


  def hasOnlyGBP: Boolean = {
    val currencies = for {
      alcohol <- alcohol.toList
      tobacco <- tobacco.toList
      otherGoods <- otherGoods.toList
      ab <- alcohol.bands
      tb <- tobacco.bands
      ob <- otherGoods.bands
    } yield {
      ab.items.map(_.metadata.currency) ++
        tb.items.map(_.metadata.currency) ++
        ob.items.map(_.metadata.currency)
    }

    !currencies.flatten.exists(_.code !="GBP")
  }

  def getItemsWithTaxToPay: List[Item] = {

    val alcoholItems = for(typ <- alcohol.toList; items <- typ.bands.map(_.items); item <- items if BigDecimal(item.calculation.allTax) > 0) yield item
    val tobaccoItems = for(typ <- tobacco.toList; items <- typ.bands.map(_.items); item <- items if BigDecimal(item.calculation.allTax) > 0) yield item
    val otherGoodsItems = for(typ <- otherGoods.toList; items <- typ.bands.map(_.items); item <- items if BigDecimal(item.calculation.allTax) > 0) yield item

    val allItems = alcoholItems ++ tobaccoItems ++ otherGoodsItems

    allItems.filter(item => BigDecimal(item.calculation.allTax) > 0)

  }


  def asDto: CalculatorResponseDto = {

    val alcoholItems = this.alcohol.map(_.bands.flatMap(b => b.items.map(i => (b.code, i)))).getOrElse(Nil)
    val tobaccoItems = this.tobacco.map(_.bands.flatMap(b => b.items.map(i => (b.code, i)))).getOrElse(Nil)
    val otherGoodsItems = this.otherGoods.map(_.bands.flatMap(b => b.items.map(i => (b.code, i)))).getOrElse(Nil)

    val bands = (alcoholItems ++ tobaccoItems ++ otherGoodsItems).groupBy(_._1).map { case (key, list) => (key, list.map( _._2 )) }

    CalculatorResponseDto(bands, this.calculation, hasOnlyGBP)
  }
}
