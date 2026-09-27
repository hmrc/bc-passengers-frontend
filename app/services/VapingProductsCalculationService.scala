/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import models.{JourneyData, PurchasedProductInstance}
import utils.{FormatsAndConversions, InstanceDecider, ProductDetector}

class VapingProductsCalculationService extends InstanceDecider with ProductDetector with FormatsAndConversions {

  private def sumPreviouslyDeclaredVapeVolume(contextJourneyData: JourneyData, productToken: String): BigDecimal =
    contextJourneyData.declarationResponse
      .fold[List[PurchasedProductInstance]](List.empty)(_.oldPurchaseProductInstances)
      .filter { product =>
        checkAlcoholProductExists(
          productToken = productToken,
          wineOrSparklingExists = product.path.toString.contains("wine"),
          ciderOrOtherAlcoholExists =
            product.path.toString.contains("cider") || product.path.toString.contains("other"),
          beerOrSpiritExists = product.path.toString.contains(productToken)
        )
      }
      .map(_.weightOrVolume.getOrElseZero)
      .sum

  private def sumVapeProductTotalVolume(contextJourneyData: JourneyData, productToken: String): BigDecimal =
    contextJourneyData.purchasedProductInstances
      .filter { product =>
        checkAlcoholProductExists(
          productToken = productToken,
          wineOrSparklingExists = product.path.toString.contains("wine"),
          ciderOrOtherAlcoholExists =
            product.path.toString.contains("cider") || product.path.toString.contains("other"),
          beerOrSpiritExists = product.path.toString.contains(productToken)
        )
      }
      .map(_.weightOrVolume.getOrElseZero)
      .sum

  def vapeAddHelper(
    contextJourneyData: JourneyData,
    weightOrVolume: BigDecimal,
    productToken: String
  ): BigDecimal = {

    val previouslyDeclaredVapeVolume: BigDecimal =
      sumPreviouslyDeclaredVapeVolume(contextJourneyData, productToken)

    val vapeProductTotalVolume: BigDecimal =
      sumVapeProductTotalVolume(contextJourneyData, productToken)

    val totalVapeVolume: BigDecimal =
      (weightOrVolume + previouslyDeclaredVapeVolume + vapeProductTotalVolume).formatDecimalPlaces(5)

    totalVapeVolume
  }

  def vapeEditHelper(
    contextJourneyData: JourneyData,
    weightOrVolume: BigDecimal,
    productToken: String,
    iid: String
  ): BigDecimal = {

    val previouslyDeclaredVapeVolume: BigDecimal =
      sumPreviouslyDeclaredVapeVolume(contextJourneyData, productToken)

    val originalVolume: BigDecimal = originalAmountEnteredWeightOrVolume(contextJourneyData, iid)

    val vapeProductTotalVolume: BigDecimal =
      sumVapeProductTotalVolume(contextJourneyData, productToken)

    val totalVapeVolume: BigDecimal =
      (weightOrVolume + previouslyDeclaredVapeVolume + vapeProductTotalVolume - originalVolume)
        .formatDecimalPlaces(5)

    totalVapeVolume
  }
}
