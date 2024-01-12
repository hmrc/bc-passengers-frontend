/*
 * Copyright 2023 HM Revenue & Customs
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
import models._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.http.WsAllMethods
import uk.gov.hmrc.http.UpstreamErrorResponse
import util.{BaseSpec, parseLocalDate}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class CalculatorServiceSpec extends BaseSpec {
  // scalastyle:off magic.number
  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[WsAllMethods].toInstance(MockitoSugar.mock[WsAllMethods]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .configure(
      "microservice.services.currency-conversion.host"        -> "currency-conversion.service",
      "microservice.services.currency-conversion.port"        -> "80",
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
        ), //Note weightOrVolume = None
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
      injected[ProductTreeService].productTree.getDescendant(ProductPath(path)).get.asInstanceOf[ProductTreeLeaf]

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

    trait LocalSetup {

      lazy val service: CalculatorService = {

        when(
          injected[WsAllMethods].GET[List[CurrencyConversionRate]](
            meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=USD"),
            any(),
            any()
          )(any(), any(), any())
        ) thenReturn {
          Future.successful(
            List(
              CurrencyConversionRate(parseLocalDate("2018-08-01"), parseLocalDate("2018-08-31"), "USD", None)
            )
          )
        }

        when(
          injected[WsAllMethods].GET[List[CurrencyConversionRate]](any(), any(), any())(any(), any(), any())
        ) thenReturn {
          Future.successful(
            List(
              CurrencyConversionRate(parseLocalDate("2018-08-01"), parseLocalDate("2018-08-31"), "AUD", Some("1.76")),
              CurrencyConversionRate(parseLocalDate("2018-08-01"), parseLocalDate("2018-08-31"), "CHF", Some("1.26"))
            )
          )
        }

        injected[CalculatorService]
      }
    }

    "return None if there was a missing rate, making a call to the currency-conversion service" in new LocalSetup {

      val response: Option[CalculatorServiceRequest] = await(
        service.journeyDataToCalculatorRequest(missingRateJourneyData, missingRateJourneyData.purchasedProductInstances)
      )

      verify(injected[Cache], times(0)).fetch(any())
      verify(injected[WsAllMethods], times(1)).GET(
        meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=USD"),
        any(),
        any()
      )(any(), any(), any())

      response shouldBe None
    }

    "skip invalid instances (instances with missing required data)" in new LocalSetup {

      val response: CalculatorServiceRequest = await(
        service.journeyDataToCalculatorRequest(imperfectJourneyData, imperfectJourneyData.purchasedProductInstances)
      ).get

      verify(injected[Cache], times(0)).fetch(any())
      verify(injected[WsAllMethods], times(1)).GET(
        meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"),
        any(),
        any()
      )(any(), any(), any())

      response shouldBe calcRequest.copy(items = calcRequest.items.filterNot(_.productTreeLeaf.token == "cigars"))
    }

    "transform journey data to a calculator request, making a call to the currency-conversion service" in new LocalSetup {

      val response: CalculatorServiceRequest =
        await(service.journeyDataToCalculatorRequest(goodJourneyData, goodJourneyData.purchasedProductInstances)).get

      verify(injected[Cache], times(0)).fetch(any())
      verify(injected[WsAllMethods], times(1)).GET(
        meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=AUD&cc=CHF"),
        any(),
        any()
      )(any(), any(), any())

      response shouldBe calcRequest
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
      injected[ProductTreeService].productTree.getDescendant(ProductPath(path)).get.asInstanceOf[ProductTreeLeaf]

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

    trait LocalSetup {

      lazy val service: CalculatorService =
        injected[CalculatorService]
    }

    "transform journey data to a limit request for an amendment journey" in new LocalSetup {

      val response: LimitRequest = service.journeyDataToLimitsRequest(amendJourneyData).get

      verify(injected[Cache], times(0)).fetch(any())

      response shouldBe amendLimitRequest
    }

    "transform journey data to a limit request for an original journey" in new LocalSetup {

      val response: LimitRequest = service.journeyDataToLimitsRequest(declareJourneyData).get

      verify(injected[Cache], times(0)).fetch(any())

      response shouldBe limitRequest
    }
  }

  "Calling CalculatorService.calculate" should {

    trait LocalSetup {

      def simulatePurchasePriceOutOfBounds: Boolean

      lazy val service: CalculatorService = {

        when(
          injected[WsAllMethods].GET[List[CurrencyConversionRate]](
            meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=CAD&cc=USD"),
            any(),
            any()
          )(any(), any(), any())
        ) thenReturn {
          Future.successful(
            List(
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
          )
        }

        if (simulatePurchasePriceOutOfBounds) {
          when(
            injected[WsAllMethods].POST[CalculatorServiceRequest, CalculatorResponse](
              meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"),
              any(),
              any()
            )(any(), any(), any(), any())
          ) thenReturn Future.failed(
            UpstreamErrorResponse
              .apply("Any message", REQUESTED_RANGE_NOT_SATISFIABLE, REQUESTED_RANGE_NOT_SATISFIABLE, Map.empty)
          )
        } else {
          when(
            injected[WsAllMethods].POST[CalculatorServiceRequest, CalculatorResponse](
              meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"),
              any(),
              any()
            )(any(), any(), any(), any())
          ) thenReturn {
            Future.successful(
              CalculatorResponse(
                Some(Alcohol(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
                Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
                Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
                Calculation("0.00", "0.00", "0.00", "0.00"),
                withinFreeAllowance = false,
                limits = Map.empty,
                isAnyItemOverAllowance = false
              )
            )
          }
        }

        injected[CalculatorService]
      }
    }

    "make a call to the currency-conversion service, the calculator service and return a valid response" in new LocalSetup {

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

      override lazy val simulatePurchasePriceOutOfBounds: Boolean = false

      val messages: MessagesApi = injected[MessagesApi]

      val response: CalculatorServiceResponse = await(service.calculate(jd)(implicitly, messages))

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

      verify(injected[WsAllMethods], times(1)).GET(
        meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=CAD&cc=USD"),
        any(),
        any()
      )(any(), any(), any())

      verify(injected[WsAllMethods], times(1)).POST[CalculatorServiceRequest, CalculatorResponse](
        meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"),
        meq(
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
        ),
        any()
      )(any(), any(), any(), any())
    }

    "make a call to the currency-conversion service, the calculator service and return CalculatorServicePurchasePriceOutOfBoundsFailureResponse" +
      "when call to calculator returns 416 REQUESTED_RANGE_NOT_SATISFIABLE" in new LocalSetup {

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

        override lazy val simulatePurchasePriceOutOfBounds: Boolean = true

        val messages: MessagesApi = injected[MessagesApi]

        await(
          service.calculate(jd)(implicitly, messages)
        ) shouldBe CalculatorServicePurchasePriceOutOfBoundsFailureResponse

        verify(injected[WsAllMethods], times(1)).GET(
          meq(s"http://currency-conversion.service:80/currency-conversion/rates/$todaysDate?cc=CAD&cc=USD"),
          any(),
          any()
        )(any(), any(), any())

        verify(injected[WsAllMethods], times(1)).POST[CalculatorServiceRequest, CalculatorResponse](
          meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/calculate"),
          meq(
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
          ),
          any()
        )(any(), any(), any(), any())
      }

    "return CalculatorServiceCantBuildCalcReqResponse" in {
      val messages: MessagesApi      = injected[MessagesApi]
      val service: CalculatorService = injected[CalculatorService]

      val response: CalculatorServiceResponse = await(service.calculate(JourneyData())(implicitly, messages))

      response shouldBe CalculatorServiceCantBuildCalcReqResponse
    }
  }

  "Calling CalculatorService.storeCalculatorResponse" should {

    "store a new CalculatorServiceResponse in JourneyData" in {

      lazy val s: CalculatorService = {
        val service = app.injector.instanceOf[CalculatorService]
        val mock    = service.cache
        when(mock.fetch(any())) thenReturn Future.successful(None)
        when(mock.store(any())(any())) thenReturn Future.successful(JourneyData())
        service
      }

      await(
        s.storeCalculatorResponse(
          JourneyData(),
          CalculatorResponse(
            None,
            None,
            None,
            Calculation("0.00", "0.00", "0.00", "0.00"),
            withinFreeAllowance = true,
            limits = Map.empty,
            isAnyItemOverAllowance = false
          )
        )
      )

      verify(s.cache, times(1)).store(
        meq(
          JourneyData(calculatorResponse =
            Some(
              CalculatorResponse(
                None,
                None,
                None,
                Calculation("0.00", "0.00", "0.00", "0.00"),
                withinFreeAllowance = true,
                limits = Map.empty,
                isAnyItemOverAllowance = false
              )
            )
          )
        )
      )(any())

    }

  }

  "Calling CalculatorService.storeCalculatorResponse" should {

    "store a new CalculatorServiceResponse along with deltaCalculation in JourneyData" in {

      lazy val calcService: CalculatorService = {
        val service = app.injector.instanceOf[CalculatorService]
        val mock    = service.cache
        when(mock.fetch(any())) thenReturn Future.successful(None)
        when(mock.store(any())(any())) thenReturn Future.successful(JourneyData())
        service
      }

      lazy val deltaCalc: Calculation = Calculation("96.27", "150.00", "109.25", "355.52")

      await(
        calcService.storeCalculatorResponse(
          JourneyData(),
          CalculatorResponse(
            None,
            None,
            None,
            Calculation("136.27", "150.00", "297.25", "583.52"),
            withinFreeAllowance = true,
            limits = Map.empty,
            isAnyItemOverAllowance = false
          ),
          Some(deltaCalc)
        )
      )

      verify(calcService.cache, times(1)).store(
        meq(
          JourneyData(
            calculatorResponse = Some(
              CalculatorResponse(
                None,
                None,
                None,
                Calculation("136.27", "150.00", "297.25", "583.52"),
                withinFreeAllowance = true,
                limits = Map.empty,
                isAnyItemOverAllowance = false
              )
            ),
            deltaCalculation = Some(deltaCalc)
          )
        )
      )(any())

    }

  }

  "Calling CalculatorService.getDeltaCalculation with old and new Calculation" should {

    "return deltaCalculation" in {

      lazy val calcService: CalculatorService = {
        val service = app.injector.instanceOf[CalculatorService]
        val mock    = service.cache
        when(mock.fetch(any())) thenReturn Future.successful(None)
        when(mock.store(any())(any())) thenReturn Future.successful(JourneyData())
        service
      }

      lazy val oldCalc: Calculation       = Calculation("96.27", "100.00", "109.25", "305.52")
      lazy val newCalc: Calculation       = Calculation("136.27", "150.00", "297.25", "583.52")
      lazy val deltaProbable: Calculation = Calculation("40.00", "50.00", "188.00", "278.00")

      val deltaCalc: Calculation = calcService.getDeltaCalculation(oldCalc, newCalc)

      deltaCalc shouldBe deltaProbable

    }
  }

  "Calling CalculatorService.getPreviousPaidCalculation with delta and new Calculation" should {

    "return previousPaidCalculation" in {

      lazy val calcService: CalculatorService = {
        val service = app.injector.instanceOf[CalculatorService]
        val mock    = service.cache
        when(mock.fetch(any())) thenReturn Future.successful(None)
        when(mock.store(any())(any())) thenReturn Future.successful(JourneyData())
        service
      }

      lazy val deltaCalc: Calculation            = Calculation("96.27", "100.00", "109.25", "305.52")
      lazy val newCalc: Calculation              = Calculation("136.27", "150.00", "297.25", "583.52")
      lazy val previousPaidProbable: Calculation = Calculation("40.00", "50.00", "188.00", "278.00")

      val previousPaidCalc: Calculation = calcService.getPreviousPaidCalculation(deltaCalc, newCalc)

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

    val service: CalculatorService = injected[CalculatorService]

    "return LimitUsageSuccessResponse" in {
      when(
        injected[WsAllMethods].POST[LimitRequest, JsObject](
          meq("http://passengers-duty-calculator.service:80/passengers-duty-calculator/limits"),
          any(),
          any()
        )(any(), any(), any(), any())
      ).thenReturn(Future.successful(jsonObj))

      val response: LimitUsageResponse = await(service.limitUsage(journeyData))

      response shouldBe LimitUsageSuccessResponse(Map("L-WINE" -> "0.4444"))

      verify(injected[WsAllMethods], times(1)).POST(
        meq(s"http://passengers-duty-calculator.service:80/passengers-duty-calculator/limits"),
        any(),
        any()
      )(any(), any(), any(), any())
    }

    "return LimitUsageCantBuildCalcReqResponse" in {
      val response: LimitUsageResponse = await(service.limitUsage(JourneyData()))

      response shouldBe LimitUsageCantBuildCalcReqResponse
    }
  }
  // scalastyle:on magic.number
}
