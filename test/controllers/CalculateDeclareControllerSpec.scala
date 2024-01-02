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

package controllers

import connectors.Cache
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import repositories.BCPassengersSessionRepository
import services._
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter, parseLocalTime}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class CalculateDeclareControllerSpec extends BaseSpec {

  override implicit lazy val app: Application =
    GuiceApplicationBuilder()
      .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
      .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
      .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
      .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
      .overrides(bind[CalculatorService].toInstance(MockitoSugar.mock[CalculatorService]))
      .overrides(bind[UserInformationService].toInstance(MockitoSugar.mock[UserInformationService]))
      .overrides(bind[PayApiService].toInstance(MockitoSugar.mock[PayApiService]))
      .overrides(bind[DateTimeProviderService].toInstance(MockitoSugar.mock[DateTimeProviderService]))
      .overrides(bind[DeclarationService].toInstance(MockitoSugar.mock[DeclarationService]))
      .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
      .build()

  override def beforeEach(): Unit = {
    reset(injected[Cache])
    reset(injected[PurchasedProductService])
    reset(injected[UserInformationService])
    reset(injected[PayApiService])
    reset(injected[DateTimeProviderService])
    reset(injected[DeclarationService])
    reset(injected[TravelDetailsService])
  }

  trait LocalSetup {

    def payApiResponse: PayApiServiceResponse
    def declarationServiceResponse: DeclarationServiceResponse

    def cachedJourneyData: Future[Option[JourneyData]] =
      Future.successful(Some(JourneyData(None, Some("nonEuOnly"), Some(true), None, None, Some(true))))

    when(injected[Cache].fetch(any())) thenReturn cachedJourneyData

    lazy val crBelowLimit: CalculatorResponse = CalculatorResponse(
      Some(
        Alcohol(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        Tobacco(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        OtherGoods(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Calculation("0.00", "0.00", "0.00", "8.99"),
      withinFreeAllowance = true,
      limits = Map.empty,
      isAnyItemOverAllowance = false
    )

    lazy val crWithinLimitLow: CalculatorResponse = CalculatorResponse(
      Some(
        Alcohol(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        Tobacco(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        OtherGoods(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Calculation("0.00", "0.00", "0.00", "9.00"),
      withinFreeAllowance = true,
      limits = Map.empty,
      isAnyItemOverAllowance = false
    )

    lazy val crWithinLimitHigh: CalculatorResponse = CalculatorResponse(
      Some(
        Alcohol(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        Tobacco(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        OtherGoods(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Calculation("0.00", "0.00", "0.00", "97000.00"),
      withinFreeAllowance = true,
      limits = Map.empty,
      isAnyItemOverAllowance = false
    )

    lazy val crAboveLimit: CalculatorResponse = CalculatorResponse(
      Some(
        Alcohol(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        Tobacco(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        OtherGoods(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Calculation("0.00", "0.00", "0.00", "97000.01"),
      withinFreeAllowance = true,
      limits = Map.empty,
      isAnyItemOverAllowance = false
    )

    lazy val crZero: CalculatorResponse = CalculatorResponse(
      Some(
        Alcohol(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        Tobacco(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Some(
        OtherGoods(
          List(
            Band(
              "A",
              List(
                Item(
                  "Adult clothing",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Adult clothing",
                    "Adult clothing",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      Calculation("0.00", "0.00", "0.00", "0.00"),
      withinFreeAllowance = true,
      limits = Map.empty,
      isAnyItemOverAllowance = true
    )

    lazy val ui: UserInformation = UserInformation(
      "Harry",
      "Potter",
      "passport",
      "SX12345",
      "abc@gmail.com",
      "LHR",
      "",
      LocalDate.parse("2018-11-12"),
      parseLocalTime("12:20 pm")
    )

    lazy val dt: LocalDateTime = LocalDateTime.parse("2018-11-23T06:21:00")

    lazy val oldAlcohol: PurchasedProductInstance                         = PurchasedProductInstance(
      ProductPath("alcohol/beer"),
      "iid0",
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
    )
    lazy val oldPurchasedProductInstances: List[PurchasedProductInstance] = List(oldAlcohol)
    lazy val calculation: Calculation                                     = Calculation("1.00", "1.00", "1.00", "3.00")
    lazy val liabilityDetails: LiabilityDetails                           = LiabilityDetails("32.0", "0.0", "126.4", "158.40")
    lazy val declarationResponse: DeclarationResponse                     = DeclarationResponse(
      calculation = calculation,
      oldPurchaseProductInstances = oldPurchasedProductInstances,
      liabilityDetails = liabilityDetails
    )
    lazy val deltaCalculation: Calculation                                = Calculation("1.00", "1.00", "1.00", "3.00")
    lazy val zeroDeltaCalculation: Calculation                            = Calculation("0.00", "0.00", "0.00", "0.00")

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(
        injected[PurchasedProductService].removePurchasedProductInstance(any(), any())(any(), any())
      ) thenReturn Future.successful(JourneyData())
      when(injected[UserInformationService].storeUserInformation(any(), any())(any(), any())) thenReturn Future
        .successful(JourneyData())
      when(injected[Cache].fetch(any())) thenReturn cachedJourneyData
      when(
        injected[PayApiService].requestPaymentUrl(any(), any(), any(), any(), any(), any(), any())(any(), any())
      ) thenReturn Future.successful(payApiResponse)
      when(injected[TravelDetailsService].storeIrishBorder(any())(any())(any())) thenReturn Future.successful(
        Some(JourneyData())
      )
      when(
        injected[DeclarationService].submitDeclaration(any(), any(), any(), any(), any())(any(), any())
      ) thenReturn Future.successful(declarationServiceResponse)
      when(
        injected[DeclarationService].submitAmendment(any(), any(), any(), any(), any())(any(), any())
      ) thenReturn Future.successful(declarationServiceResponse)
      when(injected[DeclarationService].storeChargeReference(any(), any(), any())(any())) thenReturn Future.successful(
        JourneyData()
      )
      when(injected[DateTimeProviderService].now) thenReturn dt
      when(injected[CalculatorService].calculate(any())(any(), any())) thenReturn Future.successful(
        CalculatorServiceSuccessResponse(
          CalculatorResponse(
            None,
            None,
            None,
            Calculation("0.00", "0.00", "0.00", "0.00"),
            withinFreeAllowance = true,
            Map.empty,
            isAnyItemOverAllowance = false
          )
        )
      )
      when(injected[CalculatorService].storeCalculatorResponse(any(), any(), any())(any())) thenReturn Future
        .successful(JourneyData())

      rt(app, req)
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods when there is no journey data" should {
    def test(page: String, isAmendmentsEnabled: Boolean, locationRoute: String): Unit =
      s"redirect to the $page page" in new LocalSetup {
        override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(None)
        override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
        override lazy val declarationServiceResponse: DeclarationServiceResponse =
          DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

        val app: Application = GuiceApplicationBuilder()
          .configure(
            "features.amendments" -> isAmendmentsEnabled
          )
          .build()

        val response: Future[Result] = route(
          app,
          enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")
        ).get

        status(response)           shouldBe SEE_OTHER
        redirectLocation(response) shouldBe Some(s"/check-tax-on-goods-you-bring-into-the-uk/$locationRoute")
      }

    val input: Seq[(String, Boolean, String)] = Seq(
      ("EU country check", false, "where-goods-bought"),
      ("previous declaration", true, "previous-declaration")
    )

    input.foreach(args => (test _).tupled(args))
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods when tax amount is 0.00" should {

    "Display the previous-declaration page" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crZero)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/user-information when there is no journey data" should {
    def test(page: String, isAmendmentsEnabled: Boolean, locationRoute: String): Unit =
      s"redirect to the $page page" in new LocalSetup {
        override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(None)
        override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
        override lazy val declarationServiceResponse: DeclarationServiceResponse =
          DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

        val app: Application = GuiceApplicationBuilder()
          .configure(
            "features.amendments" -> isAmendmentsEnabled
          )
          .build()

        val response: Future[Result] = route(
          app,
          enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        ).get

        status(response)           shouldBe SEE_OTHER
        redirectLocation(response) shouldBe Some(s"/check-tax-on-goods-you-bring-into-the-uk/$locationRoute")
      }

    val input: Seq[(String, Boolean, String)] = Seq(
      ("EU country check", false, "where-goods-bought"),
      ("previous declaration", true, "previous-declaration")
    )

    input.foreach(args => (test _).tupled(args))
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/user-information when in amendment journey" should {

    "Display the previous-declaration page" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(true),
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            userInformation = Some(ui),
            calculatorResponse = Some(crWithinLimitLow)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }

    "Calling GET /check-tax-on-goods-you-bring-into-the-uk/tax-due when in pending payment journey" should {

      "Display the previous-declaration page" in new LocalSetup {

        override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(true),
              euCountryCheck = Some("nonEuOnly"),
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              amendState = Some("pending-payment")
            )
          )
        )
        override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
        override lazy val declarationServiceResponse: DeclarationServiceResponse =
          DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

        val response: Future[Result] =
          route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

        status(response)           shouldBe SEE_OTHER
        redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
      }
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/user-information when tax amount is 0.00" should {

    "Display the previous-declaration page" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crZero)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with tax greater than nine pounds and less than 90,000" should {

    "Display the declare-your-goods page when at the lower end of the range" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitLow)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Declare your goods"
    }

    "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with " +
      "tax greater than nine pounds and less than 90,000 in amendments journey" should {

        "Display the declare-your-goods page with process-amendment redirect url" in new LocalSetup {
          override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
            Some(
              JourneyData(
                prevDeclaration = Some(true),
                euCountryCheck = Some("nonEuOnly"),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false),
                calculatorResponse = Some(crWithinLimitLow),
                declarationResponse = Some(declarationResponse)
              )
            )
          )
          override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
          override lazy val declarationServiceResponse: DeclarationServiceResponse =
            DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

          val response: Future[Result] =
            route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

          val content: String = contentAsString(response)
          val doc: Document   = Jsoup.parse(content)

          doc.getElementsByTag("h1").text()             shouldBe "Amend your declaration"
          doc.getElementsByClass("govuk-button").text() shouldBe "Amend your declaration"
        }
      }

    "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with total tax " +
      "greater than 97000 pounds and amendment is less than 90,000 pounds in amendments journey" should {

        "Display the declare-your-goods page with process-amendment redirect url" in new LocalSetup {
          override lazy val deltaCalculation: Calculation                          = Calculation("70000.00", "0.00", "0.00", "70000.00")
          override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
            Some(
              JourneyData(
                prevDeclaration = Some(true),
                euCountryCheck = Some("nonEuOnly"),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false),
                calculatorResponse = Some(crAboveLimit),
                deltaCalculation = Some(deltaCalculation),
                declarationResponse = Some(declarationResponse)
              )
            )
          )
          override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
          override lazy val declarationServiceResponse: DeclarationServiceResponse =
            DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

          val response: Future[Result] =
            route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

          val content: String = contentAsString(response)
          val doc: Document   = Jsoup.parse(content)

          doc.getElementsByTag("h1").text()             shouldBe "Amend your declaration"
          doc.getElementsByClass("govuk-button").text() shouldBe "Amend your declaration"
        }
      }

    "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with " +
      "total tax is 0 pounds and amendment is 0 pound in amendments journey" should {

        "Display the declare-your-goods page with process-amendment redirect url for GBNI" in new LocalSetup {
          override lazy val deltaCalculation: Calculation                          = Calculation("0.00", "0.00", "0.00", "0.00")
          override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
            Some(
              JourneyData(
                prevDeclaration = Some(true),
                euCountryCheck = Some("greatBritain"),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false),
                calculatorResponse = Some(crZero),
                deltaCalculation = Some(deltaCalculation),
                declarationResponse = Some(declarationResponse)
              )
            )
          )
          override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
          override lazy val declarationServiceResponse: DeclarationServiceResponse =
            DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

          val response: Future[Result] =
            route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

          val content: String = contentAsString(response)
          val doc: Document   = Jsoup.parse(content)

          doc.getElementsByTag("h1").text()             shouldBe "Amend your declaration"
          doc.getElementsByClass("govuk-button").text() shouldBe "Amend your declaration"
        }
      }

    "Display the declare-your-goods page when at the higher end of the range" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitHigh)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Declare your goods"
    }

    "Display the Amend-your-declaration page when at the higher end of the range" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitHigh),
            declarationResponse = Some(declarationResponse)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Amend your declaration"
    }

    "display the Amend-your-declaration page, when tax to be paid is zero in amendment journey" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crZero),
            chargeReference = Some("XJPR5768524625"),
            userInformation = Some(ui),
            declarationResponse = Some(declarationResponse),
            deltaCalculation = Some(zeroDeltaCalculation)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceSuccessResponse                  =
        PayApiServiceSuccessResponse("http://example.com/payment-journey")
      override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Amend your declaration"
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/user-information with tax greater than nine pounds and less than 90,000" should {

    "Display the user-information page when at the lower end of the range" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitLow)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Enter your details"
    }

    "Display the user-information page when at the higher end of the range" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitHigh)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Enter your details"
    }

    "Display the where-goods-bought page when at the lower end of the range from GB to NI" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitLow)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Enter your details"
    }

    "populate user-information page if user-information data is present in db" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            userInformation = Some(ui),
            calculatorResponse = Some(crWithinLimitLow)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                                    shouldBe "Enter your details"
      doc.getElementById("firstName").`val`()                              shouldBe "Harry"
      doc.getElementById("lastName").`val`()                               shouldBe "Potter"
      doc.getElementById("identification.identificationNumber").`val`()    shouldBe "SX12345"
      doc.getElementById("emailAddress.email").`val`()                     shouldBe "abc@gmail.com"
      doc.getElementById("dateTimeOfArrival.dateOfArrival.day").`val`()    shouldBe "12"
      doc.getElementById("dateTimeOfArrival.dateOfArrival.month").`val`()  shouldBe "11"
      doc.getElementById("dateTimeOfArrival.dateOfArrival.year").`val`()   shouldBe "2018"
      doc.getElementById("dateTimeOfArrival.timeOfArrival.hour").`val`()   shouldBe "12"
      doc.getElementById("dateTimeOfArrival.timeOfArrival.minute").`val`() shouldBe "20"
      doc
        .getElementById("dateTimeOfArrival.timeOfArrival.halfday")
        .getElementsByAttribute("selected")
        .`val`()                                                           shouldBe "pm"
    }

    "populate user-information page if user-information data is present in db for GB NI journey" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("greatBritain"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            userInformation = Some(ui),
            calculatorResponse = Some(crWithinLimitLow)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                                    shouldBe "Enter your details"
      doc.getElementById("firstName").`val`()                              shouldBe "Harry"
      doc.getElementById("lastName").`val`()                               shouldBe "Potter"
      doc.getElementById("identification.identificationNumber").`val`()    shouldBe "SX12345"
      doc.getElementById("emailAddress.email").`val`()                     shouldBe "abc@gmail.com"
      doc.getElementById("dateTimeOfArrival.dateOfArrival.day").`val`()    shouldBe "12"
      doc.getElementById("dateTimeOfArrival.dateOfArrival.month").`val`()  shouldBe "11"
      doc.getElementById("dateTimeOfArrival.dateOfArrival.year").`val`()   shouldBe "2018"
      doc.getElementById("dateTimeOfArrival.timeOfArrival.hour").`val`()   shouldBe "12"
      doc.getElementById("dateTimeOfArrival.timeOfArrival.minute").`val`() shouldBe "20"
      doc
        .getElementById("dateTimeOfArrival.timeOfArrival.halfday")
        .getElementsByAttribute("selected")
        .`val`()                                                           shouldBe "pm"
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with tax greater than £90,000" should {

    "Display the previous-declaration page" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crAboveLimit)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/user-information with tax greater £90,000" should {

    "Display the previous-declaration page" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crAboveLimit)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

  "Calling POST /check-tax-on-goods-you-bring-into-the-uk/enter-details" should {

    "Return BAD REQUEST and display the user information form when invalid form input is sent" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "LHR",
            "placeOfArrival.enterPlaceOfArrival"      -> "",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when first name is too long" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "123456789012345678901234567890123451234",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "LHR",
            "placeOfArrival.enterPlaceOfArrival"      -> "",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when last name is too long" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "123456789012345678901234567890123451234",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "LHR",
            "placeOfArrival.enterPlaceOfArrival"      -> "",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when identification number is too long" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "12345678901234567890123456789012345612345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when identification number is not in correct format" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "telephone",
            "identification.identificationNumber"     -> "abcdefgh",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when place of arrival is too long" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "123456789012345678901234567890123456123456",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when only email address is entered" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "123456789012345678901234567890123456123456",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when email address and confirm email address do not match" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "xyz@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "123456789012345678901234567890123456123456",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when invalid telephone num ber is entered" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "telephone",
            "identification.identificationNumber"     -> "abcdefghi",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when the date is invalid" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitLow)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "LHR",
            "placeOfArrival.enterPlaceOfArrival"      -> "",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "abc",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when date entered is in past" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "LHR",
            "placeOfArrival.enterPlaceOfArrival"      -> "",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "2",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "am"
          )
      ).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc
        .getElementById("dateTimeOfArrival-error")
        .text() shouldBe "Error: Scheduled date and time of arrival must be less than 5 days in the future"
    }

    "Redirect to next page when the schedule time of arrival is 2 hours in past" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitLow),
            userInformation = Some(ui)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceSuccessResponse                  =
        PayApiServiceSuccessResponse("http://example.com/payment-journey")
      override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "04",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "am"
          )
      ).get

      status(response)               shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe "http://example.com/payment-journey"

    }

    "Return INTERNAL_SERVER_ERROR but still store valid user information, when the payment service request fails" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitLow),
            userInformation = Some(ui)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse = DeclarationServiceFailureResponse

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "passport",
            "identification.identificationNumber"     -> "SX12345",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "LHR",
            "placeOfArrival.enterPlaceOfArrival"      -> "",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
    }

    "Return Hint Text When telephone number is entered" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "telephone",
            "identification.identificationNumber"     -> "08765432190",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc
        .getElementById("conditional-identificationType-telephone")
        .text() shouldBe "For international numbers this will need to include the country code, for example +33 for France."
    }

    "Return Inset Text When euId is entered" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "euId",
            "identification.identificationNumber"     -> "ABC3456",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc
        .getElementsByClass("govuk-inset-text")
        .text() shouldBe "You can use this number as identification for your declaration, but you may not be able to use an EU ID card to enter the UK. " +
        "Check the latest rules prior to your arrival in the UK (opens in a new tab)."
    }

    "Cache the submitted user information and redirect payment url when valid form " +
      "input is sent and the payment service request is successful" in new LocalSetup {

        override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("nonEuOnly"),
              arrivingNICheck = Some(true),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              calculatorResponse = Some(crWithinLimitLow),
              userInformation = Some(ui)
            )
          )
        )
        override lazy val payApiResponse: PayApiServiceSuccessResponse                  =
          PayApiServiceSuccessResponse("http://example.com/payment-journey")
        override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
          DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

        val response: Future[Result] = route(
          app,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
            .withFormUrlEncodedBody(
              "firstName"                               -> "Harry",
              "lastName"                                -> "Potter",
              "identification.identificationType"       -> "passport",
              "identification.identificationNumber"     -> "SX12345",
              "emailAddress.email"                      -> "abc@gmail.com",
              "emailAddress.confirmEmail"               -> "abc@gmail.com",
              "placeOfArrival.selectPlaceOfArrival"     -> "",
              "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
              "dateTimeOfArrival.dateOfArrival.day"     -> "23",
              "dateTimeOfArrival.dateOfArrival.month"   -> "11",
              "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
              "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
              "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
              "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
            )
        ).get

        status(response)               shouldBe SEE_OTHER
        redirectLocation(response).get shouldBe "http://example.com/payment-journey"

        verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
      }

    "Cache the submitted user information and redirect payment url when valid form input " +
      "is sent and the payment service request is successful from GB to NI" in new LocalSetup {

        override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("greatBritain"),
              arrivingNICheck = Some(true),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              calculatorResponse = Some(crWithinLimitLow),
              chargeReference = Some("XJPR5768524625"),
              userInformation = Some(ui)
            )
          )
        )
        override lazy val payApiResponse: PayApiServiceSuccessResponse                  =
          PayApiServiceSuccessResponse("http://example.com/payment-journey")
        override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
          DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

        val response: Future[Result] = route(
          app,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
            .withFormUrlEncodedBody(
              "firstName"                               -> "Harry",
              "lastName"                                -> "Potter",
              "identification.identificationType"       -> "telephone",
              "identification.identificationNumber"     -> "07884559563",
              "emailAddress.email"                      -> "abc@gmail.com",
              "emailAddress.confirmEmail"               -> "abc@gmail.com",
              "placeOfArrival.selectPlaceOfArrival"     -> "",
              "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
              "dateTimeOfArrival.dateOfArrival.day"     -> "23",
              "dateTimeOfArrival.dateOfArrival.month"   -> "11",
              "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
              "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
              "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
              "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
            )
        ).get

        status(response)               shouldBe SEE_OTHER
        redirectLocation(response).get shouldBe "http://example.com/payment-journey"

        verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
      }
  }

  "Calling /check-tax-on-goods-you-bring-into-the-uk/process-amendment" should {
    "Return INTERNAL_SERVER_ERROR, when amendment submission is failed" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(true),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitLow),
            userInformation = Some(ui),
            deltaCalculation = Some(deltaCalculation),
            declarationResponse = Some(declarationResponse)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceResponse = DeclarationServiceFailureResponse

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/process-amendment")).get

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "Return INTERNAL_SERVER_ERROR, when the payment service request fails" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(true),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitLow),
            userInformation = Some(ui),
            deltaCalculation = Some(deltaCalculation),
            declarationResponse = Some(declarationResponse)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                         = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/process-amendment")).get

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "fetch user information from journey data and redirect to Declaration complete page, when tax to be paid is zero in amendment journey" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(true),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crZero),
            chargeReference = Some("XJPR5768524625"),
            userInformation = Some(ui),
            declarationResponse = Some(declarationResponse),
            deltaCalculation = Some(zeroDeltaCalculation)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceSuccessResponse                  =
        PayApiServiceSuccessResponse("http://example.com/payment-journey")
      override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/process-amendment")).get

      status(response)               shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete"
    }

    "fetch user information from journey data and redirect to payment url, when payment service request is successful in amendments journey" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(true),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crWithinLimitLow),
            userInformation = Some(ui),
            deltaCalculation = Some(deltaCalculation),
            declarationResponse = Some(declarationResponse)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceSuccessResponse                  =
        PayApiServiceSuccessResponse("http://example.com/payment-journey")
      override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/process-amendment")).get

      status(response)               shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe "http://example.com/payment-journey"
    }

    "fetch user information from journey data and redirect to payment url, " +
      "when payment service request is successful from GB to NI in amendment journey" in new LocalSetup {

        override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(true),
              euCountryCheck = Some("greatBritain"),
              arrivingNICheck = Some(true),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              calculatorResponse = Some(crWithinLimitLow),
              userInformation = Some(ui),
              deltaCalculation = Some(deltaCalculation),
              declarationResponse = Some(declarationResponse)
            )
          )
        )
        override lazy val payApiResponse: PayApiServiceSuccessResponse                  =
          PayApiServiceSuccessResponse("http://example.com/payment-journey")
        override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
          DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

        val response: Future[Result] =
          route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/process-amendment")).get

        status(response)               shouldBe SEE_OTHER
        redirectLocation(response).get shouldBe "http://example.com/payment-journey"
      }

    "fetch user information from journey data and redirect to payment url, " +
      "when payment service request is successful in pending payment journey" in new LocalSetup {

        override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(true),
              euCountryCheck = Some("nonEuOnly"),
              arrivingNICheck = Some(true),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              calculatorResponse = Some(crWithinLimitLow),
              userInformation = Some(ui),
              deltaCalculation = Some(deltaCalculation),
              declarationResponse = Some(declarationResponse),
              amendState = Some("pending-payment")
            )
          )
        )
        override lazy val payApiResponse: PayApiServiceSuccessResponse                  =
          PayApiServiceSuccessResponse("http://example.com/payment-journey")
        override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
          DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))
        when(injected[CalculatorService].getPreviousPaidCalculation(any(), any())) thenReturn Calculation(
          "0.00",
          "0.00",
          "0.00",
          "0.00"
        )

        val response: Future[Result] =
          route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/process-amendment")).get

        status(response)               shouldBe SEE_OTHER
        redirectLocation(response).get shouldBe "http://example.com/payment-journey"
      }

    "throw an RuntimeException if no user information is supplied" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(true),
            euCountryCheck = Some("nonEuOnly"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceSuccessResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse = None.orNull

      intercept[RuntimeException](
        await(
          route(
            app,
            enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/process-amendment")
          ).get
        )
      ).getMessage shouldBe "no user Information"
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods when calculator response is null and eu country check is null " should {

    "Display the previous-declaration page when tax and eu country check are null " in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         =
        Future.successful(Some(JourneyData(euCountryCheck = None, calculatorResponse = None)))
      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get
      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration"
    }

    "Display the previous-declaration page when calculatorResponse is null" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         =
        Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), calculatorResponse = None)))
      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get
      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration"
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with 0 tax " should {

    "Display the declare-your-goods page when tax is 0" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            calculatorResponse = Some(crZero)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get
      status(response) shouldBe OK
      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Declare your goods"
      doc.text()                          should include("these goods are for my own use or to give away as a gift")
      doc.text()                          should include("I must pay duty and tax on these goods if I bring them into the UK")
    }

    "Cache the submitted user information and redirect to Declaration page when tax to be paid is zero" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            isVatResClaimed = None,
            isBringingDutyFree = None,
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false),
            calculatorResponse = Some(crZero),
            chargeReference = Some("XJPR5768524625"),
            userInformation = Some(ui)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceSuccessResponse                  =
        PayApiServiceSuccessResponse("http://example.com/payment-journey")
      override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse =
        DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
          .withFormUrlEncodedBody(
            "firstName"                               -> "Harry",
            "lastName"                                -> "Potter",
            "identification.identificationType"       -> "telephone",
            "identification.identificationNumber"     -> "07884559563",
            "emailAddress.email"                      -> "abc@gmail.com",
            "emailAddress.confirmEmail"               -> "abc@gmail.com",
            "placeOfArrival.selectPlaceOfArrival"     -> "",
            "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
            "dateTimeOfArrival.dateOfArrival.day"     -> "23",
            "dateTimeOfArrival.dateOfArrival.month"   -> "11",
            "dateTimeOfArrival.dateOfArrival.year"    -> "2018",
            "dateTimeOfArrival.timeOfArrival.hour"    -> "12",
            "dateTimeOfArrival.timeOfArrival.minute"  -> "00",
            "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
          )
      ).get

      status(response)               shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete"

      verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
    }

  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with 0 tax in amendments journey" should {

    "Display the declare-your-goods page when tax is 0 with process-amendment redirect url" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(true),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            calculatorResponse = Some(crZero),
            declarationResponse = Some(declarationResponse)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get
      status(response) shouldBe OK
      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()             shouldBe "Amend your declaration"
      doc.text()                                      should include("these goods are for my own use or to give away as a gift")
      doc.text()                                      should include("I must pay duty and tax on these goods if I bring them into the UK")
      doc.getElementsByClass("govuk-button").text() shouldBe "Amend your declaration"
    }
  }

  "calling GET .../ireland-to-northern-ireland" should {

    "return the ireland to northern ireland page unpopulated if there is no ireland answer in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(None)
      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")
      ).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.select("#irishBorder-true").hasAttr("checked")  shouldBe false
      doc.select("#irishBorder-false").hasAttr("checked") shouldBe false

      verify(injected[Cache], times(1)).fetch(any())
    }

    "return the ireland to northern ireland page pre-populated yes if there is ireland answer true in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(JourneyData(privateCraft = Some(true), ageOver17 = Some(true), irishBorder = Some(true)))
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")
      ).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.select("#irishBorder-value-yes").hasAttr("checked") shouldBe true
      doc.select("#irishBorder-value-no").hasAttr("checked")  shouldBe false

      verify(injected[Cache], times(1)).fetch(any())
    }

    "return the ireland to northern ireland page pre-populated no if there is ireland answer false in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(JourneyData(privateCraft = Some(false), ageOver17 = Some(false), irishBorder = Some(false)))
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")
      ).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.select("#irishBorder-value-no").hasAttr("checked")  shouldBe true
      doc.select("#irishBorder-value-yes").hasAttr("checked") shouldBe false

      verify(injected[Cache], times(1)).fetch(any())
    }

  }

  "Calling POST .../ireland-to-northern-ireland" should {

    "redirect to /check-tax-on-goods-you-bring-into-the-uk/tax-due" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]]         =
        Future.successful(Some(JourneyData(irishBorder = Some(false), privateCraft = Some(false))))
      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")
          .withFormUrlEncodedBody("irishBorder" -> "true")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tax-due")

      verify(injected[TravelDetailsService], times(1)).storeIrishBorder(any())(meq(true))(any())
    }

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")
          .withFormUrlEncodedBody("value" -> "badValue")
      ).get

      status(response) shouldBe BAD_REQUEST

      verify(injected[TravelDetailsService], times(0)).storeIrishBorder(any())(any())(any())

    }

    "return top error summary box when trying to submit a blank form" in new LocalSetup {

      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")
      ).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      Option(doc.select("a[href=#irishBorder-error]")).isEmpty shouldBe false
      doc
        .select("a[href=#irishBorder-value-yes]")
        .text()                                                shouldBe "Select yes if you are entering Northern Ireland from Ireland"
      doc.select(".govuk-error-summary__title").text()         shouldBe "There is a problem"

    }

    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")
      ).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)
      doc
        .getElementById("irishBorder-error")
        .html() shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Select yes if you are entering Northern Ireland from Ireland"
    }

  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/cannot-use-service" should {
    "return the purchase price out of bound page" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Option[JourneyData]]         = Future.successful(
        Some(
          JourneyData(
            euCountryCheck = Some("nonEuOnly"),
            bringingOverAllowance = Some(true),
            ageOver17 = Some(true),
            privateCraft = Some(false)
          )
        )
      )
      override lazy val payApiResponse: PayApiServiceResponse                  = None.orNull
      override lazy val declarationServiceResponse: DeclarationServiceResponse = None.orNull

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/cannot-use-service")
      ).get
      val content: String          = contentAsString(response)
      val doc: Document            = Jsoup.parse(content)

      status(response)      shouldBe OK
      doc.select("h1").text shouldBe "You cannot use this service"
    }
  }

  "Calling POST /check-tax-on-goods-you-bring-into-the-uk/tell-us" should {
    "redirect to the tax due page" when {
      "CalculatorService returns CalculatorServiceSuccessResponse" in new LocalSetup {
        override lazy val cachedJourneyData: Future[Option[JourneyData]]                = Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("greatBritain"),
              arrivingNICheck = Some(true),
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false)
            )
          )
        )
        override lazy val payApiResponse: PayApiServiceSuccessResponse                  = None.orNull
        override lazy val declarationServiceResponse: DeclarationServiceSuccessResponse = None.orNull

        val response: Future[Result] = route(
          app,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")
        ).get

        status(response)               shouldBe SEE_OTHER
        redirectLocation(response).get shouldBe "/check-tax-on-goods-you-bring-into-the-uk/tax-due"
      }
    }

    "redirect to the cannot use service page" when {
      "CalculatorService returns CalculatorServicePurchasePriceOutOfBoundsFailureResponse" in {
        val cachedJourneyData: Future[Option[JourneyData]] = Future.successful(
          Some(
            JourneyData(
              euCountryCheck = Some("nonEuOnly"),
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false)
            )
          )
        )

        when(injected[Cache].fetch(any())).thenReturn(cachedJourneyData)

        when(injected[CalculatorService].calculate(any())(any(), any())).thenReturn(
          Future.successful(
            CalculatorServicePurchasePriceOutOfBoundsFailureResponse
          )
        )

        val response: Future[Result] = injected[CalculateDeclareController].calculate(
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")
        )

        status(response)               shouldBe SEE_OTHER
        redirectLocation(response).get shouldBe "/check-tax-on-goods-you-bring-into-the-uk/cannot-use-service"
      }
    }

    "load the error template page" when {
      "CalculatorService returns CalculatorServiceCantBuildCalcReqResponse" in {
        val cachedJourneyData: Future[Option[JourneyData]] = Future.successful(
          Some(
            JourneyData(
              euCountryCheck = Some("nonEuOnly"),
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false)
            )
          )
        )

        when(injected[Cache].fetch(any())).thenReturn(cachedJourneyData)

        when(injected[CalculatorService].calculate(any())(any(), any())).thenReturn(
          Future.successful(
            CalculatorServiceCantBuildCalcReqResponse
          )
        )

        val response: Future[Result] = injected[CalculateDeclareController].calculate(
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")
        )
        val content: String          = contentAsString(response)
        val doc: Document            = Jsoup.parse(content)

        status(response)      shouldBe INTERNAL_SERVER_ERROR
        doc.select("h1").text shouldBe "Sorry, there is a problem with the service"
      }
    }
  }
}
