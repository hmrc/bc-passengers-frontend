/*
 * Copyright 2026 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{eq as meq, *}
import org.mockito.Mockito.*
import play.api.Application
import play.api.data.Form
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers.{route as rt, *}
import play.twirl.api.Html
import repositories.BCPassengersSessionRepository
import services.{CalculatorService, LimitUsageSuccessResponse, NewPurchaseService}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import views.html.vaping_products.vaping_products_input

import scala.concurrent.Future

class VapingProductsInputControllerSpec extends BaseSpec with Injecting{

  val injectedCache: Cache = inject[Cache]
  val injectedNewPurchaseService: NewPurchaseService = inject[NewPurchaseService]
  val injectedVapingInput: vaping_products_input = inject[vaping_products_input]

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[NewPurchaseService].toInstance(mock(classOf[NewPurchaseService])))
    .overrides(bind[CalculatorService].toInstance(mock(classOf[CalculatorService])))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[vaping_products_input].toInstance(mock(classOf[vaping_products_input])))
    .build()

  override def beforeEach(): Unit = {
    reset(injectedCache)
    reset(injectedNewPurchaseService)
    reset(injectedVapingInput)
  }

  trait LocalSetup {

    def fakeLimits: Map[String, String]

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
            ProductPath("vaping-products/vape"),
            "iid0",
            Some(BigDecimal(12.99)),
            None,
            Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
            None,
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
            ProductPath("vaping-products/vape"),
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
            ProductPath("vaping-products/vape"),
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

    val formCaptor: ArgumentCaptor[Form[VapeDto]] = ArgumentCaptor.forClass(classOf[Form[VapeDto]])

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
        injected[NewPurchaseService]
          .insertPurchasesWithIid(any(), any(), any(), any(), any(), any(), any(), any(), any())(any())
      ).thenReturn(insertedPurchase)
      when(
        injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any()
        )
      ).thenReturn(cachedJourneyData.get)

      when(
        injected[vaping_products_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
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
        injected[vaping_products_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
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
        injected[vaping_products_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))

      rt(app, req)
    }
  }

  "Getting displayEditForm" should {

    "return a 404 when given missing iid" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/edit")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 500 when purchase is missing country" in new LocalSetup {

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
              ProductPath("vaping-products/vape"),
              "iid0",
              None,
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
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit"
        )
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR

    }

    "return a 500 when missing currency" in new LocalSetup {

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
              ProductPath("vaping-products/vape"),
              "iid0",
              None,
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
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit"
        )
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 404 when purchase has invalid iid" in new LocalSetup {

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
              ProductPath("invalid/product/path"),
              "iid1",
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
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit"
        )
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when all is ok" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-VPRODUCTS" -> "1.0")

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit")
      ).get
      status(result) shouldBe OK
    }
    "pre-populate country and currency when editing an existing item" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-VPRODUCTS" -> "1.0")

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit")
      ).get

      status(result) shouldBe OK

      verify(injected[vaping_products_input], times(1))(
        formCaptor.capture(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )(any(), any(), any())

      val capturedForm: Form[VapeDto] = formCaptor.getValue
      capturedForm("country").value  shouldBe Some("FR")
      capturedForm("currency").value shouldBe Some("EUR")
    }
  }

  "Getting displayAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/invalid/path/tell-us"
        )
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when given a valid path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us")
      ).get
      status(result) shouldBe OK
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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
    "prefill originCountry if defaultOriginCountry is set and non-empty" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          selectedAliases = List(ProductAlias("Book", ProductPath("vaping-products/vape"))),
          defaultOriginCountry = Some("FR")
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[vaping_products_input], times(1))(
        formCaptor.capture(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )(any(), any(), any())

      val data = formCaptor.getValue.data
      data.get("originCountry") shouldBe Some("FR")
      data                        should not contain key("country")
      data                        should not contain key("currency")
    }
  }

  "Posting processAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/invalid/path/tell-us"
        )
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 400 if no action is supplied" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "country"  -> "",
            "currency" -> ""
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and country not present" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"   -> "continue",
            "country"  -> "",
            "currency" -> "EUR",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and country not valid" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"   -> "continue",
            "country"  -> "Not a real country",
            "currency" -> "EUR",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and currency not present" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"   -> "continue",
            "country"  -> "FR",
            "currency" -> "",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and currency not valid" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"   -> "continue",
            "country"  -> "FR",
            "currency" -> "XXX",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and cost not present" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"   -> "continue",
            "country"  -> "FR",
            "currency" -> "EUR"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and cost contains ',' only" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"   -> "continue",
            "country"  -> "FR",
            "currency" -> "EUR",
            "cost"     -> ","
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a number of PPIs to the JourneyData and redirect to next step when action == continue and iid is not present" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("vaping-products/vape")),
        any(),
        any(),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(List(BigDecimal(12.12))),
        any(),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "add a PPI using submitted iid and redirect to next step when action == continue" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "iid"            -> "ABCdef",
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")

      verify(injected[NewPurchaseService], times(1)).insertPurchasesWithIid(
        meq(ProductPath("vaping-products/vape")),
        any(),
        any(),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(List(BigDecimal(12.12))),
        meq("ABCdef"),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    Seq("vaping-products/vape").foreach { path =>
      s"add PPI to the JourneyData with path $path and redirect to UKVatPaid page when GBNI journey" in new LocalSetup {

        override lazy val fakeLimits: Map[String, String] = Map[String, String]()

        val req: FakeRequest[AnyContentAsFormUrlEncoded] =
          enhancedFakeRequest(
            "POST",
            "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
          )
            .withFormUrlEncodedBody(
              "weightOrVolume" -> "60.0",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> "12.12"
            )

        val result: Future[Result] = gbNIRoute(app, req).get
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$path/pid/gb-ni-vat-check"
        )
      }
    }

    Seq("vaping-products/vape").foreach { path =>
      s"add PPI to the JourneyData with path $path and redirect to the item CYA page" when {
        "Non EU journey" in new LocalSetup {

          override lazy val fakeLimits: Map[String, String] = Map[String, String]()

          val req: FakeRequest[AnyContentAsFormUrlEncoded] =
            enhancedFakeRequest(
              "POST",
              "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
            )
              .withFormUrlEncodedBody(
                "weightOrVolume" -> "60.0",
                "country"        -> "FR",
                "currency"       -> "EUR",
                "cost"           -> "12.12"
              )

          val result: Future[Result] = route(app, req).get
          status(result)             shouldBe SEE_OTHER
          redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")
        }
      }
    }

    "add a PPI to the JourneyData and redirect to Eu Evidence page for EUGB Journey where producedIn is an EU country" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "originCountry"  -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/pid/eu-evidence-check"
      )

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a non-EU country" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "originCountry"  -> "IN",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")
    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a null value" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")

    }
  }

  "Posting processEditForm" should {

    "return a 404 when action == continue and iid is not found in journey data" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/missing-iid/edit"
      ).withFormUrlEncodedBody(
        "action"         -> "continue",
        "weightOrVolume" -> "60.0",
        "country"        -> "FR",
        "currency"       -> "EUR",
        "cost"           -> "12.12"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND

    }

    "return a 400 when action == continue and currency not present" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "currency"       -> "",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "modify the relevant PPI in the JourneyData and redirect to next step when action == continue and iid is present" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")

      verify(injected[NewPurchaseService], times(1)).updatePurchase(
        meq(ProductPath("vaping-products/vape")),
        meq("iid0"),
        any(),
        any(),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(BigDecimal(12.12)),
        any()
      )(any())

      verify(injected[Cache], times(2)).store(any())(any())
    }

    "modify a PPI in the JourneyData and redirect to UKVatPaid page when GBNI journey" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/iid0/gb-ni-vat-check"
      )
    }

    "add a PPI to the JourneyData and redirect to Eu Evidence page for EUGB Journey where producedIn is an EU country" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "originCountry"  -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/iid0/eu-evidence-check"
      )

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a non-EU country" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "originCountry"  -> "IN",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn has a null value" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/iid0/edit"
        )
          .withFormUrlEncodedBody(
            "action"         -> "continue",
            "weightOrVolume" -> "60.0",
            "country"        -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")

    }
  }
}
