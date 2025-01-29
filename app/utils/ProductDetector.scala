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

package utils

import models.{JourneyData, PurchasedProductInstance}

trait ProductDetector {

  def checkAlcoholProductExists(
    productToken: String,
    wineOrSparklingExists: Boolean,
    ciderOrOtherAlcoholExists: Boolean,
    beerOrSpiritExists: Boolean
  ): Boolean =
    productToken match {
      case token if token.contains("wine")                             => wineOrSparklingExists
      case token if token.contains("cider") || token.contains("other") => ciderOrOtherAlcoholExists
      case _                                                           => beerOrSpiritExists
    }

  def checkProductExists(journeyData: JourneyData, path: String): Boolean =
    checkProductExistsInPurchasedProductInstances(
      journeyData,
      path
    ) || checkProductExistsInOldPurchaseProductInstances(journeyData, path)

  private def checkProductExistsInPurchasedProductInstances(journeyData: JourneyData, path: String): Boolean =
    journeyData.purchasedProductInstances.exists(_.path.toString.contains(path))

  private def checkProductExistsInOldPurchaseProductInstances(journeyData: JourneyData, path: String): Boolean =
    journeyData.declarationResponse
      .fold[List[PurchasedProductInstance]](List.empty)(_.oldPurchaseProductInstances)
      .exists(_.path.toString.contains(path))
}
