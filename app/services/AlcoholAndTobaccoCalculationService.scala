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

import models.{AlcoholDto, JourneyData, TobaccoDto}

class AlcoholAndTobaccoCalculationService {

  def alcoholAddHelper(contextJourneyData: JourneyData, dto: AlcoholDto, productToken: String): BigDecimal = {

    val alcoholProductTotalVolume: BigDecimal =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString.contains(productToken))
        .map(_.weightOrVolume.getOrElse(BigDecimal(0)))
        .sum
        .bigDecimal

    val totalAlcoholVolume: BigDecimal =
      (dto.weightOrVolume + alcoholProductTotalVolume)
        .setScale(2, BigDecimal.RoundingMode.HALF_UP)

    totalAlcoholVolume
  }

  def alcoholEditHelper(contextJourneyData: JourneyData, dto: AlcoholDto, productToken: String): BigDecimal = {

    val originalVolume: BigDecimal =
      contextJourneyData.workingInstance.flatMap(_.weightOrVolume).getOrElse(BigDecimal(0))

    val alcoholProductTotalVolume: BigDecimal =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString.contains(productToken))
        .map(_.weightOrVolume.getOrElse(BigDecimal(0)))
        .sum
        .bigDecimal

    val totalAlcoholVolume: BigDecimal =
      (dto.weightOrVolume + alcoholProductTotalVolume - originalVolume)
        .setScale(2, BigDecimal.RoundingMode.HALF_UP)

    totalAlcoholVolume
  }

  def looseTobaccoAddHelper(contextJourneyData: JourneyData, dto: TobaccoDto): BigDecimal = {
    val chewingTobaccoTotalWeight: BigDecimal =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString == "tobacco/chewing-tobacco")
        .map(_.weightOrVolume.getOrElse(BigDecimal(0)))
        .sum
        .bigDecimal

    val rollingTobaccoTotalWeight: BigDecimal      =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString == "tobacco/rolling-tobacco")
        .map(_.weightOrVolume.getOrElse(BigDecimal(0)))
        .sum
        .bigDecimal
    val looseTobaccoTotalWeightInGrams: BigDecimal =
      (
        dto.weightOrVolume.getOrElse(BigDecimal(0)) +
          chewingTobaccoTotalWeight +
          rollingTobaccoTotalWeight
      )
        .setScale(5, BigDecimal.RoundingMode.HALF_UP) * 1000

    looseTobaccoTotalWeightInGrams
  }

  def looseTobaccoEditHelper(contextJourneyData: JourneyData, dto: TobaccoDto): BigDecimal = {

    val originalWeight: BigDecimal =
      contextJourneyData.workingInstance.flatMap(_.weightOrVolume).getOrElse(BigDecimal(0))

    val chewingTobaccoTotalWeight: BigDecimal =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString == "tobacco/chewing-tobacco")
        .map(_.weightOrVolume.getOrElse(BigDecimal(0)))
        .sum
        .bigDecimal

    val rollingTobaccoTotalWeight: BigDecimal      =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString == "tobacco/rolling-tobacco")
        .map(_.weightOrVolume.getOrElse(BigDecimal(0)))
        .sum
        .bigDecimal
    val looseTobaccoTotalWeightInGrams: BigDecimal =
      (
        dto.weightOrVolume.getOrElse(BigDecimal(0)) +
          chewingTobaccoTotalWeight +
          rollingTobaccoTotalWeight -
          originalWeight
      )
        .setScale(5, BigDecimal.RoundingMode.HALF_UP) * 1000

    looseTobaccoTotalWeightInGrams
  }

  def noOfSticksTobaccoAddHelper(
    contextJourneyData: JourneyData,
    dto: TobaccoDto,
    productToken: String
  ): Int = {
    val totalNoOfSticks: Int          =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString.contains(productToken))
        .map(_.noOfSticks.getOrElse(0))
        .sum
    val totalNoOfSticksAfterCalc: Int =
      dto.noOfSticks.getOrElse(0) + totalNoOfSticks

    totalNoOfSticksAfterCalc
  }

  def noOfSticksTobaccoEditHelper(
    contextJourneyData: JourneyData,
    dto: TobaccoDto,
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
      dto.noOfSticks.getOrElse(0) + totalNoOfSticks - originalNoOfSticks

    totalNoOfSticksAfterCalc
  }

}
