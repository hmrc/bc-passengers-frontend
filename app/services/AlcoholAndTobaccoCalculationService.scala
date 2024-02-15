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

package services

import models.JourneyData
import utils.FormatsAndConversions

class AlcoholAndTobaccoCalculationService extends FormatsAndConversions {

  def alcoholAddHelper(
    contextJourneyData: JourneyData,
    weightOrVolume: BigDecimal,
    productToken: String
  ): BigDecimal = {

    val alcoholProductTotalVolume: BigDecimal =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString.contains(productToken))
        .map(_.weightOrVolume.getOrElseZero)
        .sum
        .bigDecimal

    val totalAlcoholVolume: BigDecimal =
      (weightOrVolume + alcoholProductTotalVolume).formatDecimalPlaces(5)

    totalAlcoholVolume
  }

  def alcoholEditHelper(
    contextJourneyData: JourneyData,
    weightOrVolume: BigDecimal,
    productToken: String
  ): BigDecimal = {

    val originalVolume: BigDecimal =
      contextJourneyData.workingInstance.flatMap(_.weightOrVolume).getOrElseZero

    val alcoholProductTotalVolume: BigDecimal =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString.contains(productToken))
        .map(_.weightOrVolume.getOrElseZero)
        .sum
        .bigDecimal

    val totalAlcoholVolume: BigDecimal =
      (weightOrVolume + alcoholProductTotalVolume - originalVolume).formatDecimalPlaces(5)

    totalAlcoholVolume
  }

  def looseTobaccoAddHelper(contextJourneyData: JourneyData, weightOrVolume: Option[BigDecimal]): BigDecimal = {
    val chewingTobaccoTotalWeight: BigDecimal =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString == "tobacco/chewing-tobacco")
        .map(_.weightOrVolume.getOrElseZero)
        .sum
        .bigDecimal

    val rollingTobaccoTotalWeight: BigDecimal      =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString == "tobacco/rolling-tobacco")
        .map(_.weightOrVolume.getOrElseZero)
        .sum
        .bigDecimal
    val looseTobaccoTotalWeightInGrams: BigDecimal =
      (
        weightOrVolume.getOrElseZero +
          chewingTobaccoTotalWeight +
          rollingTobaccoTotalWeight
      ).formatDecimalPlaces(5)

    looseTobaccoTotalWeightInGrams
  }

  def looseTobaccoEditHelper(contextJourneyData: JourneyData, weightOrVolume: Option[BigDecimal]): BigDecimal = {

    val originalWeight: BigDecimal =
      contextJourneyData.workingInstance.flatMap(_.weightOrVolume).getOrElseZero

    val chewingTobaccoTotalWeight: BigDecimal =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString == "tobacco/chewing-tobacco")
        .map(_.weightOrVolume.getOrElseZero)
        .sum
        .bigDecimal

    val rollingTobaccoTotalWeight: BigDecimal      =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString == "tobacco/rolling-tobacco")
        .map(_.weightOrVolume.getOrElseZero)
        .sum
        .bigDecimal
    val looseTobaccoTotalWeightInGrams: BigDecimal =
      (
        weightOrVolume.getOrElseZero +
          chewingTobaccoTotalWeight +
          rollingTobaccoTotalWeight -
          originalWeight
      ).formatDecimalPlaces(5)

    looseTobaccoTotalWeightInGrams
  }

  def noOfSticksTobaccoAddHelper(
    contextJourneyData: JourneyData,
    noOfSticks: Option[Int],
    productToken: String
  ): Int = {
    val totalNoOfSticks: Int          =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString.contains(productToken))
        .map(_.noOfSticks.getOrElse(0))
        .sum
    val totalNoOfSticksAfterCalc: Int =
      noOfSticks.getOrElse(0) + totalNoOfSticks

    totalNoOfSticksAfterCalc
  }

  def noOfSticksTobaccoEditHelper(
    contextJourneyData: JourneyData,
    noOfSticks: Option[Int],
    productToken: String
  ): Int = {
    val originalNoOfSticks: Int       =
      contextJourneyData.workingInstance.flatMap(_.noOfSticks).getOrElse(0)
    val totalNoOfSticks: Int          =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString.contains(productToken))
        .map(_.noOfSticks.getOrElse(0))
        .sum
    val totalNoOfSticksAfterCalc: Int =
      noOfSticks.getOrElse(0) + totalNoOfSticks - originalNoOfSticks

    totalNoOfSticksAfterCalc
  }
}
