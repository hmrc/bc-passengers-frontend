package services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import models._
import play.api.Mode.Mode
import play.api.libs.json.{Json, Reads}
import play.api.{Configuration, Environment, Logger}
import services.http.WsAllMethods
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode


trait CalculatorServiceResponse
case object CalculatorServiceNoJourneyDataResponse extends CalculatorServiceResponse
case object CalculatorServiceCantBuildCalcReqResponse extends CalculatorServiceResponse
case class CalculatorServiceSuccessResponse(calculatorResponse: CalculatorResponse) extends CalculatorServiceResponse

case class CurrencyConversionRate(startDate: LocalDate, endDate: LocalDate, currencyCode: String, rate: Option[String])


@Singleton
class CalculatorService @Inject() (
  val localSessionCache: LocalSessionCache,
  wsAllMethods: WsAllMethods,
  configuration: Configuration,
  environment: Environment,
  productTreeService: ProductTreeService,
  currencyService: CurrencyService,
  implicit val ec: ExecutionContext
) extends ServicesConfig with UsesJourneyData {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration: Configuration = configuration

  lazy val currencyConversionBaseUrl: String = baseUrl("currency-conversion")
  lazy val passengersDutyCalculatorBaseUrl: String = baseUrl("passengers-duty-calculator")

  def todaysDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)

  def calculate()(implicit hc: HeaderCarrier): Future[CalculatorServiceResponse] = {

    getJourneyData flatMap {

      case Some(journeyData) =>

        journeyDataToCalculatorRequest(journeyData) flatMap {
          case Some(calculatorRequest) =>

            doCalculation(calculatorRequest) map { r =>
              CalculatorServiceSuccessResponse(r)
            }

          case None =>
            Logger.error("No items available for calculation request")
            Future.successful(CalculatorServiceCantBuildCalcReqResponse)
        }

      case None =>
        Future.successful(CalculatorServiceNoJourneyDataResponse)
    }
  }


  def doCalculation(calculatorRequest: CalculatorRequest)(implicit hc: HeaderCarrier): Future[CalculatorResponse] = {

    wsAllMethods.POST[CalculatorRequest, CalculatorResponse](s"$passengersDutyCalculatorBaseUrl/passengers-duty-calculator/calculate", calculatorRequest) map { calculatorResponse =>

      calculatorResponse
    }
  }

  def storeCalculatorResponse(journeyData: JourneyData, calculatorResponse: CalculatorResponse)(implicit hc: HeaderCarrier): Future[JourneyData] = {

    val updatedJourneyData = journeyData.copy(calculatorResponse = Some(calculatorResponse))

    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def journeyDataToCalculatorRequest(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[Option[CalculatorRequest]] = {

    getCurrencyConversionRates(journeyData) map { ratesMap =>

      val purchasedItems: List[PurchasedItem] = for {
        purchasedProductInstance <- journeyData.purchasedProductInstances
        productTreeLeaf <- productTreeService.getProducts.getDescendant(purchasedProductInstance.path).collect { case p: ProductTreeLeaf => p }
        curCode <- purchasedProductInstance.currency
        currency <- currencyService.getCurrencyByCode(curCode)
        cost <- purchasedProductInstance.cost
        country <- purchasedProductInstance.country
        rate <- ratesMap.get(curCode)
      } yield PurchasedItem(purchasedProductInstance, productTreeLeaf, currency, (cost / rate).setScale(2, RoundingMode.DOWN), ExchangeRate(rate.toString, todaysDate))


      if(purchasedItems.isEmpty) {
        None
      }
      else {
        for {
          isAgeOver17 <- journeyData.ageOver17
          isPrivateCraft <- journeyData.privateCraft
        } yield CalculatorRequest(isPrivateCraft, isAgeOver17, purchasedItems.filter(i => i.productTreeLeaf.isValid(i.purchasedProductInstance)))
      }
    }

  }


  private def getCurrencyConversionRates(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[Map[String, BigDecimal]] = {

    val allCurrencies: Set[Currency] = journeyData.allCurrencyCodes.flatMap(currencyService.getCurrencyByCode)


    implicit val formats: Reads[CurrencyConversionRate] = Json.reads[CurrencyConversionRate]

    val currenciesToFetch: Set[String] = allCurrencies.flatMap(_.value)

    val gbpEquivCurrencies: Map[String, BigDecimal] = allCurrencies.filterNot(_.value.isDefined).map(c => (c.code, BigDecimal("1.00"))).toMap

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
