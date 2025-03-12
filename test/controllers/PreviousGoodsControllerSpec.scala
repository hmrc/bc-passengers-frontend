/*
 * Copyright 2025 HM Revenue & Customs
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
import models.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, times, verify, when}
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route as rt, *}
import repositories.BCPassengersSessionRepository
import services.{CalculatorService, PurchasedProductService}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class PreviousGoodsControllerSpec extends BaseSpec {
  val mockCache: Cache = mock(classOf[Cache])

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[PurchasedProductService].toInstance(mock(classOf[PurchasedProductService])))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[CalculatorService].toInstance(mock(classOf[CalculatorService])))
    .build()

  override def beforeEach(): Unit = {
    reset(injected[Cache])
    reset(injected[PurchasedProductService])
    reset(injected[Cache])
    reset(injected[CalculatorService])
  }

  trait LocalSetup {

    def travelDetailsJourneyData: JourneyData = JourneyData(
      prevDeclaration = Some(false),
      euCountryCheck = Some("nonEuOnly"),
      arrivingNICheck = Some(true),
      isVatResClaimed = None,
      isBringingDutyFree = None,
      bringingOverAllowance = Some(true),
      ageOver17 = Some(true),
      privateCraft = Some(false)
    )
    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(
        injected[PurchasedProductService].removePurchasedProductInstance(any(), any())(any(), any())
      ).thenReturn(Future.successful(JourneyData()))
      when(injected[Cache].fetch(any())).thenReturn(Future.successful(cachedJourneyData))
      rt(app, req)
    }
  }

  val controller: DashboardController = app.injector.instanceOf[DashboardController]

  "Calling GET .../previous-goods" should {
    "redirect to start if travel details are missing" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(privateCraft = None))

      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/previous-goods")).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")

      verify(controller.cache, times(1)).fetch(any())
    }

    "redirect to pending-payment if the amendState equals pending-payment in JourneyData" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData] =
        Some(travelDetailsJourneyData.copy(amendState = Some("pending-payment")))

      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/previous-goods")).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")

      verify(controller.cache, times(1)).fetch(any())
    }

    "respond with 200, display the page if all the travel details exist & display button's text for Amendment:Add goods " in new LocalSetup {
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

      val alcohol: PurchasedProductInstance = PurchasedProductInstance(
        ProductPath("alcohol/beer"),
        "iid0",
        Some(1.54332),
        None,
        Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
        Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
        Some("AUD"),
        Some(BigDecimal(10.234)),
        None,
        Some(true),
        None,
        Some(true),
        None
      )
      val tobacco: PurchasedProductInstance = PurchasedProductInstance(
        ProductPath("tobacco/cigarettes"),
        "iid0",
        Some(1.54332),
        None,
        Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
        Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
        Some("AUD"),
        Some(BigDecimal(10.234)),
        None,
        Some(true),
        None,
        Some(true),
        None
      )
      val other: PurchasedProductInstance   = PurchasedProductInstance(
        ProductPath("other-goods/antiques"),
        "iid1",
        None,
        None,
        Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
        Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
        Some("AUD"),
        Some(5432),
        Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))),
        Some(true),
        None,
        None,
        Some(true)
      )

      override val cachedJourneyData: Option[JourneyData] = Some(
        travelDetailsJourneyData.copy(
          euCountryCheck = Some("euOnly"),
          arrivingNICheck = Some(false),
          purchasedProductInstances = List(alcohol, tobacco, other),
          declarationResponse = Some(declarationResponse)
        )
      )

      val csr: CalculatorServiceRequest = CalculatorServiceRequest(
        isPrivateCraft = false,
        isAgeOver17 = false,
        isArrivingNI = false,
        List(
          PurchasedItem(
            purchasedProductInstance = alcohol,
            productTreeLeaf = ProductTreeLeaf("", "", "", "alcohol", List.empty),
            exchangeRate = ExchangeRate("", ""),
            currency = Currency("", "", None, List.empty),
            gbpCost = BigDecimal(10)
          ),
          PurchasedItem(
            purchasedProductInstance = tobacco,
            productTreeLeaf = ProductTreeLeaf("", "", "", "tobacco", List.empty),
            exchangeRate = ExchangeRate("", ""),
            currency = Currency("", "", None, List.empty),
            gbpCost = BigDecimal(10)
          ),
          PurchasedItem(
            purchasedProductInstance = other,
            productTreeLeaf = ProductTreeLeaf("", "", "", "other-goods", List.empty),
            exchangeRate = ExchangeRate("", ""),
            currency = Currency("", "", None, List.empty),
            gbpCost = BigDecimal(10)
          )
        )
      )
      when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())).thenReturn(
        Future
          .successful(
            Some(csr)
          )
      )
      val result: Future[Result]        = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/previous-goods").withFormUrlEncodedBody(
          "firstName"      -> "Harry",
          "lastName"       -> "Potter",
          "passportNumber" -> "801375812",
          "placeOfArrival" -> "Newcastle airport"
        )
      ).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()             shouldBe "Your previously declared goods"
      doc.getElementsByClass("govuk-button").text() shouldBe "Add goods"
    }
  }

}
