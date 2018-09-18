package services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import models._
import play.api.Mode.Mode
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Logger}
import services.http.WsAllMethods
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode


trait CalculatorServiceResponse
case object CalculatorServiceNoJourneyDataResponse extends CalculatorServiceResponse
case object CalculatorServiceCantBuildCalcReqResponse extends CalculatorServiceResponse
case class CalculatorServiceSuccessResponse(calculatorResponseDto: CalculatorResponseDto) extends CalculatorServiceResponse

case class CurrencyConversionRate(startDate: LocalDate, endDate: LocalDate, currencyCode: String, rate: Option[String])


@Singleton
class CalculatorService @Inject() (
  val localSessionCache: LocalSessionCache,
  wsAllMethods: WsAllMethods,
  configuration: Configuration,
  environment: Environment,
  productTreeService: ProductTreeService,
  currencyService: CurrencyService
) extends ServicesConfig with UsesJourneyData {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration = configuration

  lazy val currencyConversionBaseUrl = baseUrl("currency-conversion")
  lazy val passengersDutyCalculatorBaseUrl = baseUrl("passengers-duty-calculator")

  def todaysDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)

  def calculate()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CalculatorServiceResponse] = {

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


  def doCalculation(calculatorRequest: CalculatorRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CalculatorResponseDto] = {

    wsAllMethods.POST[CalculatorRequest, CalculatorResponse](s"$passengersDutyCalculatorBaseUrl/passengers-duty-calculator/calculate", calculatorRequest) map { calculatorResponse =>

      val alcoholItems = calculatorResponse.alcohol.map(_.bands.flatMap(b => b.items.map(i => (b.code, i)))).getOrElse(Nil)
      val tobaccoItems = calculatorResponse.tobacco.map(_.bands.flatMap(b => b.items.map(i => (b.code, i)))).getOrElse(Nil)
      val otherGoodsItems = calculatorResponse.otherGoods.map(_.bands.flatMap(b => b.items.map(i => (b.code, i)))).getOrElse(Nil)

      val bands = (alcoholItems ++ tobaccoItems ++ otherGoodsItems).groupBy(_._1).map { case (key, list) => (key, list.map( _._2 )) }

      CalculatorResponseDto(bands, calculatorResponse.calculation, calculatorRequest.hasOnlyGBP)
    }


  }

  def journeyDataToCalculatorRequest(journeyData: JourneyData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CalculatorRequest]] = {

    getCurrencyConversionRates(journeyData) map { ratesMap =>

      val purchasedItems: List[PurchasedItem] = for {
        purchasedProductInstance <- journeyData.purchasedProductInstances
        productTreeLeaf <- productTreeService.getProducts.getDescendant(purchasedProductInstance.path).collect { case p: ProductTreeLeaf => p }
        curCode <- purchasedProductInstance.currency
        currency <- currencyService.getCurrencyByCode(curCode)
        cost <- purchasedProductInstance.cost
        rate <- ratesMap.get(curCode)
      } yield PurchasedItem(purchasedProductInstance, productTreeLeaf, currency, (cost / rate).setScale(2, RoundingMode.DOWN))


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


  private def getCurrencyConversionRates(journeyData: JourneyData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Map[String, BigDecimal]] = {

    val allCurrencies: Set[Currency] = journeyData.allCurrencyCodes.map(currencyService.getCurrencyByCode).flatten


    implicit val formats = Json.reads[CurrencyConversionRate]

    val currenciesToFetch: Set[String] = allCurrencies.map(_.value).flatten

    val gbpEquivCurrencies: Map[String, BigDecimal] = allCurrencies.filterNot(_.value.isDefined).map(c => (c.code, BigDecimal(1))).toMap

    val queryString = currenciesToFetch.mkString("cc=", "&cc=", "")


    if (currenciesToFetch.isEmpty) {
      Future.successful(gbpEquivCurrencies)
    }
    else {
      wsAllMethods.GET[List[CurrencyConversionRate]](s"$currencyConversionBaseUrl/currency-conversion/rates/$todaysDate?$queryString") map { currencyConversionRates =>

        if (currencyConversionRates.exists(_.rate.isEmpty)) {
          Logger.error("Missing currency for " + currencyConversionRates.filter(_.rate.isEmpty).mkString(", "))

        }

        gbpEquivCurrencies ++ currencyConversionRates.map(ccr => ccr.rate.map(rate => (ccr.currencyCode, BigDecimal(rate)))).flatten.toMap

      }


    }

  }
}
