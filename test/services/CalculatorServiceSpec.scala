package services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import connectors.Cache
import models._
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.Helpers._
import services.http.WsAllMethods
import uk.gov.hmrc.http.{HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.http.cache.client.CacheMap
import util.BaseSpec

import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class CalculatorServiceSpec extends BaseSpec {

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[WsAllMethods].toInstance(MockitoSugar.mock[WsAllMethods]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .configure(
      "microservice.services.currency-conversion.host" -> "currency-conversion.service",
      "microservice.services.currency-conversion.port" -> "80",
      "microservice.services.passengers-duty-calculator.host" -> "passengers-duty-calculator.service",
      "microservice.services.passengers-duty-calculator.port" -> "80"
    )
    .build()

  override def beforeEach(): Unit = {
    reset(app.injector.instanceOf[WsAllMethods])
    reset(app.injector.instanceOf[Cache])
    super.beforeEach()
  }

  def todaysDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)

  "Calling CalculatorService.journeyDataToCalculatorRequest" should {

    val missingRateJourneyData = JourneyData(
      Some("nonEuOnly"),
      None,
      None,
      None,
      Some(false),
      Some(true),
      Nil,
      List(PurchasedProductInstance(ProductPath("alcohol/beer"),"iid0",Some(12),None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("USD"),Some(123)))
    )

    val goodJourneyData = JourneyData(
      Some("nonEuOnly"),
      None,
      None,
      None,
      Some(false),
      Some(true),
      Nil,
      List(
        PurchasedProductInstance(ProductPath("other-goods/car-seats"),"iid0",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("AUD"),Some(74563)),
        PurchasedProductInstance(ProductPath("other-goods/antiques"),"iid0",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("AUD"),Some(33)),
        PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("CHF"),Some(5432)),
        PurchasedProductInstance(ProductPath("tobacco/chewing-tobacco"),"iid0",Some(45),None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("CHF"),Some(43)),
        PurchasedProductInstance(ProductPath("tobacco/cigars"),"iid0",Some(40),Some(20),Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("AUD"),Some(1234)),
        PurchasedProductInstance(ProductPath("tobacco/cigarettes"),"iid0",None,Some(200),Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("GBP"),Some(60)),
        PurchasedProductInstance(ProductPath("alcohol/beer"),"iid0",Some(12),None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("GGP"),Some(123))
      )
    )

    val imperfectJourneyData = JourneyData(
      Some("nonEuOnly"),
      None,
      None,
      None,
      Some(false),
      Some(true),
      Nil,
      List(
        PurchasedProductInstance(ProductPath("other-goods/car-seats"),"iid0",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("AUD"),Some(74563)),
        PurchasedProductInstance(ProductPath("other-goods/antiques"),"iid0",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("AUD"),Some(33)),
        PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("CHF"),Some(5432)),
        PurchasedProductInstance(ProductPath("tobacco/chewing-tobacco"),"iid0",Some(45),None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("CHF"),Some(43)),
        PurchasedProductInstance(ProductPath("tobacco/cigars"), "iid0",weightOrVolume = None,Some(20),Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("AUD"),Some(1234)), //Note weightOrVolume = None
        PurchasedProductInstance(ProductPath("tobacco/cigarettes"),"iid0",None,Some(200),Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("GBP"),Some(60)),
        PurchasedProductInstance(ProductPath("alcohol/beer"),"iid0",Some(12),None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("GGP"),Some(123))
      )
    )


    val calcRequest = CalculatorRequest(isPrivateCraft = false, isAgeOver17 = true, isVatResClaimed = None, List(
      PurchasedItem(PurchasedProductInstance(ProductPath("other-goods/car-seats"),"iid0",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("AUD"),Some(74563)),ProductTreeLeaf("car-seats","label.other-goods.car-seats","OGD/MOB/MISC","other-goods", Nil),Currency("AUD","title.australian_dollars_aud",Some("AUD"), List("Australian", "Oz")), BigDecimal(74563/1.76).setScale(2, RoundingMode.DOWN), ExchangeRate("1.76", todaysDate)),
      PurchasedItem(PurchasedProductInstance(ProductPath("other-goods/antiques"),"iid0",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("AUD"),Some(33)),ProductTreeLeaf("antiques","label.other-goods.antiques","OGD/ART","other-goods", Nil),Currency("AUD","title.australian_dollars_aud",Some("AUD"), List("Australian", "Oz")), BigDecimal(33/1.76).setScale(2, RoundingMode.DOWN), ExchangeRate("1.76", todaysDate)),
      PurchasedItem(PurchasedProductInstance(ProductPath("other-goods/antiques"),"iid1",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("CHF"),Some(5432)),ProductTreeLeaf("antiques","label.other-goods.antiques","OGD/ART","other-goods", Nil),Currency("CHF","title.swiss_francs_chf",Some("CHF"), List("Swiss", "Switzerland")), BigDecimal(5432/1.26).setScale(2, RoundingMode.DOWN), ExchangeRate("1.26", todaysDate)),
      PurchasedItem(PurchasedProductInstance(ProductPath("tobacco/chewing-tobacco"),"iid0",Some(45),None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("CHF"),Some(43)),ProductTreeLeaf("chewing-tobacco","label.tobacco.chewing-tobacco","TOB/A1/OTHER","tobacco", List("L-LOOSE")),Currency("CHF","title.swiss_francs_chf",Some("CHF"), List("Swiss", "Switzerland")), BigDecimal(43/1.26).setScale(2, RoundingMode.DOWN), ExchangeRate("1.26", todaysDate)),
      PurchasedItem(PurchasedProductInstance(ProductPath("tobacco/cigars"),"iid0",Some(40),Some(20),Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("AUD"),Some(1234)),ProductTreeLeaf("cigars","label.tobacco.cigars","TOB/A1/CIGAR","cigars", List("L-CIGAR")),Currency("AUD","title.australian_dollars_aud",Some("AUD"), List("Australian", "Oz")), BigDecimal(1234/1.76).setScale(2, RoundingMode.DOWN), ExchangeRate("1.76", todaysDate)),
      PurchasedItem(PurchasedProductInstance(ProductPath("tobacco/cigarettes"),"iid0",None,Some(200),Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("GBP"),Some(60)),ProductTreeLeaf("cigarettes","label.tobacco.cigarettes","TOB/A1/CIGRT","cigarettes", List("L-CIGRT")),Currency("GBP","title.british_pounds_gbp",None, List("England", "Scotland", "Wales", "Northern Ireland", "British", "sterling", "pound", "GB")), BigDecimal(60).setScale(2, RoundingMode.DOWN), ExchangeRate("1.00", todaysDate)),
      PurchasedItem(PurchasedProductInstance(ProductPath("alcohol/beer"),"iid0",Some(12),None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("GGP"),Some(123)),ProductTreeLeaf("beer","label.alcohol.beer","ALC/A2/BEER","alcohol", List("L-BEER")),Currency("GGP","title.guernsey_pounds_ggp",None, List("Channel Islands")), BigDecimal(123).setScale(2, RoundingMode.DOWN), ExchangeRate("1.00", todaysDate))
    ))

    trait LocalSetup {

      lazy val service: CalculatorService = {

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

      val response: Option[CalculatorRequest] = await(service.journeyDataToCalculatorRequest(missingRateJourneyData))

      verify(injected[Cache], times(0)).fetch(any())
      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=USD"))(any(),any(),any())

      response shouldBe None
    }

    "skip invalid instances (instances with missing required data)" in new LocalSetup {

      val response: CalculatorRequest = await(service.journeyDataToCalculatorRequest(imperfectJourneyData)).get

      verify(injected[Cache], times(0)).fetch(any())
      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"))(any(),any(),any())

      response shouldBe calcRequest.copy(items = calcRequest.items.filterNot(_.productTreeLeaf.token=="cigars"))
    }

    "transform journey data to a calculator request, making a call to the currency-conversion service" in new LocalSetup {

      val response: CalculatorRequest = await(service.journeyDataToCalculatorRequest(goodJourneyData)).get

      verify(injected[Cache], times(0)).fetch(any())
      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"))(any(),any(),any())

      response shouldBe calcRequest
    }

    "transform journey data with vat res = Some(true) to a calculator request with the vat res parameter included as true" in new LocalSetup {

      val response: CalculatorRequest = await(service.journeyDataToCalculatorRequest(goodJourneyData.copy(isVatResClaimed = Some(true)))).get

      verify(injected[Cache], times(0)).fetch(any())
      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"))(any(),any(),any())

      response shouldBe calcRequest.copy(isVatResClaimed = Some(true))
    }

    "transform journey data with vat res = Some(false) to a calculator request with the vat res parameter included as false" in new LocalSetup {

      val response: CalculatorRequest = await(service.journeyDataToCalculatorRequest(goodJourneyData.copy(isVatResClaimed = Some(false)))).get

      verify(injected[Cache], times(0)).fetch(any())
      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"))(any(),any(),any())

      response shouldBe calcRequest.copy(isVatResClaimed = Some(false))
    }

    "transform journey data with vat res = None to a calculator request with the vat res parameter not included" in new LocalSetup {

      val response: CalculatorRequest = await(service.journeyDataToCalculatorRequest(goodJourneyData.copy(isVatResClaimed = None))).get

      verify(injected[Cache], times(0)).fetch(any())
      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"))(any(),any(),any())

      response shouldBe calcRequest.copy(isVatResClaimed = None)
    }
  }

  "Calling CalculatorService.calculate" should {

    trait LocalSetup {

      def cachedJourneyData: Option[JourneyData]

      def simulatePurchasePriceOutOfBounds: Boolean

      lazy val service = {

        when(injected[Cache].fetch(any())) thenReturn {
          Future.successful(cachedJourneyData)
        }

        when(injected[WsAllMethods].GET[List[CurrencyConversionRate]](meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=CAD&cc=USD"))(any(),any(),any())) thenReturn {
          Future.successful(List(
            CurrencyConversionRate(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31"), "USD", Some("1.4534")),
            CurrencyConversionRate(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31"), "CAD", Some("1.7654"))
          ))
        }

        if(simulatePurchasePriceOutOfBounds) {
          when(injected[WsAllMethods].POST[CalculatorRequest, CalculatorResponse](meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"), any(), any())
            (any(), any(), any(), any())) thenReturn Future.failed(new Upstream4xxResponse("Any message", REQUESTED_RANGE_NOT_SATISFIABLE, REQUESTED_RANGE_NOT_SATISFIABLE, Map.empty))
        }
        else {
          when(injected[WsAllMethods].POST[CalculatorRequest, CalculatorResponse](meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"), any(), any())
            (any(), any(), any(), any())) thenReturn {
            Future.successful(CalculatorResponse(
              Some(Alcohol(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
              Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
              Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
              Calculation("0.00", "0.00", "0.00", "0.00"),
              withinFreeAllowance = false,
              limits = Map.empty
            ))
          }
        }

        injected[CalculatorService]
      }
    }

    "make a call to the currency-conversion service, the calculator service and return a valid response" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        euCountryCheck = Some("nonEuOnly"),
        ageOver17 = Some(true),
        privateCraft = Some(false),
        purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("other-goods/antiques"), iid = "iid0", country = Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), currency = Some("CAD"), cost = Some(BigDecimal("2.00"))),
          PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid1", country = Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), currency = Some("USD"), cost = Some(BigDecimal("4.00")))
        )
      ))

      override lazy val simulatePurchasePriceOutOfBounds = false

      val messages: Messages = injected[MessagesApi].preferred(EnhancedFakeRequest("POST", "/nowhere")(app))

      val response: CalculatorServiceResponse = await(service.calculate()(implicitly, messages))

      response.asInstanceOf[CalculatorServiceSuccessResponse].calculatorResponse shouldBe
        CalculatorResponse(
          Some(Alcohol(List(),Calculation("0.00","0.00","0.00","0.00"))),
          Some(Tobacco(List(),Calculation("0.00","0.00","0.00","0.00"))),
          Some(OtherGoods(List(),Calculation("0.00","0.00","0.00","0.00"))),
          Calculation("0.00","0.00","0.00","0.00"),
          withinFreeAllowance = false,
          limits = Map.empty
        )

      verify(injected[Cache], times(1)).fetch(any())

      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=CAD&cc=USD"))(any(),any(),any())

      verify(injected[WsAllMethods], times(1)).POST[CalculatorRequest, CalculatorResponse](
        meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"),
        meq(CalculatorRequest(isPrivateCraft = false, isAgeOver17 = true, isVatResClaimed = None, List(
          PurchasedItem(PurchasedProductInstance(ProductPath("other-goods/antiques"),"iid0",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("CAD"),Some(BigDecimal("2.00"))),ProductTreeLeaf("antiques","label.other-goods.antiques","OGD/ART","other-goods", Nil),Currency("CAD","title.canadian_dollars_cad",Some("CAD"), Nil), BigDecimal("1.13"), ExchangeRate("1.7654", todaysDate))
        ))),
        any())(any(),any(),any(),any())


    }

    "make a call to the currency-conversion service, the calculator service and return CalculatorServicePurchasePriceOutOfBoundsFailureResponse when call to calculator returns 416 REQUESTED_RANGE_NOT_SATISFIABLE" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        euCountryCheck = Some("nonEuOnly"),
        ageOver17 = Some(true),
        privateCraft = Some(false),
        purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("other-goods/antiques"), iid = "iid0", country = Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), currency = Some("CAD"), cost = Some(BigDecimal("2.00"))),
          PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid1", country = Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), currency = Some("USD"), cost = Some(BigDecimal("4.00")))
        )
      ))

      override lazy val simulatePurchasePriceOutOfBounds = true

      val messages: Messages = injected[MessagesApi].preferred(EnhancedFakeRequest("POST", "/nowhere")(app))

      await(service.calculate()(implicitly, messages)) shouldBe CalculatorServicePurchasePriceOutOfBoundsFailureResponse

      verify(injected[Cache], times(1)).fetch(any())

      verify(injected[WsAllMethods], times(1)).GET(meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=CAD&cc=USD"))(any(),any(),any())

      verify(injected[WsAllMethods], times(1)).POST[CalculatorRequest, CalculatorResponse](
        meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"),
        meq(CalculatorRequest(isPrivateCraft = false, isAgeOver17 = true, isVatResClaimed = None, List(
          PurchasedItem(PurchasedProductInstance(ProductPath("other-goods/antiques"),"iid0",None,None,Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)),Some("CAD"),Some(BigDecimal("2.00"))),ProductTreeLeaf("antiques","label.other-goods.antiques","OGD/ART","other-goods", Nil),Currency("CAD","title.canadian_dollars_cad",Some("CAD"), Nil), BigDecimal("1.13"), ExchangeRate("1.7654", todaysDate))
        ))),
        any())(any(),any(),any(),any())


    }
  }


  "Calling UserInformationService.storeCalculatorResponse" should {

    "store a new user information" in {

      lazy val s = {
        val service = app.injector.instanceOf[CalculatorService]
        val mock = service.cache
        when(mock.fetch(any())) thenReturn Future.successful( None )
        when(mock.store(any())(any())) thenReturn Future.successful( CacheMap("fakeid", Map.empty) )
        service
      }

      await(s.storeCalculatorResponse(JourneyData(), CalculatorResponse(None, None, None, Calculation("0.00", "0.00", "0.00", "0.00"), withinFreeAllowance = true, limits = Map.empty)))

      verify(s.cache, times(1)).store(
        meq(JourneyData(calculatorResponse = Some(CalculatorResponse(None, None, None, Calculation("0.00", "0.00", "0.00", "0.00"), withinFreeAllowance = true, limits = Map.empty))))
      )(any())

    }

  }
}
