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

import connectors.Cache
import models.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.{BaseSpec, parseLocalDate}

import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class CalculatorServiceSpec extends BaseSpec with ScalaFutures {

  private val mockGetRequestBuilder: RequestBuilder  = mock(classOf[RequestBuilder])
  private val mockPostRequestBuilder: RequestBuilder = mock(classOf[RequestBuilder])
  private val mockHttpClient: HttpClientV2           = mock(classOf[HttpClientV2])
  private val mockCache: Cache                       = mock(classOf[Cache])
  private val mockServicesConfig: ServicesConfig     = mock(classOf[ServicesConfig])
  private val productTreeService: ProductTreeService = new ProductTreeService
  private val currencyService: CurrencyService       = new CurrencyService

  private val urlCapture: ArgumentCaptor[URL]                     = ArgumentCaptor.forClass(classOf[URL])
  private val jsonBodyCapture: ArgumentCaptor[JsValue]            = ArgumentCaptor.forClass(classOf[JsValue])
  private val journeyDataBodyCapture: ArgumentCaptor[JourneyData] = ArgumentCaptor.forClass(classOf[JourneyData])

  given messagesApi: MessagesApi = injected[MessagesApi]

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockGetRequestBuilder)
    reset(mockPostRequestBuilder)
    reset(mockCache)
    super.beforeEach()
  }

  def todaysDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)

  private val service: CalculatorService = new CalculatorService(
    cache = mockCache,
    httpClient = mockHttpClient,
    productTreeService = productTreeService,
    currencyService = currencyService,
    servicesConfig = mockServicesConfig,
    ec = ec
  )

  "Calling CalculatorService.journeyDataToCalculatorRequest" should {

    val missingRateJourneyData = JourneyData(
      None,
      Some("nonEuOnly"),
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some(false),
      Some(true),
      Some(true),
      Nil,
      List(
        PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(12),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("USD"),
          Some(123)
        )
      ),
      None
    )

    val goodJourneyData = JourneyData(
      euCountryCheck = Some("nonEuOnly"),
      isVatResClaimed = None,
      isBringingDutyFree = Some(false),
      bringingOverAllowance = None,
      privateCraft = Some(false),
      ageOver17 = Some(true),
      arrivingNICheck = Some(false),
      irishBorder = Some(false),
      isUKVatPaid = None,
      isUKVatExcisePaid = Some(true),
      isUKResident = Some(false),
      isUccRelief = Some(true),
      purchasedProductInstances = List(
        PurchasedProductInstance(
          ProductPath("other-goods/car-seats"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(74563)
        ),
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(33)
        ),
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          "iid1",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("CHF"),
          Some(5432)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/chewing-tobacco"),
          "iid0",
          Some(45),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("CHF"),
          Some(43)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/cigars"),
          "iid0",
          Some(40),
          Some(20),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(1234)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid0",
          None,
          Some(200),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GBP"),
          Some(60)
        ),
        PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(12),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GGP"),
          Some(123)
        )
      ),
      deltaCalculation = None
    )

    val imperfectJourneyData = JourneyData(
      euCountryCheck = Some("nonEuOnly"),
      isVatResClaimed = None,
      isBringingDutyFree = Some(false),
      bringingOverAllowance = None,
      privateCraft = Some(false),
      ageOver17 = Some(true),
      arrivingNICheck = Some(false),
      irishBorder = Some(false),
      isUKVatPaid = None,
      isUKVatExcisePaid = Some(true),
      isUKResident = Some(false),
      isUccRelief = Some(true),
      purchasedProductInstances = List(
        PurchasedProductInstance(
          ProductPath("other-goods/car-seats"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(74563)
        ),
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(33)
        ),
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          "iid1",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("CHF"),
          Some(5432)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/chewing-tobacco"),
          "iid0",
          Some(45),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("CHF"),
          Some(43)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/cigars"),
          "iid0",
          weightOrVolume = None,
          Some(20),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(1234)
        ), // Note weightOrVolume = None
        PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid0",
          None,
          Some(200),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GBP"),
          Some(60)
        ),
        PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(12),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GGP"),
          Some(123)
        )
      )
    )

    def getProductTreeLeaf(path: String): ProductTreeLeaf =
      productTreeService.productTree.getDescendant(ProductPath(path)).get.asInstanceOf[ProductTreeLeaf]

    val calcRequest = CalculatorServiceRequest(
      isPrivateCraft = false,
      isAgeOver17 = true,
      isArrivingNI = false,
      List(
        PurchasedItem(
          PurchasedProductInstance(
            ProductPath("other-goods/car-seats"),
            "iid0",
            None,
            None,
            Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
            None,
            Some("AUD"),
            Some(74563)
          ),
          getProductTreeLeaf("other-goods/car-seats"),
          Currency("AUD", "title.australian_dollars_aud", Some("AUD"), List("Australian", "Oz")),
          BigDecimal(74563 / 1.76).setScale(2, RoundingMode.DOWN),
          ExchangeRate("1.76", todaysDate)
        ),
        PurchasedItem(
          PurchasedProductInstance(
            ProductPath("other-goods/antiques"),
            "iid0",
            None,
            None,
            Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
            None,
            Some("AUD"),
            Some(33)
          ),
          getProductTreeLeaf("other-goods/antiques"),
          Currency("AUD", "title.australian_dollars_aud", Some("AUD"), List("Australian", "Oz")),
          BigDecimal(33 / 1.76).setScale(2, RoundingMode.DOWN),
          ExchangeRate("1.76", todaysDate)
        ),
        PurchasedItem(
          PurchasedProductInstance(
            ProductPath("other-goods/antiques"),
            "iid1",
            None,
            None,
            Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
            None,
            Some("CHF"),
            Some(5432)
          ),
          getProductTreeLeaf("other-goods/antiques"),
          Currency("CHF", "title.swiss_francs_chf", Some("CHF"), List("Swiss", "Switzerland")),
          BigDecimal(5432 / 1.26).setScale(2, RoundingMode.DOWN),
          ExchangeRate("1.26", todaysDate)
        ),
        PurchasedItem(
          PurchasedProductInstance(
            ProductPath("tobacco/chewing-tobacco"),
            "iid0",
            Some(45),
            None,
            Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
            None,
            Some("CHF"),
            Some(43)
          ),
          getProductTreeLeaf("tobacco/chewing-tobacco"),
          Currency("CHF", "title.swiss_francs_chf", Some("CHF"), List("Swiss", "Switzerland")),
          BigDecimal(43 / 1.26).setScale(2, RoundingMode.DOWN),
          ExchangeRate("1.26", todaysDate)
        ),
        PurchasedItem(
          PurchasedProductInstance(
            ProductPath("tobacco/cigars"),
            "iid0",
            Some(40),
            Some(20),
            Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
            None,
            Some("AUD"),
            Some(1234)
          ),
          getProductTreeLeaf("tobacco/cigars"),
          Currency("AUD", "title.australian_dollars_aud", Some("AUD"), List("Australian", "Oz")),
          BigDecimal(1234 / 1.76).setScale(2, RoundingMode.DOWN),
          ExchangeRate("1.76", todaysDate)
        ),
        PurchasedItem(
          PurchasedProductInstance(
            ProductPath("tobacco/cigarettes"),
            "iid0",
            None,
            Some(200),
            Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
            None,
            Some("GBP"),
            Some(60)
          ),
          getProductTreeLeaf("tobacco/cigarettes"),
          Currency(
            "GBP",
            "title.british_pounds_gbp",
            None,
            List("England", "Scotland", "Wales", "Northern Ireland", "British", "sterling", "pound", "GB")
          ),
          BigDecimal(60).setScale(2, RoundingMode.DOWN),
          ExchangeRate("1.00", todaysDate)
        ),
        PurchasedItem(
          PurchasedProductInstance(
            ProductPath("alcohol/beer"),
            "iid0",
            Some(12),
            None,
            Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
            None,
            Some("GGP"),
            Some(123)
          ),
          getProductTreeLeaf("alcohol/beer"),
          Currency("GGP", "title.guernsey_pounds_ggp", None, List("Channel Islands")),
          BigDecimal(123).setScale(2, RoundingMode.DOWN),
          ExchangeRate("1.00", todaysDate)
        )
      )
    )

    trait Setup {

      when(mockServicesConfig.baseUrl("currency-conversion")).thenReturn("http://localhost:9016")
      when(mockGetRequestBuilder.execute(using any[HttpReads[List[CurrencyConversionRate]]], any()))
        .thenReturn(
          Future.successful(
            List(
              CurrencyConversionRate(parseLocalDate("2018-08-01"), parseLocalDate("2018-08-31"), "USD", None)
            )
          )
        )
      when(mockGetRequestBuilder.execute(using any[HttpReads[List[CurrencyConversionRate]]], any()))
        .thenReturn(
          Future.successful(
            List(
              CurrencyConversionRate(parseLocalDate("2018-08-01"), parseLocalDate("2018-08-31"), "AUD", Some("1.76")),
              CurrencyConversionRate(parseLocalDate("2018-08-01"), parseLocalDate("2018-08-31"), "CHF", Some("1.26"))
            )
          )
        )
      when(mockHttpClient.get(any())(any())).thenReturn(mockGetRequestBuilder)
    }

    "return None if there was a missing rate, making a call to the currency-conversion service" in new Setup {

      val url: String = s"http://localhost:9016/currency-conversion/rates/$todaysDate?cc=USD"

      val response: Option[CalculatorServiceRequest] =
        service
          .journeyDataToCalculatorRequest(missingRateJourneyData, missingRateJourneyData.purchasedProductInstances)
          .futureValue

      response shouldBe None

      verify(mockGetRequestBuilder, times(1)).execute(using any(), any())
      verify(mockHttpClient, times(1)).get(urlCapture.capture())(any())

      urlCapture.getValue shouldBe url"$url"
    }

    "skip invalid instances (instances with missing required data)" in new Setup {

      val url: String = s"http://localhost:9016/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"

      val response: CalculatorServiceRequest =
        service
          .journeyDataToCalculatorRequest(imperfectJourneyData, imperfectJourneyData.purchasedProductInstances)
          .futureValue
          .get

      response shouldBe calcRequest.copy(items = calcRequest.items.filterNot(_.productTreeLeaf.token == "cigars"))

      verify(mockGetRequestBuilder, times(1)).execute(using any(), any())
      verify(mockHttpClient, times(1)).get(urlCapture.capture())(any())

      urlCapture.getValue shouldBe url"$url"
    }

    "transform journey data to a calculator request, making a call to the currency-conversion service" in new Setup {

      val url: String = s"http://localhost:9016/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"

      val response: CalculatorServiceRequest =
        service
          .journeyDataToCalculatorRequest(goodJourneyData, goodJourneyData.purchasedProductInstances)
          .futureValue
          .get

      response shouldBe calcRequest

      verify(mockGetRequestBuilder, times(1)).execute(using any(), any())
      verify(mockHttpClient, times(1)).get(urlCapture.capture())(any())

      urlCapture.getValue shouldBe url"$url"
    }
  }

  "Calling CalculatorService.journeyDataToLimitsRequest" should {

    val amendJourneyData = JourneyData(
      euCountryCheck = Some("nonEuOnly"),
      isVatResClaimed = None,
      isBringingDutyFree = Some(false),
      bringingOverAllowance = None,
      privateCraft = Some(false),
      ageOver17 = Some(true),
      arrivingNICheck = Some(false),
      irishBorder = Some(false),
      isUKVatPaid = None,
      isUKVatExcisePaid = Some(true),
      isUKResident = Some(false),
      isUccRelief = Some(true),
      purchasedProductInstances = List(
        PurchasedProductInstance(
          ProductPath("other-goods/car-seats"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(74563)
        ),
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(33)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/chewing-tobacco"),
          "iid0",
          Some(45),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("CHF"),
          Some(43)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/cigars"),
          "iid0",
          Some(40),
          Some(20),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(1234)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid0",
          None,
          Some(200),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GBP"),
          Some(60)
        ),
        PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(12),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GGP"),
          Some(123)
        )
      ),
      declarationResponse = Some(
        DeclarationResponse(
          oldPurchaseProductInstances = List(
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid1",
              Some(1.54332),
              None,
              Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
              None,
              Some("AUD"),
              Some(BigDecimal(10.234)),
              None,
              None,
              None,
              isEditable = Some(false)
            ),
            PurchasedProductInstance(
              ProductPath("tobacco/cigarettes"),
              "iid1",
              Some(1.54332),
              Some(20),
              Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
              None,
              Some("AUD"),
              Some(BigDecimal(10.234)),
              None,
              None,
              None,
              isEditable = Some(false)
            ),
            PurchasedProductInstance(
              ProductPath("other-goods/antiques"),
              "iid2",
              None,
              None,
              Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
              None,
              Some("CHF"),
              Some(5432),
              None,
              None,
              None,
              isEditable = Some(false)
            )
          ),
          liabilityDetails = LiabilityDetails("32.0", "0.0", "126.4", "158.40"),
          calculation = Calculation("1.00", "1.00", "1.00", "3.00")
        )
      )
    )

    val declareJourneyData = JourneyData(
      euCountryCheck = Some("nonEuOnly"),
      isVatResClaimed = None,
      isBringingDutyFree = Some(false),
      bringingOverAllowance = None,
      privateCraft = Some(false),
      ageOver17 = Some(true),
      arrivingNICheck = Some(false),
      irishBorder = Some(false),
      isUKVatPaid = None,
      isUKVatExcisePaid = Some(true),
      isUKResident = Some(false),
      isUccRelief = Some(true),
      purchasedProductInstances = List(
        PurchasedProductInstance(
          ProductPath("other-goods/car-seats"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(74563)
        ),
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(33)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/chewing-tobacco"),
          "iid0",
          Some(45),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("CHF"),
          Some(43)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/cigars"),
          "iid0",
          Some(40),
          Some(20),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(1234)
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid0",
          None,
          Some(200),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GBP"),
          Some(60)
        ),
        PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(12),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GGP"),
          Some(123)
        )
      )
    )

    def getProductTreeLeaf(path: String): ProductTreeLeaf =
      productTreeService.productTree.getDescendant(ProductPath(path)).get.asInstanceOf[ProductTreeLeaf]

    val amendSpeculativeItem: List[SpeculativeItem] = List(
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid1",
          Some(1.54332),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(BigDecimal(10.234)),
          None,
          None,
          None,
          isEditable = Some(false)
        ),
        getProductTreeLeaf("alcohol/beer"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid1",
          Some(1.54332),
          Some(20),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(BigDecimal(10.234)),
          None,
          None,
          None,
          isEditable = Some(false)
        ),
        getProductTreeLeaf("tobacco/cigarettes"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          "iid2",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("CHF"),
          Some(5432),
          None,
          None,
          None,
          isEditable = Some(false)
        ),
        getProductTreeLeaf("other-goods/antiques"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("other-goods/car-seats"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(74563)
        ),
        getProductTreeLeaf("other-goods/car-seats"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(33)
        ),
        getProductTreeLeaf("other-goods/antiques"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("tobacco/chewing-tobacco"),
          "iid0",
          Some(45),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("CHF"),
          Some(43)
        ),
        getProductTreeLeaf("tobacco/chewing-tobacco"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("tobacco/cigars"),
          "iid0",
          Some(40),
          Some(20),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(1234)
        ),
        getProductTreeLeaf("tobacco/cigars"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid0",
          None,
          Some(200),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GBP"),
          Some(60)
        ),
        getProductTreeLeaf("tobacco/cigarettes"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(12),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GGP"),
          Some(123)
        ),
        getProductTreeLeaf("alcohol/beer"),
        0
      )
    )

    val speculativeItem: List[SpeculativeItem] = List(
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("other-goods/car-seats"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(74563)
        ),
        getProductTreeLeaf("other-goods/car-seats"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          "iid0",
          None,
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(33)
        ),
        getProductTreeLeaf("other-goods/antiques"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("tobacco/chewing-tobacco"),
          "iid0",
          Some(45),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("CHF"),
          Some(43)
        ),
        getProductTreeLeaf("tobacco/chewing-tobacco"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("tobacco/cigars"),
          "iid0",
          Some(40),
          Some(20),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("AUD"),
          Some(1234)
        ),
        getProductTreeLeaf("tobacco/cigars"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid0",
          None,
          Some(200),
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GBP"),
          Some(60)
        ),
        getProductTreeLeaf("tobacco/cigarettes"),
        0
      ),
      SpeculativeItem(
        PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(12),
          None,
          Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          None,
          Some("GGP"),
          Some(123)
        ),
        getProductTreeLeaf("alcohol/beer"),
        0
      )
    )

    val amendLimitRequest =
      LimitRequest(isPrivateCraft = false, isAgeOver17 = true, isArrivingNI = false, items = amendSpeculativeItem)

    val limitRequest =
      LimitRequest(isPrivateCraft = false, isAgeOver17 = true, isArrivingNI = false, items = speculativeItem)

    "transform journey data to a limit request for an amendment journey" in {

      val response: LimitRequest = service.journeyDataToLimitsRequest(amendJourneyData).get

      response shouldBe amendLimitRequest
    }

    "transform journey data to a limit request for an original journey" in {

      val response: LimitRequest = service.journeyDataToLimitsRequest(declareJourneyData).get

      response shouldBe limitRequest
    }
  }

  "Calling CalculatorService.calculate" should {

    val ccUrl: String  = s"http://localhost:9016/currency-conversion/rates/$todaysDate?cc=CAD&cc=USD"
    val pdcUrl: String = "http://localhost:9027/passengers-duty-calculator/calculate"

    val currencyConversionRates: List[CurrencyConversionRate] = List(
      CurrencyConversionRate(
        parseLocalDate("2018-08-01"),
        parseLocalDate("2018-08-31"),
        "USD",
        Some("1.4534")
      ),
      CurrencyConversionRate(
        parseLocalDate("2018-08-01"),
        parseLocalDate("2018-08-31"),
        "CAD",
        Some("1.7654")
      )
    )

    val calculationResponse: CalculatorResponse = CalculatorResponse(
      Some(Alcohol(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
      Calculation("0.00", "0.00", "0.00", "0.00"),
      withinFreeAllowance = false,
      limits = Map.empty,
      isAnyItemOverAllowance = false
    )

    val jd: JourneyData = JourneyData(
      euCountryCheck = Some("nonEuOnly"),
      ageOver17 = Some(true),
      arrivingNICheck = Some(false),
      privateCraft = Some(false),
      purchasedProductInstances = List(
        PurchasedProductInstance(
          ProductPath("other-goods/antiques"),
          iid = "iid0",
          country = Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          currency = Some("CAD"),
          cost = Some(BigDecimal("2.00"))
        ),
        PurchasedProductInstance(
          ProductPath("tobacco/cigars"),
          iid = "iid1",
          country = Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          currency = Some("USD"),
          cost = Some(BigDecimal("4.00"))
        )
      )
    )

    val json: JsValue = Json.toJson[CalculatorServiceRequest](
      CalculatorServiceRequest(
        isPrivateCraft = false,
        isAgeOver17 = true,
        isArrivingNI = false,
        List(
          PurchasedItem(
            PurchasedProductInstance(
              ProductPath("other-goods/antiques"),
              "iid0",
              None,
              None,
              Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
              None,
              Some("CAD"),
              Some(BigDecimal("2.00"))
            ),
            ProductTreeLeaf("antiques", "label.other-goods.antiques", "OGD/ART", "other-goods", Nil),
            Currency("CAD", "title.canadian_dollars_cad", Some("CAD"), Nil),
            BigDecimal("1.13"),
            ExchangeRate("1.7654", todaysDate)
          )
        )
      )
    )(CalculatorServiceRequest.writes)

    when(mockServicesConfig.baseUrl("passengers-duty-calculator")).thenReturn("http://localhost:9027")
    when(mockServicesConfig.baseUrl("currency-conversion")).thenReturn("http://localhost:9016")

    trait Setup {

      when(mockGetRequestBuilder.execute(using any[HttpReads[List[CurrencyConversionRate]]], any()))
        .thenReturn(
          Future.successful(
            currencyConversionRates
          )
        )
      when(mockHttpClient.get(any())(any())).thenReturn(mockGetRequestBuilder)

      when(mockPostRequestBuilder.withBody(any())(using any(), any(), any())).thenReturn(mockPostRequestBuilder)
      when(mockPostRequestBuilder.execute(using any[HttpReads[CalculatorResponse]], any())).thenReturn(
        Future.successful(calculationResponse)
      )
      when(mockHttpClient.post(any())(any())).thenReturn(mockPostRequestBuilder)
    }

    "make a call to the currency-conversion service, the calculator service and return a valid response" in new Setup {

      when(mockPostRequestBuilder.execute(using any[HttpReads[CalculatorResponse]], any())).thenReturn(
        Future.successful(calculationResponse)
      )

      val response: CalculatorServiceResponse = service.calculate(jd).futureValue

      response.asInstanceOf[CalculatorServiceSuccessResponse].calculatorResponse shouldBe
        CalculatorResponse(
          Some(Alcohol(List(), Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(Tobacco(List(), Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(List(), Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("0.00", "0.00", "0.00", "0.00"),
          withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = false
        )

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockPostRequestBuilder, times(1)).withBody(jsonBodyCapture.capture())(using any(), any(), any())
      verify(mockPostRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue      shouldBe url"$pdcUrl"
      jsonBodyCapture.getValue shouldBe json

      verify(mockHttpClient, times(1)).get(urlCapture.capture())(any())
      verify(mockGetRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue shouldBe url"$ccUrl"
    }

    "make a call to the currency-conversion service, the calculator service and return CalculatorServicePurchasePriceOutOfBoundsFailureResponse" when {
      "call to calculator returns 416 REQUESTED_RANGE_NOT_SATISFIABLE" in new Setup {

        when(mockPostRequestBuilder.execute(using any[HttpReads[CalculatorResponse]], any())).thenReturn(
          Future.failed(
            UpstreamErrorResponse
              .apply("Any message", REQUESTED_RANGE_NOT_SATISFIABLE, REQUESTED_RANGE_NOT_SATISFIABLE, Map.empty)
          )
        )

        val response: CalculatorServiceResponse = service.calculate(jd).futureValue

        response shouldBe CalculatorServicePurchasePriceOutOfBoundsFailureResponse

        verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
        verify(mockPostRequestBuilder, times(1)).withBody(jsonBodyCapture.capture())(using any(), any(), any())
        verify(mockPostRequestBuilder, times(1)).execute(using any(), any())

        urlCapture.getValue      shouldBe url"$pdcUrl"
        jsonBodyCapture.getValue shouldBe json

        verify(mockHttpClient, times(1)).get(urlCapture.capture())(any())
        verify(mockGetRequestBuilder, times(1)).execute(using any(), any())

        urlCapture.getValue shouldBe url"$ccUrl"
      }
    }

    "return CalculatorServiceCantBuildCalcReqResponse" in {

      val response: CalculatorServiceResponse = service.calculate(JourneyData()).futureValue

      response shouldBe CalculatorServiceCantBuildCalcReqResponse
    }
  }

  "Calling CalculatorService.storeCalculatorResponse" should {

    val calculation: Calculation = Calculation("136.27", "150.00", "297.25", "583.52")

    val calculatorResponse = CalculatorResponse(
      None,
      None,
      None,
      calculation,
      withinFreeAllowance = true,
      limits = Map.empty,
      isAnyItemOverAllowance = false
    )

    "store a new CalculatorServiceResponse in JourneyData" in {

      when(mockCache.store(any())(any())).thenReturn(Future.successful(JourneyData()))

      val response: JourneyData = service.storeCalculatorResponse(JourneyData(), calculatorResponse).futureValue

      response shouldBe JourneyData(calculatorResponse = Some(calculatorResponse))

      verify(mockCache, times(1)).store(journeyDataBodyCapture.capture())(any())

      journeyDataBodyCapture.getValue shouldBe JourneyData(
        calculatorResponse = Some(
          CalculatorResponse(
            None,
            None,
            None,
            calculation,
            withinFreeAllowance = true,
            limits = Map.empty,
            isAnyItemOverAllowance = false
          )
        )
      )
    }

    "store a new CalculatorServiceResponse along with deltaCalculation in JourneyData" in {

      when(mockCache.store(any())(any())).thenReturn(Future.successful(JourneyData()))

      lazy val deltaCalc: Calculation = Calculation("96.27", "150.00", "109.25", "355.52")

      val response: JourneyData = service
        .storeCalculatorResponse(
          JourneyData(),
          calculatorResponse,
          Some(deltaCalc)
        )
        .futureValue

      response shouldBe JourneyData(calculatorResponse = Some(calculatorResponse), deltaCalculation = Some(deltaCalc))

      verify(mockCache, times(1)).store(journeyDataBodyCapture.capture())(any())

      journeyDataBodyCapture.getValue shouldBe JourneyData(
        calculatorResponse = Some(
          CalculatorResponse(
            None,
            None,
            None,
            calculation,
            withinFreeAllowance = true,
            limits = Map.empty,
            isAnyItemOverAllowance = false
          )
        ),
        deltaCalculation = Some(deltaCalc)
      )
    }
  }

  "Calling CalculatorService.getDeltaCalculation with old and new Calculation" should {

    "return deltaCalculation" in {

      lazy val oldCalc: Calculation       = Calculation("96.27", "100.00", "109.25", "305.52")
      lazy val newCalc: Calculation       = Calculation("136.27", "150.00", "297.25", "583.52")
      lazy val deltaProbable: Calculation = Calculation("40.00", "50.00", "188.00", "278.00")

      val deltaCalc: Calculation = service.getDeltaCalculation(oldCalc, newCalc)

      deltaCalc shouldBe deltaProbable
    }
  }

  "Calling CalculatorService.getPreviousPaidCalculation with delta and new Calculation" should {

    "return previousPaidCalculation" in {

      lazy val deltaCalc: Calculation            = Calculation("96.27", "100.00", "109.25", "305.52")
      lazy val newCalc: Calculation              = Calculation("136.27", "150.00", "297.25", "583.52")
      lazy val previousPaidProbable: Calculation = Calculation("40.00", "50.00", "188.00", "278.00")

      val previousPaidCalc: Calculation = service.getPreviousPaidCalculation(deltaCalc, newCalc)

      previousPaidCalc shouldBe previousPaidProbable
    }
  }

  "Calling CalculatorService.limitUsage" should {
    val jsonObj: JsObject = Json.obj(
      "limits" -> Json.obj(
        "L-WINE" -> "0.4444"
      )
    )

    val purchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
      path = ProductPath("alcohol/wine"),
      iid = "iid0"
    )

    val calculation: Calculation = Calculation(
      excise = "0.00",
      customs = "0.00",
      vat = "0.00",
      allTax = "0.00"
    )

    val liabilityDetails: LiabilityDetails = LiabilityDetails(
      totalExciseGBP = "0.00",
      totalCustomsGBP = "0.00",
      totalVATGBP = "0.00",
      grandTotalGBP = "0.00"
    )

    val oldPurchasedProductInstances: List[PurchasedProductInstance] = List(purchasedProductInstance)

    val declarationResponse: DeclarationResponse = DeclarationResponse(
      calculation = calculation,
      oldPurchaseProductInstances = oldPurchasedProductInstances,
      liabilityDetails = liabilityDetails
    )

    val journeyData: JourneyData = JourneyData(
      privateCraft = Some(false),
      ageOver17 = Some(false),
      arrivingNICheck = Some(false),
      declarationResponse = Some(declarationResponse)
    )

    "return LimitUsageSuccessResponse" in {

      val url: String   = "http://localhost:9027/passengers-duty-calculator/limits"
      val body: JsValue = Json.toJson(service.journeyDataToLimitsRequest(journeyData))

      when(mockServicesConfig.baseUrl("passengers-duty-calculator")).thenReturn("http://localhost:9027")
      when(mockPostRequestBuilder.withBody(any())(using any(), any(), any())).thenReturn(mockPostRequestBuilder)
      when(mockPostRequestBuilder.execute(using any[HttpReads[JsObject]], any())).thenReturn(Future.successful(jsonObj))
      when(mockHttpClient.post(any())(any())).thenReturn(mockPostRequestBuilder)

      val response: LimitUsageResponse = service.limitUsage(journeyData).futureValue

      response shouldBe LimitUsageSuccessResponse(Map("L-WINE" -> "0.4444"))

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockPostRequestBuilder, times(1)).withBody(jsonBodyCapture.capture())(using any(), any(), any())
      verify(mockPostRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue      shouldBe url"$url"
      jsonBodyCapture.getValue shouldBe body
    }

    "return LimitUsageCantBuildCalcReqResponse" in {
      val response: LimitUsageResponse = service.limitUsage(JourneyData()).futureValue

      response shouldBe LimitUsageCantBuildCalcReqResponse
    }
  }
}
