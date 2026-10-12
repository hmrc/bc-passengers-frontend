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
import play.api.test.FakeRequest
import play.api.test.Helpers.{route as rt, *}
import play.twirl.api.Html
import repositories.BCPassengersSessionRepository
import services.{CalculatorService, LimitUsageSuccessResponse, NewPurchaseService}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import views.html.other_goods_ni.other_goods_input_ni

import scala.concurrent.Future

class OtherGoodsInputNIControllerSpec extends BaseSpec {

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .overrides(bind[NewPurchaseService].toInstance(mock(classOf[NewPurchaseService])))
    .overrides(bind[CalculatorService].toInstance(mock(classOf[CalculatorService])))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[other_goods_input_ni].toInstance(mock(classOf[other_goods_input_ni])))
    .build()

  override def beforeEach(): Unit = {
    reset(injected[Cache])
    reset(injected[NewPurchaseService])
    reset(injected[CalculatorService])
    reset(injected[other_goods_input_ni])
  }

  private val niPath: String = "other-ni-goods/books"

  private def ppi(
    weightOrVolume: Option[BigDecimal] = Some(BigDecimal(2)),
    country: Option[Country] = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
    currency: Option[String] = Some("EUR"),
    cost: Option[BigDecimal] = Some(BigDecimal(12.99)),
    path: ProductPath = ProductPath(niPath),
    iid: String = "iid0"
  ): PurchasedProductInstance =
    PurchasedProductInstance(
      path = path,
      iid = iid,
      weightOrVolume = weightOrVolume,
      country = country,
      currency = currency,
      cost = cost
    )

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
        purchasedProductInstances = List(ppi())
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
        purchasedProductInstances = List(ppi())
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
        purchasedProductInstances = List(ppi())
      )
    )

    val formCaptor: ArgumentCaptor[Form[OtherGoodsNIDto]] = ArgumentCaptor.forClass(classOf[Form[OtherGoodsNIDto]])

    private def stubCommon(journeyData: Option[JourneyData]): Unit = {
      when(injected[Cache].fetch(any())).thenReturn(Future.successful(journeyData))
      when(injected[Cache].store(any())(any())).thenReturn(Future.successful(JourneyData()))
      when(injected[Cache].storeJourneyData(any())(any())).thenReturn(Future.successful(journeyData))
      val insertedPurchase = (journeyData.get, "pid")
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
      ).thenReturn(journeyData.get)
      when(injected[CalculatorService].limitUsage(any())(any()))
        .thenReturn(Future.successful(LimitUsageSuccessResponse(Map.empty)))
      when(
        injected[other_goods_input_ni]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Html(""))
    }

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      stubCommon(cachedJourneyData)
      rt(app, req)
    }

    def gbNIRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      stubCommon(cachedGBNIJourneyData)
      rt(app, req)
    }

    def euGBRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      stubCommon(cachedEUGBJourneyData)
      rt(app, req)
    }
  }

  "Getting displayEditForm" should {

    "return a 404 when the iid is not present in the journey data" in new LocalSetup {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/missing/edit")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 500 when the purchase is missing its country" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(ppi(country = None))
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when the purchase is missing its currency" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(ppi(currency = None))
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when the purchase is missing its weightOrVolume" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(ppi(weightOrVolume = None))
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 404 when the purchase has an invalid product path" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          purchasedProductInstances = List(ppi(path = ProductPath("invalid/product/path")))
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when all is ok" in new LocalSetup {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
      ).get
      status(result) shouldBe OK
    }

    "pre-populate country and currency when editing an existing item" in new LocalSetup {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
      ).get

      status(result) shouldBe OK

      verify(injected[other_goods_input_ni], times(1))(
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

      val capturedForm = formCaptor.getValue
      capturedForm("country").value  shouldBe Some("FR")
      capturedForm("currency").value shouldBe Some("EUR")
    }

    "redirect to previous-declaration page when amendState = pending-payment" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          amendState = Some("pending-payment"),
          purchasedProductInstances = List(ppi())
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

  "Getting displayAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/invalid/path/tell-us"
        )
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when given a valid path" in new LocalSetup {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
      ).get
      status(result) shouldBe OK
    }

    "prefill originCountry if defaultOriginCountry is set and non-empty" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          Some("nonEuOnly"),
          arrivingNICheck = Some(true),
          bringingOverAllowance = Some(true),
          privateCraft = Some(false),
          ageOver17 = Some(true),
          defaultOriginCountry = Some("FR"),
          purchasedProductInstances = List(ppi())
        )
      )

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[other_goods_input_ni], times(1))(
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

      formCaptor.getValue.data.get("originCountry") shouldBe Some("FR")
    }

    "not prefill originCountry if defaultOriginCountry is not set" in new LocalSetup {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[other_goods_input_ni], times(1))(
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

      formCaptor.getValue.data should not contain key("originCountry")
    }

    "redirect to previous-declaration page when amendState = pending-payment" in new LocalSetup {
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
        enhancedFakeRequest("GET", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

  "Posting processAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/invalid/path/tell-us"
        ).withFormUrlEncodedBody(
          "country"  -> "FR",
          "currency" -> "EUR",
          "cost"     -> "12.12"
        )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 400 when country is not present" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
          .withFormUrlEncodedBody(
            "country"  -> "",
            "currency" -> "EUR",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country is not valid" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
          .withFormUrlEncodedBody(
            "country"  -> "Not a real country",
            "currency" -> "EUR",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency is not present" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
          .withFormUrlEncodedBody(
            "country"  -> "FR",
            "currency" -> "",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost is not present" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
          .withFormUrlEncodedBody(
            "country"  -> "FR",
            "currency" -> "EUR"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI and redirect to the item CYA page for a non-EU journey" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
          .withFormUrlEncodedBody(
            "weightOrVolume" -> "1",
            "country"        -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")

      verify(injected[NewPurchaseService], atLeastOnce()).insertPurchases(
        meq(ProductPath(niPath)),
        any(),
        any(),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(List(BigDecimal(12.12))),
        any(),
        any()
      )(any())

      verify(injected[Cache], atLeastOnce()).store(any())(any())
    }

    "add a PPI using a submitted iid and redirect to the item CYA page" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
          .withFormUrlEncodedBody(
            "iid"      -> "ABCdef",
            "country"  -> "FR",
            "currency" -> "EUR",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")

      verify(injected[NewPurchaseService], atLeastOnce()).insertPurchasesWithIid(
        meq(ProductPath(niPath)),
        any(),
        any(),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(List(BigDecimal(12.12))),
        meq("ABCdef"),
        any()
      )(any())
    }

    "add a PPI and redirect to the UKVatPaid page for a GBNI journey" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
          .withFormUrlEncodedBody(
            "country"  -> "FR",
            "currency" -> "EUR",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/pid/gb-ni-vat-check"
      )
    }

    "add a PPI and redirect to the EU Evidence page for an EUGB journey where originCountry is an EU country" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
          .withFormUrlEncodedBody(
            "country"       -> "FR",
            "originCountry" -> "FR",
            "currency"      -> "EUR",
            "cost"          -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/pid/eu-evidence-check"
      )
    }

    "add a PPI and redirect to the item CYA page for an EUGB journey where originCountry is a non-EU country" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/tell-us")
          .withFormUrlEncodedBody(
            "country"       -> "FR",
            "originCountry" -> "IN",
            "currency"      -> "EUR",
            "cost"          -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get
      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")
    }
  }

  "Posting processEditForm" should {

    "return a 404 when the iid is not found in the journey data" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/missing/edit"
      ).withFormUrlEncodedBody(
        "country"  -> "FR",
        "currency" -> "EUR",
        "cost"     -> "12.12"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 400 when currency is not present" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "country"  -> "FR",
            "currency" -> "",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "modify the relevant PPI and redirect to the item CYA page for a non-EU journey" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "weightOrVolume" -> "1",
            "country"        -> "FR",
            "currency"       -> "EUR",
            "cost"           -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")

      verify(injected[NewPurchaseService], atLeastOnce()).updatePurchase(
        meq(ProductPath(niPath)),
        meq("iid0"),
        any(),
        any(),
        meq("FR"),
        any(),
        meq("EUR"),
        meq(BigDecimal(12.12)),
        any()
      )(any())

      verify(injected[Cache], atLeastOnce()).store(any())(any())
      session(result).get(ControllerHelpers.checkYourItemEditModeSessionKey) shouldBe Some("iid0")
    }

    "modify a PPI and redirect to the UKVatPaid page for a GBNI journey" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "country"  -> "FR",
            "currency" -> "EUR",
            "cost"     -> "12.12"
          )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/iid0/gb-ni-vat-check"
      )
    }

    "modify a PPI and redirect to the EU Evidence page for an EUGB journey where originCountry is an EU country" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "country"       -> "FR",
            "originCountry" -> "FR",
            "currency"      -> "EUR",
            "cost"          -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        s"/check-tax-on-goods-you-bring-into-the-uk/enter-goods/$niPath/iid0/eu-evidence-check"
      )
    }

    "modify a PPI and redirect to the item CYA page for an EUGB journey where originCountry is a non-EU country" in new LocalSetup {
      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-ni-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "country"       -> "FR",
            "originCountry" -> "IN",
            "currency"      -> "EUR",
            "cost"          -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get
      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("/check-tax-on-goods-you-bring-into-the-uk/check-your-item/")
    }
  }
}
