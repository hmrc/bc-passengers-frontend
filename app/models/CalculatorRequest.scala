package models

import play.api.libs.json.{JsValue, Json, Writes}
import util._

import scala.math.BigDecimal.RoundingMode

object CalculatorRequest {

  implicit val writes = {

    implicit val piw = new Writes[PurchasedItem] {

      override def writes(item: PurchasedItem): JsValue = {

        Json.obj(
          "purchaseCost" -> item.gbpCost.setScale(2, RoundingMode.DOWN).toString,
          "rateId" -> item.productTreeLeaf.rateID,
          "weightOrVolume" -> item.purchasedProductInstance.weightOrVolume,
          "noOfUnits" -> item.purchasedProductInstance.noOfSticks,
          "metadata" -> Json.obj(
            "description" -> item.description,
            "cost" -> item.purchasedProductInstance.cost.map(_.setScale(2, RoundingMode.DOWN).toString),
            "country" -> item.purchasedProductInstance.country,
            "currency" -> item.displayCurrency
          )
        ).stripNulls
      }
    }
    Json.writes[CalculatorRequest]
  }
}

case class CalculatorRequest(isPrivateCraft: Boolean, isAgeOver17: Boolean, items: List[PurchasedItem]) {
  def hasOnlyGBP: Boolean = !items.exists(_.currency.code != "GBP")
}
