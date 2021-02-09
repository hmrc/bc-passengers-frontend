/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import connectors.Cache
import javax.inject.{Inject, Singleton}
import models._
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json, Reads}
import play.api.{Configuration, Environment, Logger}
import services.http.WsAllMethods
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

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
  configuration: Configuration,
  environment: Environment,
  productTreeService: ProductTreeService,
  currencyService: CurrencyService,
  servicesConfig: ServicesConfig,
  implicit val ec: ExecutionContext
) {

  lazy val currencyConversionBaseUrl: String = servicesConfig.baseUrl("currency-conversion")
  lazy val passengersDutyCalculatorBaseUrl: String = servicesConfig.baseUrl("passengers-duty-calculator")

  def todaysDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)

  def limitUsage(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[LimitUsageResponse] = {

    journeyDataToLimitsRequest(journeyData) match {

      case Some(limitRequest) =>
        wsAllMethods.POST[LimitRequest, JsObject](s"$passengersDutyCalculatorBaseUrl/passengers-duty-calculator/limits", limitRequest) map { r =>
          LimitUsageSuccessResponse(  (r \ "limits").as[Map[String,String]]  )
        }

      case None =>

        Logger.debug("No items available for limits request")
        Future.successful(LimitUsageCantBuildCalcReqResponse)
    }
  }

  def calculate(journeyData: JourneyData)(implicit hc: HeaderCarrier, messages: Messages): Future[CalculatorServiceResponse] = {

    journeyDataToCalculatorRequest(journeyData) flatMap {

      case Some(calculatorRequest) =>

            wsAllMethods.POST[CalculatorServiceRequest, CalculatorResponse](s"$passengersDutyCalculatorBaseUrl/passengers-duty-calculator/calculate", calculatorRequest) map { r =>
              CalculatorServiceSuccessResponse(r)
            } recover {
              case e: UpstreamErrorResponse if e.statusCode == REQUESTED_RANGE_NOT_SATISFIABLE =>
                CalculatorServicePurchasePriceOutOfBoundsFailureResponse
            }

      case None =>

        Logger.error("No items available for calculation request")
        Future.successful(CalculatorServiceCantBuildCalcReqResponse)
    }
  }


  def storeCalculatorResponse(journeyData: JourneyData, calculatorResponse: CalculatorResponse)(implicit hc: HeaderCarrier): Future[JourneyData] = {

    val updatedJourneyData = journeyData.copy(calculatorResponse = Some(calculatorResponse))

    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def journeyDataToLimitsRequest(journeyData: JourneyData)(implicit hc: HeaderCarrier): Option[LimitRequest] = {
      val speculativeItems: List[SpeculativeItem] = for {

        purchasedProductInstance <- journeyData.purchasedProductInstances
        productTreeLeaf <- productTreeService.productTree.getDescendant(purchasedProductInstance.path).collect { case p: ProductTreeLeaf => p }
      } yield SpeculativeItem(purchasedProductInstance, productTreeLeaf, 0)

      for {
        isAgeOver17 <- journeyData.ageOver17
        isPrivateCraft <- journeyData.privateCraft
        isArrivingNI <- journeyData.arrivingNICheck
      } yield LimitRequest(isPrivateCraft, isAgeOver17, isArrivingNI, speculativeItems)
  }

  def journeyDataToCalculatorRequest(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[Option[CalculatorServiceRequest]] = {

    getCurrencyConversionRates(journeyData) map { ratesMap =>

      val purchasedItems: List[PurchasedItem] = for {
        purchasedProductInstance <- journeyData.purchasedProductInstances
        productTreeLeaf <- productTreeService.productTree.getDescendant(purchasedProductInstance.path).collect { case p: ProductTreeLeaf => p }
        curCode <- purchasedProductInstance.currency
        currency <- currencyService.getCurrencyByCode(curCode)
        cost <- purchasedProductInstance.cost
        rate <- ratesMap.get(curCode)
      } yield PurchasedItem(purchasedProductInstance, productTreeLeaf, currency, (cost / rate).setScale(2, RoundingMode.DOWN), ExchangeRate(rate.toString, todaysDate))


      if(purchasedItems.isEmpty) {
        None
      }
      else {
        for {
          isAgeOver17 <- journeyData.ageOver17
          isPrivateCraft <- journeyData.privateCraft
          isArrivingNI <- journeyData.arrivingNICheck
        } yield CalculatorServiceRequest(
          isPrivateCraft,
          isAgeOver17,
          isArrivingNI,
          purchasedItems.filter(i => i.productTreeLeaf.isValid(i.purchasedProductInstance)))
      }
    }

  }


  private def getCurrencyConversionRates(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[Map[String, BigDecimal]] = {

    val allCurrencies: Set[Currency] = journeyData.allCurrencyCodes.flatMap(currencyService.getCurrencyByCode)


    implicit val formats: Reads[CurrencyConversionRate] = Json.reads[CurrencyConversionRate]

    val currenciesToFetch: Set[String] = allCurrencies.flatMap(_.valueForConversion)

    val gbpEquivCurrencies: Map[String, BigDecimal] = allCurrencies.filterNot(_.valueForConversion.isDefined).map(c => (c.code, BigDecimal("1.00"))).toMap

    val queryString = currenciesToFetch.mkString("cc=", "&cc=", "")


    if (currenciesToFetch.isEmpty) {
      Future.successful(gbpEquivCurrencies)
    }
    else {
      wsAllMethods.GET[List[CurrencyConversionRate]](s"$currencyConversionBaseUrl/currency-conversion/rates/$todaysDate?$queryString") map { currencyConversionRates =>

        if (currencyConversionRates.exists(_.rate.isEmpty)) {
          Logger.error("Missing currency for " + currencyConversionRates.filter(_.rate.isEmpty).mkString(", "))
        }

        gbpEquivCurrencies ++ currencyConversionRates.flatMap(ccr => ccr.rate.map(rate => (ccr.currencyCode, BigDecimal(rate)))).toMap
      }
    }
  }

}
