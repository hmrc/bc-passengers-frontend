/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{Json, OWrites, Writes}
import util._

import scala.math.BigDecimal.RoundingMode

object CalculatorServiceRequest {

  implicit def writes(implicit messages: MessagesApi): OWrites[CalculatorServiceRequest] = {

    implicit val piw: Writes[PurchasedItem] = (item: PurchasedItem) => {
      implicit val defaultLanguage: Lang = Lang("en")

      val descriptionLabels =
        item.productTreeLeaf.getDescriptionLabels(item.purchasedProductInstance, long = false) match {
          case Some((messageKey, args)) => DescriptionLabels(messageKey, args)
          case _                        => DescriptionLabels(item.name, List.empty) //Should not happen
        }

      Json
        .obj(
          "purchaseCost"   -> item.gbpCost.setScale(2, RoundingMode.DOWN).toString,
          "rateId"         -> item.productTreeLeaf.rateID,
          "weightOrVolume" -> item.purchasedProductInstance.weightOrVolume,
          "noOfUnits"      -> item.purchasedProductInstance.noOfSticks,
          "isVatPaid"      -> item.purchasedProductInstance.isVatPaid,
          "isExcisePaid"   -> item.purchasedProductInstance.isExcisePaid,
          "isUccRelief"    -> item.purchasedProductInstance.isUccRelief,
          "isCustomPaid"   -> item.purchasedProductInstance.isCustomPaid,
          "metadata"       -> Json.obj(
            "description"       -> messages(
              descriptionLabels.description,
              descriptionLabels.args.map(messages(_).toLowerCase): _*
            ),
            "descriptionLabels" -> descriptionLabels,
            "name"              -> item.name,
            "cost"              -> item.purchasedProductInstance.cost.map(_.setScale(2, RoundingMode.DOWN).toString),
            "currency"          -> item.currency,
            "country"           -> item.purchasedProductInstance.country,
            "exchangeRate"      -> item.exchangeRate,
            "originCountry"     -> item.purchasedProductInstance.originCountry
          )
        )
        .stripNulls
    }
    Json.writes[CalculatorServiceRequest]
  }
}

case class CalculatorServiceRequest(
  isPrivateCraft: Boolean,
  isAgeOver17: Boolean,
  isArrivingNI: Boolean,
  items: List[PurchasedItem]
)

object LimitRequest {

  implicit val writes: OWrites[LimitRequest] = {

    implicit val piw: Writes[SpeculativeItem] = (item: SpeculativeItem) => {

      Json
        .obj(
          "purchaseCost"   -> item.gbpCost.setScale(2, RoundingMode.DOWN).toString,
          "rateId"         -> item.productTreeLeaf.rateID,
          "weightOrVolume" -> item.purchasedProductInstance.weightOrVolume,
          "noOfUnits"      -> item.purchasedProductInstance.noOfSticks,
          "metadata"       -> Json.obj()
        )
        .stripNulls
    }
    Json.writes[LimitRequest]
  }
}

case class LimitRequest(
  isPrivateCraft: Boolean,
  isAgeOver17: Boolean,
  isArrivingNI: Boolean,
  items: List[SpeculativeItem]
)
