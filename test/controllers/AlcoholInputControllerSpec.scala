/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _} //TODO
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.MockitoSugar
import play.api.Application
import play.api.data.Form
import play.api.http.Writeable
import play.api.test.Helpers.{route => rt, _}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import play.twirl.api.Html
import repositories.BCPassengersSessionRepository
import services.{CalculatorService, LimitUsageSuccessResponse, NewPurchaseService}
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import views.html.alcohol.alcohol_input

import scala.concurrent.Future

class AlcoholInputControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[NewPurchaseService].toInstance(MockitoSugar.mock[NewPurchaseService]))
    .overrides(bind[CalculatorService].toInstance(MockitoSugar.mock[CalculatorService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[alcohol_input].toInstance(MockitoSugar.mock[alcohol_input]))
    .build()

  override def beforeEach: Unit =
    reset(injected[Cache], injected[NewPurchaseService], injected[alcohol_input])

  trait LocalSetup {

    lazy val cachedJourneyData: Option[JourneyData] = Some(
      JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(
          PurchasedProductInstance(
            ProductPath("alcohol/beer"),
            "iid0",
            Some(20.0),
            None,
            Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
            Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, Nil)),
            Some("EUR"),
            Some(BigDecimal(12.99))
          )
        )
      )
    )

    lazy val cachedGBNIJourneyData: Option[JourneyData] = Some(
      JourneyData(
        prevDeclaration = Some(false),
        Some("greatBritain"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(
          PurchasedProductInstance(
            ProductPath("alcohol/beer"),
            "iid0",
            Some(20.0),
            None,
            Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
            None,
            Some("EUR"),
            Some(BigDecimal(12.99))
          )
        )
      )
    )

    lazy val cachedEUGBJourneyData: Option[JourneyData] = Some(
      JourneyData(
        prevDeclaration = Some(false),
        Some("euOnly"),
        arrivingNICheck = Some(false),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(
          PurchasedProductInstance(
            ProductPath("alcohol/beer"),
            "iid0",
            Some(20.0),
            None,
            Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
            None,
            Some("EUR"),
            Some(BigDecimal(12.99))
          )
        )
      )
    )

    val formCaptor: ArgumentCaptor[Form[AlcoholDto]] = ArgumentCaptor.forClass(classOf[Form[AlcoholDto]])

    def fakeLimits: Map[String, String]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())

      when(injected[CalculatorService].limitUsage(any())(any())) thenReturn Future.successful(
        LimitUsageSuccessResponse(fakeLimits)
      )
      val insertedPurchase = (cachedJourneyData.get, "pid")
      when(
        injected[NewPurchaseService].insertPurchases(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ) thenReturn insertedPurchase
      when(
        injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ) thenReturn cachedJourneyData.get

      when(
        injected[alcohol_input].apply(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ) thenReturn Html("")

      rt(app, req)
    }

    def gbNIRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedGBNIJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())

      when(injected[CalculatorService].limitUsage(any())(any())) thenReturn Future.successful(
        LimitUsageSuccessResponse(fakeLimits)
      )
      val insertedPurchase = (cachedGBNIJourneyData.get, "pid")
      when(
        injected[NewPurchaseService].insertPurchases(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ) thenReturn insertedPurchase
      when(
        injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ) thenReturn cachedGBNIJourneyData.get

      when(
        injected[alcohol_input].apply(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ) thenReturn Html("")

      rt(app, req)
    }

    def euGBRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedEUGBJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())

      when(injected[CalculatorService].limitUsage(any())(any())) thenReturn Future.successful(
        LimitUsageSuccessResponse(fakeLimits)
      )
      val insertedPurchase = (cachedEUGBJourneyData.get, "pid")
      when(
        injected[NewPurchaseService].insertPurchases(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ) thenReturn insertedPurchase
      when(
        injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ) thenReturn cachedEUGBJourneyData.get

      when(
        injected[alcohol_input].apply(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ) thenReturn Html("")

      rt(app, req)
    }
  }

  "Getting displayEditForm" should {

    "return a 404 when given an invalid iid" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/missing-iid/edit")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 500 when purchase is missing country" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          isVatResClaimed = None,
          isBringingDutyFree = None,
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid0",
              Some(20.0),
              None,
              None,
              None,
              Some("EUR"),
              Some(BigDecimal(12.99))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when missing currency" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          isVatResClaimed = None,
          isBringingDutyFree = None,
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid0",
              Some(20.0),
              None,
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              None,
              Some(BigDecimal(12.99))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when missing weightOrVolume" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          isVatResClaimed = None,
          isBringingDutyFree = None,
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid0",
              None,
              None,
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              Some("EUR"),
              Some(BigDecimal(12.99))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 404 when purchase has invalid product path" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          isVatResClaimed = None,
          isBringingDutyFree = None,
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              ProductPath("invalid/product/path"),
              "iid0",
              None,
              None,
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              Some("EUR"),
              Some(BigDecimal(12.99))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when all is ok" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
      ).get
      status(result) shouldBe OK
    }

    "redirect to previous-declaration page when amendState = pending-payment set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits                             = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")
      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          amendState = Some("pending-payment")
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

  "Getting displayAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/invalid/path/tell-us")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when given a valid path" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
      ).get
      status(result) shouldBe OK
    }

    "display default country and currency if set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          isVatResClaimed = None,
          isBringingDutyFree = None,
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid0",
              Some(20.0),
              None,
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              Some("EUR"),
              Some(BigDecimal(12.99))
            )
          ),
          defaultCountry = Some("FR"),
          defaultCurrency = Some("EUR")
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[views.html.alcohol.alcohol_input], times(1))(
        formCaptor.capture(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )(any(), any(), any())

      formCaptor.getValue.data("country")  shouldBe "FR"
      formCaptor.getValue.data("currency") shouldBe "EUR"
    }

    "display default origin country if set in euOnly JourneyData" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("euOnly"),
          arrivingNICheck = Some(true),
          isVatResClaimed = None,
          isBringingDutyFree = None,
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid0",
              Some(20.0),
              None,
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, Nil)),
              Some("EUR"),
              Some(BigDecimal(12.99))
            )
          ),
          defaultCountry = Some("FR"),
          defaultOriginCountry = Some("IT"),
          defaultCurrency = Some("EUR")
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[views.html.alcohol.alcohol_input], times(1))(
        formCaptor.capture(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )(any(), any(), any())

      formCaptor.getValue.data("originCountry") shouldBe "IT"
    }

    "not display default country and currency if not set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          isVatResClaimed = None,
          isBringingDutyFree = None,
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid0",
              Some(20.0),
              None,
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              Some("EUR"),
              Some(BigDecimal(12.99))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[views.html.alcohol.alcohol_input], times(1))(
        formCaptor.capture(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )(any(), any(), any())

      formCaptor.getValue.data("country")  shouldBe ""
      formCaptor.getValue.data("currency") shouldBe ""
    }

    "redirect to previous-declaration page when amendState = pending-payment set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          amendState = Some("pending-payment")
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")

    }
  }

  "Posting processAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/invalid/path/tell-us"
        )
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 400 when country not present" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "Not a real country",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "XXX",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not valid" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "invalid-cost"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not present" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"  -> "FR",
            "currency" -> "EUR",
            "cost"     -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not valid" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "invalid-volume",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when beer is over the calculator limit" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "111",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/beer/upper-limits"
      )
    }

    "redirect to warning page on more than allowance 60 litres in sparkling-wine" in new LocalSetup {

      override lazy val fakeLimits = Map("L-WINESP" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/sparkling-wine/tell-us"
      ).withFormUrlEncodedBody(
        "weightOrVolume" -> "65",
        "country"        -> "FR",
        "currency"       -> "EUR",
        "cost"           -> "50"
      )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/sparkling-wine/upper-limits"
      )
    }

    "redirect to warning page on more than allowance 90 litres in wine" in new LocalSetup {

      override lazy val fakeLimits = Map("L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/wine/tell-us")
          .withFormUrlEncodedBody(
            "weightOrVolume" -> "95",
            "country"        -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "50"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/wine/upper-limits"
      )
    }

    "add a PPI to the JourneyData and redirect to next step" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("alcohol/beer")),
        meq(Some(BigDecimal(20.0))),
        any(),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(List(BigDecimal(12.50))),
        any(),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "add a PPI to the JourneyData and redirect to UK VAT Paid page for GBNI Journey" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/pid/gb-ni-vat-check"
      )

    }

    "add a PPI to the JourneyData and redirect to Eu Evidence page for EUGB Journey where producedIn is an EU country" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "originCountry"  -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/pid/eu-evidence-check"
      )

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a non-EU country" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "originCountry"  -> "IN",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn has a null value" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }
  }

  "Posting processEditForm" should {

    "return a 404 when iid is not found in journey data" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/missing-iid/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND

    }

    "return a 400 when beer is over the calculator limit" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "111",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/beer/upper-limits"
      )
    }

    "modify the relevant PPI in the JourneyData and redirect to next step" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "13.0",
            "cost"           -> "50.00"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).updatePurchase(
        meq(ProductPath("alcohol/beer")),
        meq("iid0"),
        meq(Some(BigDecimal(13.0))),
        any(),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(BigDecimal(50.00)),
        any()
      )(any())

      verify(injected[Cache], times(2)).store(any())(any())
    }

    "modify the relevant PPI in the JourneyData and redirect to UK VAT Paid page for GBNI Journey" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "13.0",
            "cost"           -> "50.00"
          )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/iid0/gb-ni-vat-check"
      )

    }

    "add a PPI to the JourneyData and redirect to Eu Evidence page for EUGB Journey where producedIn is an EU country" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "originCountry"  -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/iid0/eu-evidence-check"
      )

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a non-EU country" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "originCountry"  -> "IN",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn has a null value" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "20.0",
            "cost"           -> "12.50"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }
  }
}
