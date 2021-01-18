/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import connectors.Cache
import models._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
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
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
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

  override def beforeEach: Unit = {
    reset(injected[Cache], injected[NewPurchaseService], injected[alcohol_input])
  }

  trait LocalSetup {

    lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
      prevDeclaration = Some(false),
      Some("nonEuOnly"),
      arrivingNICheck= Some(true),
      isVatResClaimed = None,
      isBringingDutyFree = None,
      bringingOverAllowance = Some(true),
      privateCraft = Some(false),
      ageOver17 = Some(true),
      purchasedProductInstances = List(PurchasedProductInstance(
        ProductPath("alcohol/beer"),
        "iid0",
        Some(20.0),
        None,
        Some(Country("FR", "title.france", "FR", isEu = true, Nil)),
        Some("EUR"),
        Some(BigDecimal(12.99))
      ))
    ))

    lazy val cachedGBNIJourneyData: Option[JourneyData] = Some(JourneyData(
      prevDeclaration = Some(false),
      Some("greatBritain"),
      arrivingNICheck= Some(true),
      isVatResClaimed = None,
      isBringingDutyFree = None,
      bringingOverAllowance = Some(true),
      privateCraft = Some(false),
      ageOver17 = Some(true),
      purchasedProductInstances = List(PurchasedProductInstance(
        ProductPath("alcohol/beer"),
        "iid0",
        Some(20.0),
        None,
        Some(Country("FR", "title.france", "FR", isEu = true, Nil)),
        Some("EUR"),
        Some(BigDecimal(12.99))
      ))
    ))

    val formCaptor: ArgumentCaptor[Form[AlcoholDto]] = ArgumentCaptor.forClass(classOf[Form[AlcoholDto]])

    def fakeLimits: Map[String, String]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())

      when(injected[CalculatorService].limitUsage(any())(any())) thenReturn Future.successful(LimitUsageSuccessResponse(fakeLimits))
      val insertedPurchase = (cachedJourneyData.get,"pid")
      when(injected[NewPurchaseService].insertPurchases(any(), any(), any(), any(), any(), any(), any())(any())) thenReturn insertedPurchase
      when(injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any())(any())) thenReturn cachedJourneyData.get

      when(injected[alcohol_input].apply(any(), any(), any(), any(), any(), any())(any(), any())) thenReturn Html("")

      rt(app, req)
    }

    def gbNIRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedGBNIJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())

      when(injected[CalculatorService].limitUsage(any())(any())) thenReturn Future.successful(LimitUsageSuccessResponse(fakeLimits))
      val insertedPurchase = (cachedGBNIJourneyData.get,"pid")
      when(injected[NewPurchaseService].insertPurchases(any(), any(), any(), any(), any(), any(), any())(any())) thenReturn insertedPurchase
      when(injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any())(any())) thenReturn cachedGBNIJourneyData.get

      when(injected[alcohol_input].apply(any(), any(), any(), any(), any(), any())(any(), any())) thenReturn Html("")

      rt(app, req)
    }
  }

  "Getting displayEditForm" should {

    "return a 404 when given an invalid iid" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/missing-iid/edit")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 500 when purchase is missing country" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(20.0),
          None,
          None,
          Some("EUR"),
          Some(BigDecimal(12.99))
        ))
      ))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when missing currency" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(20.0),
          None,
          Some(Country("FR", "title.france", "FR", isEu = true, Nil)),
          None,
          Some(BigDecimal(12.99))
        ))
      ))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when missing weightOrVolume" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          None,
          None,
          Some(Country("FR", "title.france", "FR", isEu = true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        ))
      ))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 404 when purchase has invalid product path" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("invalid/product/path"),
          "iid0",
          None,
          None,
          Some(Country("FR", "title.france", "FR", isEu = true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        ))
      ))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when all is ok" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit")).get
      status(result) shouldBe OK
    }
  }

  "Getting displayAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/invalid/path/tell-us")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when given a valid path" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")).get
      status(result) shouldBe OK
    }

    "display default country and currency if set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(20.0),
          None,
          Some(Country("FR", "title.france", "FR", isEu = true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        )),
        defaultCountry = Some("FR"),
        defaultCurrency = Some("EUR")
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")).get

      status(result) shouldBe OK

      verify(injected[views.html.alcohol.alcohol_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe "FR"
      formCaptor.getValue.data("currency") shouldBe "EUR"
    }

    "not display default country and currency if not set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          Some(20.0),
          None,
          Some(Country("FR", "title.france", "FR", isEu = true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us")).get

      status(result) shouldBe OK

      verify(injected[views.html.alcohol.alcohol_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe ""
      formCaptor.getValue.data("currency") shouldBe ""
    }
  }

  "Posting processAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map[String, String]()

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/invalid/path/tell-us")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 400 when country not present" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "",
        "currency" -> "EUR",
        "weightOrVolume" -> "20.0",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "Not a real country",
        "currency" -> "EUR",
        "weightOrVolume" -> "20.0",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "",
        "weightOrVolume" -> "20.0",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid" in new LocalSetup {

      override lazy val fakeLimits: Map[String, String] = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "XXX",
        "weightOrVolume" -> "20.0",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "20.0"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not valid" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "20.0",
        "cost" -> "invalid-cost"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not present" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not valid" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "invalid-volume",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when beer is over the calculator limit" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "111",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI to the JourneyData and redirect to next step" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "20.0",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("alcohol/beer")),
        meq(Some(BigDecimal(20.0))),
        any(),
        meq("FR"),
        meq("EUR"),
        meq(List(BigDecimal(12.50))),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "add a PPI to the JourneyData and redirect to UK VAT Paid page for GBNI Journey" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "20.0",
        "cost" -> "12.50"
      )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/pid/gb-ni-vat-check")

    }
  }

  "Posting processEditForm" should {

    "return a 404 when iid is not found in journey data" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/missing-iid/edit").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "20.0",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND

    }

    "return a 400 when beer is over the calculator limit" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "111",
        "cost" -> "12.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "modify the relevant PPI in the JourneyData and redirect to next step" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "13.0",
        "cost" -> "50.00"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).updatePurchase(
        meq(ProductPath("alcohol/beer")),
        meq("iid0"),
        meq(Some(BigDecimal(13.0))),
        any(),
        meq("FR"),
        meq("EUR"),
        meq(BigDecimal(50.00))
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "modify the relevant PPI in the JourneyData and redirect to UK VAT Paid page for GBNI Journey" in new LocalSetup {

      override lazy val fakeLimits = Map("L-BEER" -> "1.0", "L-WINE" -> "1.1")

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/iid0/edit").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "13.0",
        "cost" -> "50.00"
      )

      val result: Future[Result] = gbNIRoute(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/iid0/gb-ni-vat-check")

    }
  }
}
