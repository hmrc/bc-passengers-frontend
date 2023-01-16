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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.MockitoSugar
import play.api.Application
import play.api.data.Form
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{route => rt, _}
import play.twirl.api.Html
import repositories.BCPassengersSessionRepository
import services.NewPurchaseService
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import views.html.other_goods.other_goods_input

import scala.concurrent.Future

class OtherGoodsInputControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[NewPurchaseService].toInstance(MockitoSugar.mock[NewPurchaseService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[other_goods_input].toInstance(MockitoSugar.mock[other_goods_input]))
    .build()

  override def beforeEach(): Unit = {
    reset(injected[Cache])
    reset(injected[NewPurchaseService])
    reset(injected[other_goods_input])
  }

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
            ProductPath("other-goods/books"),
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
            ProductPath("other-goods/books"),
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
            ProductPath("other-goods/books"),
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

    val formCaptor: ArgumentCaptor[Form[OtherGoodsDto]] = ArgumentCaptor.forClass(classOf[Form[OtherGoodsDto]])

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())
      when(injected[Cache].storeJourneyData(any())(any())) thenReturn Future.successful(cachedJourneyData)
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
        injected[other_goods_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ) thenReturn Html("")

      rt(app, req)
    }

    def gbNIRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedGBNIJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())
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
        injected[other_goods_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ) thenReturn Html("")

      rt(app, req)
    }

    def euGBRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedEUGBJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())
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
        injected[other_goods_input]
          .apply(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ) thenReturn Html("")

      rt(app, req)
    }
  }

  "Getting displayEditForm" should {

    "return a 404 when given missing iid" in new LocalSetup {

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/edit")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 500 when purchase is missing country" in new LocalSetup {

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
              ProductPath("other-goods/books"),
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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when missing currency" in new LocalSetup {

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
              ProductPath("other-goods/books"),
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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
      ).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 404 when purchase has invalid iid" in new LocalSetup {

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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when all is ok" in new LocalSetup {

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
      ).get
      status(result) shouldBe OK
    }
  }

  "Getting displayAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/invalid/path/tell-us"
        )
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when given a valid path" in new LocalSetup {

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
      ).get
      status(result) shouldBe OK
    }

    "display default searchTerm, country and currency if set in JourneyData" in new LocalSetup {

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
          selectedAliases = List(ProductAlias("Book", ProductPath("other-goods/books"))),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              ProductPath("other-goods/books"),
              "iid0",
              None,
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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[other_goods_input], times(1))(
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

      formCaptor.getValue.data("country")    shouldBe "FR"
      formCaptor.getValue.data("currency")   shouldBe "EUR"
      formCaptor.getValue.data("searchTerm") shouldBe "Book"
    }

    "not display default searchTerm, country and currency if not set in JourneyData" in new LocalSetup {

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
              ProductPath("other-goods/books"),
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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
      ).get

      status(result) shouldBe OK

      verify(injected[other_goods_input], times(1))(
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

      formCaptor.getValue.data("country")    shouldBe ""
      formCaptor.getValue.data("currency")   shouldBe ""
      formCaptor.getValue.data("searchTerm") shouldBe ""
    }

    "redirect to previous-declaration page when amendState = pending-payment set in JourneyData" in new LocalSetup {

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
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

  "Posting processAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/invalid/path/tell-us"
        )
      ).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 400 if no action is supplied" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
          .withFormUrlEncodedBody(
            "country"  -> "",
            "currency" -> ""
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and country not present" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
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

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
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

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
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

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
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

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
          .withFormUrlEncodedBody(
            "action"   -> "continue",
            "country"  -> "FR",
            "currency" -> "EUR"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and cost contains ',' only" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
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

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
          .withFormUrlEncodedBody(
            "action"     -> "continue",
            "searchTerm" -> "label.other-goods.womens_clothes",
            "country"    -> "FR",
            "currency"   -> "EUR",
            "cost"       -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("other-goods/adult/adult-clothing")),
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

    "add a PPI to the JourneyData and redirect to UKVatPaid page when GBNI journey" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
          .withFormUrlEncodedBody(
            "action"     -> "continue",
            "searchTerm" -> "label.other-goods.adult.adult-clothing",
            "country"    -> "FR",
            "currency"   -> "EUR",
            "cost"       -> "12.12"
          )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/pid/gb-ni-vat-check"
      )
    }

    "add a PPI to the JourneyData and redirect to Eu Evidence page for EUGB Journey where producedIn is an EU country" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
          .withFormUrlEncodedBody(
            "action"        -> "continue",
            "searchTerm"    -> "label.other-goods.adult.adult-clothing",
            "country"       -> "FR",
            "originCountry" -> "FR",
            "currency"      -> "EUR",
            "cost"          -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/pid/eu-evidence-check"
      )

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a non-EU country" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
          .withFormUrlEncodedBody(
            "action"        -> "continue",
            "searchTerm"    -> "label.other-goods.adult.adult-clothing",
            "country"       -> "FR",
            "originCountry" -> "IN",
            "currency"      -> "EUR",
            "cost"          -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a null value" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us")
          .withFormUrlEncodedBody(
            "action"     -> "continue",
            "searchTerm" -> "label.other-goods.adult.adult-clothing",
            "country"    -> "FR",
            "currency"   -> "EUR",
            "cost"       -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }
  }

  "Posting processEditForm" should {

    "return a 404 when action == continue and iid is not found in journey data" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = enhancedFakeRequest(
        "POST",
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/missing-iid/edit"
      ).withFormUrlEncodedBody(
        "action"   -> "continue",
        "country"  -> "FR",
        "currency" -> "EUR",
        "cost"     -> "12.12"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND

    }

    "return a 400 when action == continue and currency not present" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "action"     -> "continue",
            "searchTerm" -> "label.other-goods.book",
            "country"    -> "FR",
            "currency"   -> "",
            "cost"       -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "modify the relevant PPI in the JourneyData and redirect to next step when action == continue and iid is present" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "action"     -> "continue",
            "searchTerm" -> "label.other-goods.book",
            "country"    -> "FR",
            "currency"   -> "EUR",
            "cost"       -> "12.12"
          )

      val result: Future[Result] = route(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).updatePurchase(
        meq(ProductPath("other-goods/books")),
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

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "action"     -> "continue",
            "searchTerm" -> "label.other-goods.book",
            "country"    -> "FR",
            "currency"   -> "EUR",
            "cost"       -> "12.12"
          )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/books/iid0/gb-ni-vat-check"
      )
    }

    "add a PPI to the JourneyData and redirect to Eu Evidence page for EUGB Journey where producedIn is an EU country" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "action"        -> "continue",
            "searchTerm"    -> "label.other-goods.book",
            "country"       -> "FR",
            "originCountry" -> "FR",
            "currency"      -> "EUR",
            "cost"          -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/books/iid0/eu-evidence-check"
      )

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn is a non-EU country" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "action"        -> "continue",
            "searchTerm"    -> "label.other-goods.adult.adult-clothing",
            "country"       -> "FR",
            "originCountry" -> "IN",
            "currency"      -> "EUR",
            "cost"          -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "add a PPI to the JourneyData and redirect to next-step for EUGB Journey where producedIn has a null value" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")
          .withFormUrlEncodedBody(
            "action"     -> "continue",
            "searchTerm" -> "label.other-goods.adult.adult-clothing",
            "country"    -> "FR",
            "currency"   -> "EUR",
            "cost"       -> "12.12"
          )

      val result: Future[Result] = euGBRoute(app, req).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }
  }
}
