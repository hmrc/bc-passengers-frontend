/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import play.api.i18n.Messages
import play.api.libs.json.{Json, OWrites, Writes}
import util._

import scala.math.BigDecimal.RoundingMode

object CalculatorServiceRequest {

  implicit def writes(implicit messages: Messages): OWrites[CalculatorServiceRequest] = {

    implicit val piw: Writes[PurchasedItem] = (item: PurchasedItem) => {

      val description = item.productTreeLeaf.getDescriptionArgs(item.purchasedProductInstance, long = false) match {
        case Some((messageKey, args)) => messages(messageKey, args: _*)
        case _ => messages(item.name) //Should not happen
      }

      Json.obj(
        "purchaseCost" -> item.gbpCost.setScale(2, RoundingMode.DOWN).toString,
        "rateId" -> item.productTreeLeaf.rateID,
        "weightOrVolume" -> item.purchasedProductInstance.weightOrVolume,
        "noOfUnits" -> item.purchasedProductInstance.noOfSticks,
        "isVatPaid" -> item.purchasedProductInstance.isVatPaid,
        "isExcisePaid" -> item.purchasedProductInstance.isExcisePaid,
        "isUccRelief" -> item.purchasedProductInstance.isUccRelief,
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
    Json.writes[CalculatorServiceRequest]
  }
}

case class CalculatorServiceRequest(
  isPrivateCraft: Boolean,
  isAgeOver17: Boolean,
  isArrivingNI: Boolean,
  isUKVatPaid: Option[Boolean],
  isUKExcisePaid: Option[Boolean],
  isUKResident: Option[Boolean],
  isUccRelief: Option[Boolean],
  items: List[PurchasedItem]
)


object LimitRequest {

  implicit val writes: OWrites[LimitRequest] = {

    implicit val piw: Writes[SpeculativeItem] = (item: SpeculativeItem) => {

      Json.obj(
        "purchaseCost" -> item.gbpCost.setScale(2, RoundingMode.DOWN).toString,
        "rateId" -> item.productTreeLeaf.rateID,
        "weightOrVolume" -> item.purchasedProductInstance.weightOrVolume,
        "noOfUnits" -> item.purchasedProductInstance.noOfSticks,
        "metadata" -> Json.obj()
      ).stripNulls
    }
    Json.writes[LimitRequest]
  }
}

case class LimitRequest(isPrivateCraft: Boolean, isAgeOver17: Boolean, isArrivingNI: Boolean, items: List[SpeculativeItem])

