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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, *}
import org.mockito.Mockito.*
import play.api.Application
import play.api.data.Form
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{route => rt, *}
import play.twirl.api.Html
import repositories.BCPassengersSessionRepository
import services.*
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import views.html.tobacco.*

import scala.concurrent.Future

class TobaccoInputControllerSpec extends BaseSpec {

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .overrides(bind[NewPurchaseService].toInstance(mock(classOf[NewPurchaseService])))
    .overrides(bind[CalculatorService].toInstance(mock(classOf[CalculatorService])))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[no_of_sticks_input].toInstance(mock(classOf[no_of_sticks_input])))
    .overrides(
      bind[weight_or_volume_input]
        .toInstance(mock(classOf[weight_or_volume_input]))
    )
    .overrides(
      bind[no_of_sticks_weight_or_volume_input]
        .toInstance(mock(classOf[no_of_sticks_weight_or_volume_input]))
    )
    .build()

  override def beforeEach(): Unit = {
    reset(injected[Cache])
    reset(injected[NewPurchaseService])
    reset(injected[no_of_sticks_input])
    reset(injected[Cache])
    reset(injected[NewPurchaseService])
    reset(injected[weight_or_volume_input])
    reset(injected[Cache])
    reset(injected[NewPurchaseService])
    reset(injected[no_of_sticks_weight_or_volume_input])
  }

  trait LocalSetup {

    def productPath: ProductPath
    def weightOrVolume: Option[BigDecimal]
    def noOfSticks: Option[Int]

    lazy val cachedJourneyData: Option[JourneyData] =
      Some(
        JourneyData(
          prevDeclaration = Some(false),
          euCountryCheck = Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          isVatResClaimed = Some(true),
          isBringingDutyFree = None,
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          selectedAliases = List(),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              productPath,
              "iid0",
              weightOrVolume,
              noOfSticks,
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              Some("EUR"),
              Some(BigDecimal(12.99))
            )
          )
        )
      )

    lazy val cachedGBNIJourneyData: Some[JourneyData] = Some(
      JourneyData(
        prevDeclaration = Some(false),
        Some("greatBritain"),
        arrivingNICheck = Some(true),
        isVatResClaimed = Some(true),
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(
          PurchasedProductInstance(
            productPath,
            "iid0",
            weightOrVolume,
            noOfSticks,
            Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
            None,
            Some("EUR"),
            Some(BigDecimal(12.99))
          )
        )
      )
    )

    lazy val cachedEUGBJourneyData: Some[JourneyData] = Some(
      JourneyData(
        prevDeclaration = Some(false),
        Some("euOnly"),
        arrivingNICheck = Some(false),
        isVatResClaimed = Some(true),
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(
          PurchasedProductInstance(
            productPath,
            "iid0",
            weightOrVolume,
            noOfSticks,
            Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
            None,
            Some("EUR"),
            Some(BigDecimal(12.99))
          )
        )
      )
    )

    def fakeLimits: Map[String, String]

    val formCaptor: ArgumentCaptor[Form[TobaccoDto]] = ArgumentCaptor.forClass(classOf[Form[TobaccoDto]])

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())).thenReturn(Future.successful(cachedJourneyData))
      when(injected[Cache].store(any())(any())).thenReturn(Future.successful(JourneyData()))

      when(injected[CalculatorService].limitUsage(any())(any())).thenReturn(
        Future.successful(
          LimitUsageSuccessResponse(fakeLimits)
        )
      )
      val insertedPurchase = (cachedJourneyData.get, "pid")
      when(
        injected[NewPurchaseService].insertPurchases(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ).thenReturn(insertedPurchase)
      when(
        injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ).thenReturn(cachedJourneyData.get)

      when(
        injected[no_of_sticks_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))
      when(
        injected[weight_or_volume_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))
      when(
        injected[no_of_sticks_weight_or_volume_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))

      rt(app, req)
    }

    def gbNIRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())).thenReturn(Future.successful(cachedGBNIJourneyData))
      when(injected[Cache].store(any())(any())).thenReturn(Future.successful(JourneyData()))

      when(injected[CalculatorService].limitUsage(any())(any())).thenReturn(
        Future.successful(
          LimitUsageSuccessResponse(fakeLimits)
        )
      )
      val insertedPurchase = (cachedGBNIJourneyData.get, "pid")
      when(
        injected[NewPurchaseService].insertPurchases(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ).thenReturn(insertedPurchase)
      when(
        injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ).thenReturn(cachedGBNIJourneyData.get)

      when(
        injected[no_of_sticks_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))
      when(
        injected[weight_or_volume_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))
      when(
        injected[no_of_sticks_weight_or_volume_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))

      rt(app, req)
    }

    def euGBRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())).thenReturn(Future.successful(cachedEUGBJourneyData))
      when(injected[Cache].store(any())(any())).thenReturn(Future.successful(JourneyData()))

      when(injected[CalculatorService].limitUsage(any())(any())).thenReturn(
        Future.successful(
          LimitUsageSuccessResponse(fakeLimits)
        )
      )
      val insertedPurchase = (cachedEUGBJourneyData.get, "pid")
      when(
        injected[NewPurchaseService].insertPurchases(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ).thenReturn(insertedPurchase)
      when(
        injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ).thenReturn(cachedEUGBJourneyData.get)

      when(
        injected[no_of_sticks_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))
      when(
        injected[weight_or_volume_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))
      when(
        injected[no_of_sticks_weight_or_volume_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))

      rt(app, req)
    }
  }

  "Getting enter-goods/tobacco/*/tell-us" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/invalid/path/tell-us")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when given a valid path for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
      ).get
      status(result) shouldBe OK
    }

    "return a 200 when given a valid path for rolling tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = None

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
        )
      ).get
      status(result) shouldBe OK
    }

    "return a 200 when given a valid path for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
      ).get
      status(result) shouldBe OK
    }

    "return a 200 when given a valid path for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
        )
      ).get
      status(result) shouldBe OK
    }

    "display default country and currency if set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

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
              productPath,
              "iid0",
              weightOrVolume,
              noOfSticks,
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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[no_of_sticks_weight_or_volume_input], times(1))(
        formCaptor.capture(),
        any(),
        any(),
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

    "not display default country and currency if not set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

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
              productPath,
              "iid0",
              weightOrVolume,
              noOfSticks,
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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[no_of_sticks_weight_or_volume_input], times(1))(
        formCaptor.capture(),
        any(),
        any(),
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

      override def productPath: ProductPath = ProductPath("tobacco/cigars")

      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))

      override def noOfSticks: Option[Int] = Some(150)

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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }

    "redirect to previous-declaration page when amendState = pending-payment set in JourneyData for displayNoOfSticksAddForm" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")

      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))

      override def noOfSticks: Option[Int] = Some(150)

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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }

    "redirect to previous-declaration page when amendState = pending-payment set in JourneyData for displayWeightAddForm" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")

      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))

      override def noOfSticks: Option[Int] = Some(150)

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
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
        )
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

  "Posting /enter-goods/tobacco/*/tell-us" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/invalid/path/tell-us"
        )
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 400 when country not present for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "",
            "currency"   -> "EUR",
            "noOfSticks" -> "400",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "Not a real country",
            "currency"   -> "EUR",
            "noOfSticks" -> "400",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "",
            "noOfSticks" -> "400",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "Not a valid currency",
            "noOfSticks" -> "400",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "EUR",
            "noOfSticks" -> "400",
            "cost"       -> ""
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks not present for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "EUR",
            "noOfSticks" -> "",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfStick not valid for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "EUR",
            "noOfSticks" -> "invalid noOfSticks",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI to the JourneyData and redirect to next step for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "EUR",
            "noOfSticks" -> "400",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("tobacco/cigarettes")),
        any(),
        meq(Some(400)),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(List(BigDecimal(92.50))),
        any(),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "add a PPI to the JourneyData and redirect to UK VAT PAid question for GBNI Journey" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "EUR",
            "noOfSticks" -> "400",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/pid/gb-ni-vat-check"
      )

    }

    "add a PPI to the JourneyData and redirect to Eu Evidence page for EUGB Journey where producedIn is an EU country" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"       -> "FR",
            "originCountry" -> "FR",
            "currency"      -> "EUR",
            "noOfSticks"    -> "400",
            "cost"          -> "92.50"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/pid/eu-evidence-check"
      )

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a non-EU country" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"       -> "FR",
            "originCountry" -> "IN",
            "currency"      -> "EUR",
            "noOfSticks"    -> "400",
            "cost"          -> "92.50"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn has a null value" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "EUR",
            "noOfSticks" -> "400",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "return a 400 when country not present for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"    -> "",
        "currency"   -> "EUR",
        "noOfSticks" -> "400",
        "cost"       -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"    -> "Not a real country",
        "currency"   -> "EUR",
        "noOfSticks" -> "400",
        "cost"       -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"    -> "FR",
        "currency"   -> "",
        "noOfSticks" -> "400",
        "cost"       -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"    -> "FR",
        "currency"   -> "Not a valid currency",
        "noOfSticks" -> "400",
        "cost"       -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"    -> "FR",
        "currency"   -> "EUR",
        "noOfSticks" -> "400",
        "cost"       -> ""
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks not present for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"    -> "FR",
        "currency"   -> "EUR",
        "noOfSticks" -> "",
        "cost"       -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfStick not valid for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"    -> "FR",
        "currency"   -> "EUR",
        "noOfSticks" -> "invalid noOfSticks",
        "cost"       -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI to the JourneyData and redirect to next step for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"    -> "FR",
        "currency"   -> "EUR",
        "noOfSticks" -> "400",
        "cost"       -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("tobacco/heated-tobacco")),
        any(),
        meq(Some(400)),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(List(BigDecimal(92.50))),
        any(),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "return a 400 when country not present for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = None

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"        -> "",
        "currency"       -> "EUR",
        "weightOrVolume" -> "400.0",
        "cost"           -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = None

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"        -> "Not a real country",
        "currency"       -> "EUR",
        "weightOrVolume" -> "400.0",
        "cost"           -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = None

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"        -> "FR",
        "currency"       -> "",
        "weightOrVolume" -> "400.0",
        "cost"           -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = None

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"        -> "FR",
        "currency"       -> "Not a valid currency",
        "weightOrVolume" -> "400.0",
        "cost"           -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = None

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"        -> "FR",
        "currency"       -> "EUR",
        "weightOrVolume" -> "400.0",
        "cost"           -> ""
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not present for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = None

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"        -> "FR",
        "currency"       -> "EUR",
        "weightOrVolume" -> "",
        "cost"           -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not valid for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = None

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"        -> "FR",
        "currency"       -> "EUR",
        "weightOrVolume" -> "Invalid weightOrVolume",
        "cost"           -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI to the JourneyData and redirect to next step for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = None

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us"
        )
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "400.0",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("tobacco/rolling-tobacco")),
        meq(Some(BigDecimal(0.4))),
        any(),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(List(BigDecimal(92.50))),
        any(),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "redirect to limit exceed page for over allowance of cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-CIGRT" -> "1.1")

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(900)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "EUR",
            "noOfSticks" -> "900",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/cigarettes/upper-limits/units-of-product"
      )
    }

    "redirect to limit exceed page for over allowance of heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-HTB" -> "1.1")

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(801)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "noOfSticks" -> "801",
        "country"    -> "FR",
        "currency"   -> "EUR",
        "cost"       -> "50"
      )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/heated-tobacco/upper-limits/units-of-product"
      )
    }

    "redirect to warning page on more than allowance for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-CIGAR" -> "1.1")

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(201))
      override def noOfSticks: Option[Int]            = Some(201)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "noOfSticks"     -> "400",
            "weightOrVolume" -> "400.0",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/cigars/upper-limits/units-of-product"
      )
    }

    "redirect to limit exceed page for over allowance of chewing tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-LOOSE" -> "1.1")

      override def productPath: ProductPath           = ProductPath("tobacco/chewing-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(1001)
      override def noOfSticks: Option[Int]            = None

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/chewing-tobacco/tell-us"
      ).withFormUrlEncodedBody(
        "country"        -> "FR",
        "currency"       -> "EUR",
        "weightOrVolume" -> "1000.01",
        "cost"           -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/chewing-tobacco/upper-limits/weight"
      )
    }

    "return a 400 when country not present for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "",
            "currency"       -> "EUR",
            "noOfSticks"     -> "50",
            "weightOrVolume" -> "400.0",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "Invalid country",
            "currency"       -> "EUR",
            "noOfSticks"     -> "50",
            "weightOrVolume" -> "400.0",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "",
            "noOfSticks"     -> "50",
            "weightOrVolume" -> "400.0",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "Invalid currency",
            "noOfSticks"     -> "50",
            "weightOrVolume" -> "400.0",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "noOfSticks"     -> "50",
            "weightOrVolume" -> "400.0",
            "cost"           -> ""
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not present for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "noOfSticks"     -> "50",
            "weightOrVolume" -> "",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "noOfSticks"     -> "50",
            "weightOrVolume" -> "Invalid weightOrVolume",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks not present for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "noOfSticks"     -> "",
            "weightOrVolume" -> "400.0",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "noOfSticks"     -> "Invalid noOfSticks",
            "weightOrVolume" -> "400.0",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks and weightOrVolume not present for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "noOfSticks"     -> "",
            "weightOrVolume" -> "",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks and weightOrVolume not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "noOfSticks"     -> "Invalid noOfSticks",
            "weightOrVolume" -> "Invalid weightOrVolume",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI to the JourneyData and redirect to next step for cigars" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "noOfSticks"     -> "50",
            "weightOrVolume" -> "400.0",
            "cost"           -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("tobacco/cigars")),
        meq(Some(BigDecimal(0.4))),
        meq(Some(50)),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(List(BigDecimal(92.50))),
        any(),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }
  }

  "Getting displayNoOfSticksEditForm" should {

    "return a 404 when given an invalid iid" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/missing-iid/edit")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 500 when purchase is missing country" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

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
              ProductPath("tobacco/cigarettes"),
              "iid0",
              None,
              Some(400),
              None,
              None,
              Some("EUR"),
              Some(BigDecimal(92.50))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when missing currency" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

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
              ProductPath("tobacco/cigarettes"),
              "iid0",
              None,
              Some(400),
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              None,
              Some(BigDecimal(92.50))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 404 when purchase has invalid product path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

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
              ProductPath("tobacco/invalid/path"),
              "iid0",
              None,
              Some(400),
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              Some("EUR"),
              Some(BigDecimal(92.50))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when all is ok for cigarettes" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

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
              ProductPath("tobacco/cigarettes"),
              "iid0",
              None,
              Some(400),
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              Some("EUR"),
              Some(BigDecimal(92.50))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
      ).get
      status(result) shouldBe OK
    }

    "return a 200 when all is ok for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

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
              ProductPath("tobacco/heated-tobacco"),
              "iid0",
              None,
              Some(400),
              Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
              None,
              Some("EUR"),
              Some(BigDecimal(92.50))
            )
          )
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
      ).get
      status(result) shouldBe OK
    }
  }

  "Posting processEditForm" should {

    "return a 404 when iid is not found in journey data" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/missing-iid/edit")
          .withFormUrlEncodedBody(
            "country"    -> "FR",
            "currency"   -> "EUR",
            "noOfSticks" -> "400",
            "cost"       -> "92.50"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND

    }

    "modify the relevant PPI in the JourneyData and redirect to next step" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(20.0))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "400.0",
            "noOfSticks"     -> "50",
            "cost"           -> "98.00"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).updatePurchase(
        meq(ProductPath("tobacco/cigars")),
        meq("iid0"),
        meq(Some(BigDecimal(0.4))),
        meq(Some(50)),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(BigDecimal(98.00)),
        any()
      )(any())

      verify(injected[Cache], times(2)).store(any())(any())
    }

    "modify a PPI in the JourneyData and redirect to UKVatPaid page when GBNI journey" in new LocalSetup {
      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(20.0))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "400.0",
            "noOfSticks"     -> "50",
            "cost"           -> "98.00"
          )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/iid0/gb-ni-vat-check"
      )
    }

    "add a PPI to the JourneyData and redirect to Eu Evidence page for EUGB Journey where producedIn is an EU country" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(20.0))
      override def noOfSticks: Option[Int]            = Some(150)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "originCountry"  -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "400.0",
            "noOfSticks"     -> "50",
            "cost"           -> "98.00"
          )

      val result: Future[Result] = euGBRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/iid0/eu-evidence-check"
      )

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a non-EU country" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "originCountry"  -> "IN",
            "weightOrVolume" -> "400.0",
            "noOfSticks"     -> "50",
            "cost"           -> "98.00"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn has a null value" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(400)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "400.0",
            "noOfSticks"     -> "50",
            "cost"           -> "98.00"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "redirect to limit exceed warning page for cigarettes edit" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-CIGRT" -> "1.1")

      override def productPath: ProductPath           = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int]            = Some(801)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "50",
            "noOfSticks"     -> "801",
            "cost"           -> "98.00"
          )

      val result: Future[Result] = route(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/cigarettes/upper-limits/iid0/edit/units-of-product"
      )
    }

    "redirect to limit exceed warning page for cigars edit" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-CIGAR" -> "1.1")

      override def productPath: ProductPath           = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(201)
      override def noOfSticks: Option[Int]            = Some(201)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "999.5",
            "noOfSticks"     -> "201",
            "cost"           -> "98.00"
          )

      val result: Future[Result] = route(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/cigars/upper-limits/iid0/edit/units-of-product"
      )
    }

    "redirect to limit exceed warning page for loose tobacco edit" in {

      lazy val fakeLimits: Map[String, String] = Map("L-LOOSE" -> "1.1")

      val cacheDataWithWorkingInstance =
        JourneyData(
          prevDeclaration = Some(false),
          euCountryCheck = Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          isUKVatPaid = None,
          isUKVatExcisePaid = None,
          isUKResident = None,
          isUccRelief = None,
          isVatResClaimed = Some(true),
          isBringingDutyFree = None,
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          irishBorder = None,
          selectedAliases = List(),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              path = ProductPath("tobacco/chewing-tobacco"),
              iid = "iid0",
              weightOrVolume = Some(0.7),
              noOfSticks = None,
              country = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List())),
              originCountry = None,
              currency = Some("EUR"),
              cost = Some(12.99),
              searchTerm = None,
              isVatPaid = None,
              isCustomPaid = None,
              isExcisePaid = None,
              isUccRelief = None,
              hasEvidence = None,
              isEditable = Some(true)
            ),
            PurchasedProductInstance(
              path = ProductPath("tobacco/rolling-tobacco"),
              iid = "iid1",
              weightOrVolume = Some(0.1),
              noOfSticks = None,
              country = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List())),
              originCountry = None,
              currency = Some("EUR"),
              cost = Some(12.99),
              searchTerm = None,
              isVatPaid = None,
              isCustomPaid = None,
              isExcisePaid = None,
              isUccRelief = None,
              hasEvidence = None,
              isEditable = Some(true)
            ),
            PurchasedProductInstance(
              path = ProductPath("tobacco/chewing-tobacco"),
              iid = "iid2",
              weightOrVolume = Some(0.1),
              noOfSticks = None,
              country = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List())),
              originCountry = None,
              currency = Some("EUR"),
              cost = Some(12.99),
              searchTerm = None,
              isVatPaid = None,
              isCustomPaid = None,
              isExcisePaid = None,
              isUccRelief = None,
              hasEvidence = None,
              isEditable = Some(true)
            )
          ),
          workingInstance = Some(
            PurchasedProductInstance(
              path = ProductPath("tobacco/chewing-tobacco"),
              iid = "iid2",
              weightOrVolume = Some(0.1),
              noOfSticks = None,
              country = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List())),
              originCountry = None,
              currency = Some("EUR"),
              cost = Some(12.99),
              searchTerm = None,
              isVatPaid = None,
              isCustomPaid = None,
              isExcisePaid = None,
              isUccRelief = None,
              hasEvidence = None,
              isEditable = Some(true)
            )
          ),
          userInformation = None,
          calculatorResponse = None,
          chargeReference = None,
          defaultCountry = Some("FR"),
          defaultOriginCountry = Some("FR"),
          defaultCurrency = Some("EUR"),
          previousDeclarationRequest = None,
          declarationResponse = None,
          deltaCalculation = None,
          amendmentCount = None,
          pendingPayment = None,
          amendState = None
        )

      when(injected[CalculatorService].limitUsage(any())(any())).thenReturn(
        Future.successful(LimitUsageSuccessResponse(fakeLimits))
      )

      when(injected[Cache].fetch(any()))
        .thenReturn(Future.successful(Some(cacheDataWithWorkingInstance)))

      when(injected[Cache].store(any())(any()))
        .thenReturn(Future.successful(cacheDataWithWorkingInstance))

      when(
        injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ).thenReturn(cacheDataWithWorkingInstance)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        FakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid2/edit")
          .withSession(SessionKeys.sessionId -> "fakesessionid")
          .withFormUrlEncodedBody(
            "country"        -> "FR",
            "originCountry"  -> "FR",
            "currency"       -> "EUR",
            "weightOrVolume" -> "300.00",
            "cost"           -> "98.00"
          )

      val result: Future[Result] = rt(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/chewing-tobacco/upper-limits/iid2/edit/weight"
      )
    }
  }
}
