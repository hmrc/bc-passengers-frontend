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

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import connectors.Cache

import javax.inject.{Inject, Singleton}
import models._
import play.api.Logger
import play.api.http.Status._
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, Json, Reads}
import services.http.WsAllMethods
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

trait CalculatorServiceResponse
case object CalculatorServiceCantBuildCalcReqResponse extends CalculatorServiceResponse
case object CalculatorServicePurchasePriceOutOfBoundsFailureResponse extends CalculatorServiceResponse
case class CalculatorServiceSuccessResponse(calculatorResponse: CalculatorResponse) extends CalculatorServiceResponse

trait LimitUsageResponse
case object LimitUsageCantBuildCalcReqResponse extends LimitUsageResponse
case class LimitUsageSuccessResponse(limits: Map[String, String]) extends LimitUsageResponse

case class CurrencyConversionRate(startDate: LocalDate, endDate: LocalDate, currencyCode: String, rate: Option[String])

@Singleton
class CalculatorService @Inject() (
  val cache: Cache,
  wsAllMethods: WsAllMethods,
  productTreeService: ProductTreeService,
  currencyService: CurrencyService,
  servicesConfig: ServicesConfig,
  implicit val ec: ExecutionContext
) {

  private lazy val currencyConversionBaseUrl: String       = servicesConfig.baseUrl("currency-conversion")
  private lazy val passengersDutyCalculatorBaseUrl: String = servicesConfig.baseUrl("passengers-duty-calculator")

  private val logger = Logger(this.getClass)

  def todaysDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)

  def limitUsage(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[LimitUsageResponse] =
    journeyDataToLimitsRequest(journeyData) match {

      case Some(limitRequest) =>
        wsAllMethods.POST[LimitRequest, JsObject](
          s"$passengersDutyCalculatorBaseUrl/passengers-duty-calculator/limits",
          limitRequest
        ) map { r =>
          LimitUsageSuccessResponse((r \ "limits").as[Map[String, String]])
        }

      case None =>
        logger.error("No items available for limits request")
        Future.successful(LimitUsageCantBuildCalcReqResponse)
    }

  def calculate(
    journeyData: JourneyData
  )(implicit hc: HeaderCarrier, messages: MessagesApi): Future[CalculatorServiceResponse] = {

    val allPurchasedProductInstances = journeyData.declarationResponse
      .map(_.oldPurchaseProductInstances)
      .getOrElse(Nil) ++ journeyData.purchasedProductInstances

    journeyDataToCalculatorRequest(journeyData, allPurchasedProductInstances) flatMap {

      case Some(calculatorRequest) =>
        wsAllMethods.POST[CalculatorServiceRequest, CalculatorResponse](
          s"$passengersDutyCalculatorBaseUrl/passengers-duty-calculator/calculate",
          calculatorRequest
        ) map { r =>
          CalculatorServiceSuccessResponse(r)
        } recover {
          case e: UpstreamErrorResponse if e.statusCode == REQUESTED_RANGE_NOT_SATISFIABLE =>
            CalculatorServicePurchasePriceOutOfBoundsFailureResponse
        }

      case None =>
        logger.error("No items available for calculation request")
        Future.successful(CalculatorServiceCantBuildCalcReqResponse)
    }
  }

  def storeCalculatorResponse(
    journeyData: JourneyData,
    calculatorResponse: CalculatorResponse,
    deltaCalc: Option[Calculation] = None
  )(implicit hc: HeaderCarrier): Future[JourneyData] = {

    val updatedJourneyData =
      journeyData.copy(calculatorResponse = Some(calculatorResponse), deltaCalculation = deltaCalc)

    cache.store(updatedJourneyData).map(_ => updatedJourneyData)
  }

  def getDeltaCalculation(oldCalcObj: Calculation, currentCalculation: Calculation): Calculation = {
    val deltaCustoms = (BigDecimal(currentCalculation.customs) - BigDecimal(oldCalcObj.customs)).setScale(2).toString
    val deltaVat     = (BigDecimal(currentCalculation.vat) - BigDecimal(oldCalcObj.vat)).setScale(2).toString
    val deltaExcise  = (BigDecimal(currentCalculation.excise) - BigDecimal(oldCalcObj.excise)).setScale(2).toString
    val deltaTotal   = (BigDecimal(currentCalculation.allTax) - BigDecimal(oldCalcObj.allTax)).setScale(2).toString
    Calculation(deltaExcise, deltaCustoms, deltaVat, deltaTotal)
  }

  def getPreviousPaidCalculation(deltaCalculation: Calculation, currentCalculation: Calculation): Calculation = {
    val previousCustoms =
      (BigDecimal(currentCalculation.customs) - BigDecimal(deltaCalculation.customs)).setScale(2).toString
    val previousVat     = (BigDecimal(currentCalculation.vat) - BigDecimal(deltaCalculation.vat)).setScale(2).toString
    val previousExcise  =
      (BigDecimal(currentCalculation.excise) - BigDecimal(deltaCalculation.excise)).setScale(2).toString
    val previousTotal   =
      (BigDecimal(currentCalculation.allTax) - BigDecimal(deltaCalculation.allTax)).setScale(2).toString
    Calculation(previousExcise, previousCustoms, previousVat, previousTotal)
  }

  def journeyDataToLimitsRequest(journeyData: JourneyData): Option[LimitRequest] = {
    val allPurchasedProductInstances = journeyData.declarationResponse
      .map(_.oldPurchaseProductInstances)
      .getOrElse(Nil) ++ journeyData.purchasedProductInstances

    val speculativeItems: List[SpeculativeItem] = for {

      purchasedProductInstance <- allPurchasedProductInstances
      productTreeLeaf          <- productTreeService.productTree.getDescendant(purchasedProductInstance.path).collect {
                                    case p: ProductTreeLeaf => p
                                  }
    } yield SpeculativeItem(purchasedProductInstance, productTreeLeaf, 0)

    for {
      isAgeOver17    <- journeyData.ageOver17
      isPrivateCraft <- journeyData.privateCraft
      isArrivingNI   <- journeyData.arrivingNICheck
    } yield LimitRequest(isPrivateCraft, isAgeOver17, isArrivingNI, speculativeItems)
  }

  def journeyDataToCalculatorRequest(
    journeyData: JourneyData,
    purchasedProductInstance: List[PurchasedProductInstance]
  )(implicit hc: HeaderCarrier): Future[Option[CalculatorServiceRequest]] =
    getCurrencyConversionRates(journeyData, purchasedProductInstance) map { ratesMap =>
      val purchasedItems: List[PurchasedItem] = for {
        purchasedProductInstance <- purchasedProductInstance
        productTreeLeaf          <- productTreeService.productTree.getDescendant(purchasedProductInstance.path).collect {
                                      case p: ProductTreeLeaf => p
                                    }
        curCode                  <- purchasedProductInstance.currency
        currency                 <- currencyService.getCurrencyByCode(curCode)
        cost                     <- purchasedProductInstance.cost
        rate                     <- ratesMap.get(curCode)
      } yield PurchasedItem(
        purchasedProductInstance,
        productTreeLeaf,
        currency,
        (cost / rate).setScale(2, RoundingMode.DOWN),
        ExchangeRate(rate.toString, todaysDate)
      )

      if (purchasedItems.isEmpty) {
        None
      } else {
        for {
          isAgeOver17    <- journeyData.ageOver17
          isPrivateCraft <- journeyData.privateCraft
          isArrivingNI   <- journeyData.arrivingNICheck
        } yield CalculatorServiceRequest(
          isPrivateCraft,
          isAgeOver17,
          isArrivingNI,
          purchasedItems.filter(i => i.productTreeLeaf.isValid(i.purchasedProductInstance))
        )
      }
    }

  private def getCurrencyConversionRates(
    journeyData: JourneyData,
    purchasedProductInstance: List[PurchasedProductInstance]
  )(implicit hc: HeaderCarrier): Future[Map[String, BigDecimal]] = {

    val allCurrencies: Set[Currency] =
      journeyData.allCurrencyCodes(purchasedProductInstance).flatMap(currencyService.getCurrencyByCode)

    implicit val formats: Reads[CurrencyConversionRate] = Json.reads[CurrencyConversionRate]

    val currenciesToFetch: Set[String] = allCurrencies.flatMap(_.valueForConversion)

    val gbpEquivCurrencies: Map[String, BigDecimal] =
      allCurrencies.filterNot(_.valueForConversion.isDefined).map(c => (c.code, BigDecimal("1.00"))).toMap

    val queryString = currenciesToFetch.mkString("cc=", "&cc=", "")

    if (currenciesToFetch.isEmpty) {
      Future.successful(gbpEquivCurrencies)
    } else {
      wsAllMethods.GET[List[CurrencyConversionRate]](
        s"$currencyConversionBaseUrl/currency-conversion/rates/$todaysDate?$queryString"
      ) map { currencyConversionRates =>
        if (currencyConversionRates.exists(_.rate.isEmpty)) {
          logger.error("Missing currency for " + currencyConversionRates.filter(_.rate.isEmpty).mkString(", "))
        }

        gbpEquivCurrencies ++ currencyConversionRates
          .flatMap(ccr => ccr.rate.map(rate => (ccr.currencyCode, BigDecimal(rate))))
          .toMap
      }
    }
  }

}
