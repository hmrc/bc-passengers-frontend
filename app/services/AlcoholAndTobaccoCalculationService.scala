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

import models.{JourneyData, PurchasedProductInstance}
import utils.InstanceDecider

class AlcoholAndTobaccoCalculationService extends InstanceDecider {

  private def sumPreviouslyDeclaredAlcoholVolume(contextJourneyData: JourneyData, productToken: String): BigDecimal =
    contextJourneyData.declarationResponse
      .fold[List[PurchasedProductInstance]](List.empty)(_.oldPurchaseProductInstances)
      .filter(_.path.toString.contains(productToken))
      .map(_.weightOrVolume.getOrElseZero)
      .sum

  private def sumAlcoholProductTotalVolume(contextJourneyData: JourneyData, productToken: String): BigDecimal =
    contextJourneyData.purchasedProductInstances
      .filter(_.path.toString.contains(productToken))
      .map(_.weightOrVolume.getOrElseZero)
      .sum

  private def sumPreviouslyDeclaredLooseTobaccoWeight(contextJourneyData: JourneyData): BigDecimal =
    contextJourneyData.declarationResponse
      .fold[List[PurchasedProductInstance]](List.empty)(_.oldPurchaseProductInstances)
      .filter(product =>
        product.path.toString == "tobacco/chewing-tobacco" || product.path.toString == "tobacco/rolling-tobacco"
      )
      .map(_.weightOrVolume.getOrElseZero)
      .sum

  private def sumLooseTobaccoTotalWeight(contextJourneyData: JourneyData): BigDecimal =
    contextJourneyData.purchasedProductInstances
      .filter(product =>
        product.path.toString == "tobacco/chewing-tobacco" || product.path.toString == "tobacco/rolling-tobacco"
      )
      .map(_.weightOrVolume.getOrElseZero)
      .sum

  def alcoholAddHelper(
    contextJourneyData: JourneyData,
    weightOrVolume: BigDecimal,
    productToken: String
  ): BigDecimal = {

    val previouslyDeclaredAlcoholVolume: BigDecimal =
      sumPreviouslyDeclaredAlcoholVolume(contextJourneyData, productToken)

    val alcoholProductTotalVolume: BigDecimal =
      sumAlcoholProductTotalVolume(contextJourneyData, productToken)

    val totalAlcoholVolume: BigDecimal =
      (weightOrVolume + previouslyDeclaredAlcoholVolume + alcoholProductTotalVolume).formatDecimalPlaces(5)

    totalAlcoholVolume
  }

  def alcoholEditHelper(
    contextJourneyData: JourneyData,
    weightOrVolume: BigDecimal,
    productToken: String,
    iid: String
  ): BigDecimal = {

    val previouslyDeclaredAlcoholVolume: BigDecimal =
      sumPreviouslyDeclaredAlcoholVolume(contextJourneyData, productToken)

    val originalVolume: BigDecimal = originalAmountEnteredWeightOrVolume(contextJourneyData, iid)

    val alcoholProductTotalVolume: BigDecimal =
      sumAlcoholProductTotalVolume(contextJourneyData, productToken)

    val totalAlcoholVolume: BigDecimal =
      (weightOrVolume + previouslyDeclaredAlcoholVolume + alcoholProductTotalVolume - originalVolume)
        .formatDecimalPlaces(5)

    totalAlcoholVolume
  }

  def looseTobaccoAddHelper(contextJourneyData: JourneyData, weightOrVolume: Option[BigDecimal]): BigDecimal = {

    val previouslyDeclaredLooseTobaccoWeight: BigDecimal =
      sumPreviouslyDeclaredLooseTobaccoWeight(contextJourneyData)

    val looseTobaccoTotalWeight: BigDecimal =
      sumLooseTobaccoTotalWeight(contextJourneyData)

    val looseTobaccoTotalWeightInGrams: BigDecimal =
      (
        weightOrVolume.getOrElseZero +
          previouslyDeclaredLooseTobaccoWeight +
          looseTobaccoTotalWeight
      ).formatDecimalPlaces(5)

    looseTobaccoTotalWeightInGrams
  }

  def looseTobaccoEditHelper(
    contextJourneyData: JourneyData,
    weightOrVolume: Option[BigDecimal],
    iid: String
  ): BigDecimal = {

    val originalWeight: BigDecimal = originalAmountEnteredWeightOrVolume(contextJourneyData, iid)

    val previouslyDeclaredLooseTobaccoWeight: BigDecimal =
      sumPreviouslyDeclaredLooseTobaccoWeight(contextJourneyData)

    val looseTobaccoTotalWeight: BigDecimal =
      sumLooseTobaccoTotalWeight(contextJourneyData)

    val looseTobaccoTotalWeightInGrams: BigDecimal =
      (
        weightOrVolume.getOrElseZero +
          previouslyDeclaredLooseTobaccoWeight +
          looseTobaccoTotalWeight -
          originalWeight
      ).formatDecimalPlaces(5)

    looseTobaccoTotalWeightInGrams
  }

  def noOfSticksTobaccoAddHelper(
    contextJourneyData: JourneyData,
    noOfSticks: Option[Int],
    productToken: String
  ): Int = {

    val previousDeclarationTotalNoOfSticks: Int =
      contextJourneyData.declarationResponse
        .fold[List[PurchasedProductInstance]](List.empty)(_.oldPurchaseProductInstances)
        .filter(_.path.toString.contains(productToken))
        .map(_.noOfSticks.getOrElse(0))
        .sum

    val totalNoOfSticks: Int =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString.contains(productToken))
        .map(_.noOfSticks.getOrElse(0))
        .sum

    val totalNoOfSticksAfterCalc: Int =
      noOfSticks.getOrElse(0) + previousDeclarationTotalNoOfSticks + totalNoOfSticks

    totalNoOfSticksAfterCalc
  }

  def noOfSticksTobaccoEditHelper(
    contextJourneyData: JourneyData,
    noOfSticks: Option[Int],
    productToken: String,
    iid: String
  ): Int = {

    val previousDeclarationTotalNoOfSticks: Int = //amend journey ignore
      contextJourneyData.declarationResponse
        .fold[List[PurchasedProductInstance]](List.empty)(_.oldPurchaseProductInstances)
        .filter(_.path.toString.contains(productToken))
        .map(_.noOfSticks.getOrElse(0))
        .sum

    val originalNoOfSticks: Int = originalAmountEnteredNoOfSticks(contextJourneyData, iid)

    val totalNoOfSticks: Int =
      contextJourneyData.purchasedProductInstances
        .filter(_.path.toString.contains(productToken))
        .map(_.noOfSticks.getOrElse(0))
        .sum

    println("noOfSticks " + noOfSticks)
    println("previously added amend jouney " + previousDeclarationTotalNoOfSticks)
    println("original no sticks " + originalNoOfSticks)
    println("total no sticks " + totalNoOfSticks)

    val totalNoOfSticksAfterCalc: Int =
      noOfSticks.getOrElse(0) + previousDeclarationTotalNoOfSticks + totalNoOfSticks - originalNoOfSticks

    println("totalNoOfSticksAfterCalc " + totalNoOfSticksAfterCalc)
    totalNoOfSticksAfterCalc
  }
}
