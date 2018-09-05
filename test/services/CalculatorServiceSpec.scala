package services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import models._
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.http.WsAllMethods
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import util.BaseSpec

import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class CalculatorServiceSpec extends BaseSpec {

  override lazy val app = GuiceApplicationBuilder()
    .overrides(bind[WsAllMethods].toInstance(MockitoSugar.mock[WsAllMethods]))
    .overrides(bind[LocalSessionCache].toInstance(MockitoSugar.mock[LocalSessionCache]))
    .configure(
      "microservice.services.currency-conversion.host" -> "currency-conversion.service",
      "microservice.services.currency-conversion.port" -> "80",
      "microservice.services.passengers-duty-calculator.host" -> "passengers-duty-calculator.service",
      "microservice.services.passengers-duty-calculator.port" -> "80"
    )
    .build()

  override def beforeEach(): Unit = {
    reset(app.injector.instanceOf[WsAllMethods])
    reset(app.injector.instanceOf[LocalSessionCache])
    super.beforeEach()
  }

  def todaysDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)

  "Calling CalculatorService.journeyDataToCalculatorRequest" should {

    val missingRateJourneyData = JourneyData(
      Some("Andorra"),
      Some(true),
      Some(false),
      Nil,
      List(
        PurchasedProduct(Some(ProductPath("alcohol/beer")),Some(1),List(PurchasedProductInstance(0,Some(12),None,Some("USD"),Some(123))))
      )
    )

    val goodJourneyData = JourneyData(
      Some("Andorra"),
      Some(true),
      Some(false),
      Nil,
      List(
        PurchasedProduct(Some(ProductPath("other-goods/car-seats")),Some(1),List(PurchasedProductInstance(0,None,None,Some("AUD"),Some(74563)))),
        PurchasedProduct(Some(ProductPath("other-goods/antiques")),Some(2),List(PurchasedProductInstance(0,None,None,Some("AUD"),Some(33)), PurchasedProductInstance(1,None,None,Some("CHF"),Some(5432)))),
        PurchasedProduct(Some(ProductPath("tobacco/chewing")),Some(1),List(PurchasedProductInstance(0,Some(45),None,Some("CHF"),Some(43)))),
        PurchasedProduct(Some(ProductPath("tobacco/cigars")),Some(1),List(PurchasedProductInstance(0,Some(40),Some(20),Some("AUD"),Some(1234)))),
        PurchasedProduct(Some(ProductPath("tobacco/cigarettes")),Some(1),List(PurchasedProductInstance(0,None,Some(200),Some("GBP"),Some(60)))),
        PurchasedProduct(Some(ProductPath("alcohol/beer")),Some(1),List(PurchasedProductInstance(0,Some(12),None,Some("GGP"),Some(123))))
      )
    )

    val imperfectJourneyData = JourneyData(
      Some("Andorra"),
      Some(true),
      Some(false),
      Nil,
      List(
        PurchasedProduct(Some(ProductPath("other-goods/car-seats")),Some(1),List(PurchasedProductInstance(0,None,None,Some("AUD"),Some(74563)))),
        PurchasedProduct(Some(ProductPath("other-goods/antiques")),Some(2),List(PurchasedProductInstance(0,None,None,Some("AUD"),Some(33)), PurchasedProductInstance(1,None,None,Some("CHF"),Some(5432)))),
        PurchasedProduct(Some(ProductPath("tobacco/chewing")),Some(1),List(PurchasedProductInstance(0,Some(45),None,Some("CHF"),Some(43)))),
        PurchasedProduct(Some(ProductPath("tobacco/cigars")),Some(1),List(PurchasedProductInstance(0,weightOrVolume = None,Some(20),Some("AUD"),Some(1234)))), //Note weightOrVolume = None
        PurchasedProduct(Some(ProductPath("tobacco/cigarettes")),Some(1),List(PurchasedProductInstance(0,None,Some(200),Some("GBP"),Some(60)))),
        PurchasedProduct(Some(ProductPath("alcohol/beer")),Some(1),List(PurchasedProductInstance(0,Some(12),None,Some("GGP"),Some(123))))
      )
    )


    val calcRequest = CalculatorRequest(false,true,List(
      PurchasedItem(PurchasedProductInstance(0,None,None,Some("AUD"),Some(74563)),ProductTreeLeaf("car-seats","Children’s car seats","OGD/MOB/MISC","other-goods"),Currency("AUD","Australia Dollar (AUD)",Some("AUD")), BigDecimal(74563/1.76).setScale(2, RoundingMode.DOWN)),
      PurchasedItem(PurchasedProductInstance(0,None,None,Some("AUD"),Some(33)),ProductTreeLeaf("antiques","Antiques and works of art","OGD/ART","other-goods"),Currency("AUD","Australia Dollar (AUD)",Some("AUD")), BigDecimal(33/1.76).setScale(2, RoundingMode.DOWN)),
      PurchasedItem(PurchasedProductInstance(1,None,None,Some("CHF"),Some(5432)),ProductTreeLeaf("antiques","Antiques and works of art","OGD/ART","other-goods"),Currency("CHF","Switzerland Franc (CHF)",Some("CHF")), BigDecimal(5432/1.26).setScale(2, RoundingMode.DOWN)),
      PurchasedItem(PurchasedProductInstance(0,Some(45),None,Some("CHF"),Some(43)),ProductTreeLeaf("chewing","Chewing or pipe tobacco","TOB/A1/OTHER","tobacco"),Currency("CHF","Switzerland Franc (CHF)",Some("CHF")), BigDecimal(43/1.26).setScale(2, RoundingMode.DOWN)),
      PurchasedItem(PurchasedProductInstance(0,Some(40),Some(20),Some("AUD"),Some(1234)),ProductTreeLeaf("cigars","Cigars","TOB/A1/CIGAR","cigars"),Currency("AUD","Australia Dollar (AUD)",Some("AUD")), BigDecimal(1234/1.76).setScale(2, RoundingMode.DOWN)),
      PurchasedItem(PurchasedProductInstance(0,None,Some(200),Some("GBP"),Some(60)),ProductTreeLeaf("cigarettes","Cigarettes","TOB/A1/CIGRT","cigarettes"),Currency("GBP","British Pound (GBP)",None), BigDecimal(60).setScale(2, RoundingMode.DOWN)),
      PurchasedItem(PurchasedProductInstance(0,Some(12),None,Some("GGP"),Some(123)),ProductTreeLeaf("beer","Beer","ALC/A2/BEER","alcohol"),Currency("GGP","Guernsey Pound (GGP)",None), BigDecimal(123).setScale(2, RoundingMode.DOWN))
    ))


    trait LocalSetup {

      lazy val service = {

        when(injected[WsAllMethods].GET[List[CurrencyConversionRate]](meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=USD"))(any(),any(),any())) thenReturn {
          Future.successful(List(
            CurrencyConversionRate(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31"), "USD", None)
          ))
        }

        when(injected[WsAllMethods].GET[List[CurrencyConversionRate]](any())(any(),any(),any())) thenReturn {
          Future.successful(List(
            CurrencyConversionRate(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31"), "AUD", Some("1.76")),
            CurrencyConversionRate(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31"), "CHF", Some("1.26"))
          ))
        }

        injected[CalculatorService]
      }
    }

    "return None if there was a missing rate, making a call to the currency-conversion service" in new LocalSetup {

      val response = await(service.journeyDataToCalculatorRequest(missingRateJourneyData))

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=USD"))(any(),any(),any())

      response shouldBe None
    }

    "skip invalid instances (instances with missing required data)" in new LocalSetup {

      val response: CalculatorRequest = await(service.journeyDataToCalculatorRequest(imperfectJourneyData)).get

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"))(any(),any(),any())

      response shouldBe calcRequest.copy(items = calcRequest.items.filterNot(_.productTreeLeaf.token=="cigars"))
    }

    "transform journey data to a calculator request, making a call to the currency-conversion service" in new LocalSetup {

      val response: CalculatorRequest = await(service.journeyDataToCalculatorRequest(goodJourneyData)).get

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"))(any(),any(),any())

      response shouldBe calcRequest
    }
  }

  "Calling CalculatorService.calculate" should {

    trait LocalSetup {

      def cachedJourneyData: Option[JourneyData]

      lazy val service = {

        when(injected[LocalSessionCache].fetchAndGetJourneyData(any())) thenReturn {
          Future.successful(cachedJourneyData)
        }

        when(injected[WsAllMethods].GET[List[CurrencyConversionRate]](meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=CAD&cc=USD"))(any(),any(),any())) thenReturn {
          Future.successful(List(
            CurrencyConversionRate(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31"), "USD", Some("1.4534")),
            CurrencyConversionRate(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31"), "CAD", Some("1.7654"))
          ))
        }

        when(injected[WsAllMethods].POST[CalculatorRequest, CalculatorResponse](meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"), any(), any())(any(),any(),any(),any())) thenReturn {
          Future.successful(CalculatorResponse(
            Some(Alcohol(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
            Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
            Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
            Calculation("0.00", "0.00", "0.00", "0.00")
          ))
        }

        injected[CalculatorService]
      }
    }

    "make a call to the currency-conversion service, the calculator service and return a valid response" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        country = Some("United States"),
        ageOver17 = Some(true),
        privateCraft = Some(false),
        purchasedProducts = List(
          PurchasedProduct(
            path = Some(ProductPath("other-goods/antiques")),
            quantity = Some(1),
            purchasedProductInstances = List(PurchasedProductInstance(index = 0, currency = Some("CAD"), cost = Some(BigDecimal("2.00"))))
          ),
          PurchasedProduct(
            path = Some(ProductPath("tobacco/cigars")),
            quantity = Some(2),
            purchasedProductInstances = List(PurchasedProductInstance(index = 0, currency = Some("USD"), cost = Some(BigDecimal("4.00"))))
          )
        ))
      )

      val response: CalculatorServiceResponse = await(service.calculate())

      response.asInstanceOf[CalculatorServiceSuccessResponse].calculatorResponseDto shouldBe CalculatorResponseDto(Map(), Calculation("0.00", "0.00", "0.00", "0.00"), hasOnlyGBP = false)

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())

      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=CAD&cc=USD"))(any(),any(),any())

      verify(injected[WsAllMethods], times(1)).POST[CalculatorRequest, CalculatorResponse](
        meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"),
        meq(CalculatorRequest(false, true, List(
          PurchasedItem(PurchasedProductInstance(0,None,None,Some("CAD"),Some(BigDecimal("2.00"))),ProductTreeLeaf("antiques","Antiques and works of art","OGD/ART","other-goods"),Currency("CAD","Canada Dollar (CAD)",Some("CAD")),BigDecimal("1.13"))
        ))),
        any())(any(),any(),any(),any())


    }
  }
}
