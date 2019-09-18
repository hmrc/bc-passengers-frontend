package models

import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json, OWrites, Writes}
import util._

import scala.math.BigDecimal.RoundingMode

object CalculatorServiceRequest {

  implicit def writes(implicit messages: Messages): OWrites[CalculatorServiceRequest] = {

    implicit val piw: Writes[PurchasedItem] = new Writes[PurchasedItem] {

      override def writes(item: PurchasedItem): JsValue = {

        val description = item.productTreeLeaf.getDescriptionArgs(item.purchasedProductInstance, long = false) match {
          case Some( (messageKey, args) ) => messages(messageKey, args : _*)
          case _ => messages(item.name)  //Should not happen
        }

        Json.obj(
          "purchaseCost" -> item.gbpCost.setScale(2, RoundingMode.DOWN).toString,
          "rateId" -> item.productTreeLeaf.rateID,
          "weightOrVolume" -> item.purchasedProductInstance.weightOrVolume,
          "noOfUnits" -> item.purchasedProductInstance.noOfSticks,
          "metadata" -> Json.obj(
            "description" -> description,
            "name" -> item.name,
            "cost" -> item.purchasedProductInstance.cost.map(_.setScale(2, RoundingMode.DOWN).toString),
            "currency" -> item.currency,
            "country" -> item.purchasedProductInstance.country,
            "exchangeRate" -> item.exchangeRate
          )
        ).stripNulls
      }
    }
    Json.writes[CalculatorServiceRequest]
  }
}

case class CalculatorServiceRequest(
  isPrivateCraft: Boolean,
  isAgeOver17: Boolean,
  isVatResClaimed: Option[Boolean],
  isBringingDutyFree: Boolean,
  isIrishBorderCrossing: Boolean,
  items: List[PurchasedItem]
)


object LimitRequest {

  implicit val writes: OWrites[LimitRequest] = {

    implicit val piw: Writes[SpeculativeItem] = new Writes[SpeculativeItem] {

      override def writes(item: SpeculativeItem): JsValue = {

        Json.obj(
          "purchaseCost" -> item.gbpCost.setScale(2, RoundingMode.DOWN).toString,
          "rateId" -> item.productTreeLeaf.rateID,
          "weightOrVolume" -> item.purchasedProductInstance.weightOrVolume,
          "noOfUnits" -> item.purchasedProductInstance.noOfSticks,
          "metadata" -> Json.obj()
        ).stripNulls
      }
    }
    Json.writes[LimitRequest]
  }
}

case class LimitRequest(isPrivateCraft: Boolean, isAgeOver17: Boolean, isVatResClaimed: Option[Boolean], items: List[SpeculativeItem])

