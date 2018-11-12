package models

import play.api.libs.json.{JsValue, Json, OWrites, Writes}
import util._

import scala.math.BigDecimal.RoundingMode

object CalculatorRequest {

  implicit val writes: OWrites[CalculatorRequest] = {

    implicit val piw: Writes[PurchasedItem] = new Writes[PurchasedItem] {

      override def writes(item: PurchasedItem): JsValue = {

        Json.obj(
          "purchaseCost" -> item.gbpCost.setScale(2, RoundingMode.DOWN).toString,
          "rateId" -> item.productTreeLeaf.rateID,
          "weightOrVolume" -> item.purchasedProductInstance.weightOrVolume,
          "noOfUnits" -> item.purchasedProductInstance.noOfSticks,
          "metadata" -> Json.obj(
            "description" -> item.description,
            "declarationMessageDescription" -> item.declarationMessageDescription,
            "cost" -> item.purchasedProductInstance.cost.map(_.setScale(2, RoundingMode.DOWN).toString),
            "currency" -> item.currency,
            "country" -> item.purchasedProductInstance.country,
            "exchangeRate" -> item.exchangeRate
          )
        ).stripNulls
      }
    }
    Json.writes[CalculatorRequest]
  }
}

case class CalculatorRequest(isPrivateCraft: Boolean, isAgeOver17: Boolean, items: List[PurchasedItem])
