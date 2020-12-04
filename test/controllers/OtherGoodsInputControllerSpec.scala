/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import connectors.Cache
import models._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
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
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
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

  override def beforeEach: Unit = {
    reset(injected[Cache], injected[NewPurchaseService], injected[other_goods_input])
  }

  trait LocalSetup {

    lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
      prevDeclaration = Some(false),
      Some("nonEuOnly"),
      arrivingNICheck = Some(true),
      isVatResClaimed = None,
      isBringingDutyFree = None,
      bringingOverAllowance = Some(true),
      privateCraft = Some(false),
      ageOver17 = Some(true),
      purchasedProductInstances = List(PurchasedProductInstance(
        ProductPath("other-goods/books"),
        "iid0",
        None,
        None,
        Some(Country("FR", "title.france", "FR", true, Nil)),
        Some("EUR"),
        Some(BigDecimal(12.99))
      ))
    ))

    val formCaptor: ArgumentCaptor[Form[OtherGoodsDto]] = ArgumentCaptor.forClass(classOf[Form[OtherGoodsDto]])

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())

      when(injected[NewPurchaseService].insertPurchases(any(),any(),any(),any(),any(),any(),any())(any())) thenReturn cachedJourneyData.get
      when(injected[NewPurchaseService].updatePurchase(any(),any(),any(),any(),any(),any(),any())(any())) thenReturn cachedJourneyData.get

      when(injected[other_goods_input].apply(any(), any(), any(), any(), any(), any())(any(), any())) thenReturn Html("")

      rt(app, req)
    }
  }

  "Getting displayEditForm" should {

    "return a 404 when given an invalid iid" in new LocalSetup {

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/edit/missing-iid")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 500 when purchase is missing country" in new LocalSetup {

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
          ProductPath("other-goods/books"),
          "iid0",
          None,
          None,
          None,
          Some("EUR"),
          Some(BigDecimal(12.99))
        ))
      ))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when missing currency" in new LocalSetup {

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
          ProductPath("other-goods/books"),
          "iid0",
          None,
          None,
          Some(Country("FR", "title.france", "FR", true, Nil)),
          None,
          Some(BigDecimal(12.99))
        ))
      ))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 404 when purchase has invalid product path" in new LocalSetup {

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
          Some(Country("FR", "title.france", "FR", true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        ))
      ))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when all is ok" in new LocalSetup {


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit")).get
      status(result) shouldBe OK
    }
  }

  "Getting displayAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/invalid/path/tell-us")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when given a valid path" in new LocalSetup {

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us")).get
      status(result) shouldBe OK
    }

    "display default country and currency if set in JourneyData" in new LocalSetup {

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
          ProductPath("other-goods/books"),
          "iid0",
          None,
          None,
          Some(Country("FR", "title.france", "FR", true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        )),
        defaultCountry = Some("FR"),
        defaultCurrency = Some("EUR")
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/books/tell-us")).get

      status(result) shouldBe OK

      verify(injected[other_goods_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe "FR"
      formCaptor.getValue.data("currency") shouldBe "EUR"
    }

    "not display default country and currency if not set in JourneyData" in new LocalSetup {

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
          ProductPath("other-goods/books"),
          "iid0",
          None,
          None,
          Some(Country("FR", "title.france", "FR", true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/books/tell-us")).get

      status(result) shouldBe OK

      verify(injected[other_goods_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe ""
      formCaptor.getValue.data("currency") shouldBe ""
    }
  }

  "Posting processAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/invalid/path/tell-us")).get
      status(result) shouldBe NOT_FOUND
    }

    "render the form with an extra cost input when action == add-cost" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "add-cost",
        "country" -> "FR",
        "currency" -> "EUR",
        "costs[0]" -> "12.12"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe OK

      verify(injected[views.html.other_goods.other_goods_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe "FR"
      formCaptor.getValue.data("currency") shouldBe "EUR"
      formCaptor.getValue.data("costs[0]") shouldBe "12.12"
      formCaptor.getValue.data("costs[1]") shouldBe ""
    }

    "only render the form with up to 50 inputs when action == add-cost" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "add-cost",
        "country" -> "FR",
        "currency" -> "EUR",
        "costs[0]" -> "12.10",
        "costs[1]" -> "12.11",
        "costs[2]" -> "12.12",
        "costs[3]" -> "12.13",
        "costs[4]" -> "12.14",
        "costs[5]" -> "12.15",
        "costs[6]" -> "12.16",
        "costs[7]" -> "12.17",
        "costs[8]" -> "12.18",
        "costs[9]" -> "12.17",
        "costs[10]" -> "12.18",
        "costs[11]" -> "12.19",
        "costs[12]" -> "12.20",
        "costs[13]" -> "12.21",
        "costs[14]" -> "12.22",
        "costs[15]" -> "12.23",
        "costs[16]" -> "12.24",
        "costs[17]" -> "12.25",
        "costs[18]" -> "12.26",
        "costs[19]" -> "12.27",
        "costs[20]" -> "12.28",
        "costs[21]" -> "12.29",
        "costs[22]" -> "12.30",
        "costs[23]" -> "12.31",
        "costs[24]" -> "12.32",
        "costs[25]" -> "12.33",
        "costs[26]" -> "12.34",
        "costs[27]" -> "12.35",
        "costs[28]" -> "12.36",
        "costs[29]" -> "12.37",
        "costs[30]" -> "12.38",
        "costs[31]" -> "12.39",
        "costs[32]" -> "12.40",
        "costs[33]" -> "12.41",
        "costs[34]" -> "12.42",
        "costs[35]" -> "12.43",
        "costs[36]" -> "12.44",
        "costs[37]" -> "12.45",
        "costs[38]" -> "12.46",
        "costs[39]" -> "12.47",
        "costs[40]" -> "12.48",
        "costs[41]" -> "12.49",
        "costs[42]" -> "12.50",
        "costs[43]" -> "12.51",
        "costs[44]" -> "12.52",
        "costs[45]" -> "12.53",
        "costs[46]" -> "12.54",
        "costs[47]" -> "12.55",
        "costs[48]" -> "12.56",
        "costs[49]" -> "12.57"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe OK

      verify(injected[views.html.other_goods.other_goods_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe "FR"
      formCaptor.getValue.data("currency") shouldBe "EUR"
      formCaptor.getValue.data("costs[0]") shouldBe "12.10"
      formCaptor.getValue.data("costs[49]") shouldBe "12.57"
      formCaptor.getValue.data.get("costs[50]") shouldBe None

    }

    "return a 400 if no action is supplied" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "country" -> "",
        "currency" -> ""
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and country not present" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "",
        "currency" -> "EUR",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and country not valid" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "Not a real country",
        "currency" -> "EUR",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and currency not present" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "FR",
        "currency" -> "",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and currency not valid" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "FR",
        "currency" -> "XXX",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and cost not present" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "FR",
        "currency" -> "EUR"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and cost contains ',' only" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "FR",
        "currency" -> "EUR",
        "costs[0]" -> ","
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a number of PPIs to the JourneyData and redirect to next step when action == continue and iid is not present" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "FR",
        "currency" -> "EUR",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("other-goods/adult/adult-clothing")),
        any(),
        any(),
        meq("FR"),
        meq("EUR"),
        meq(List(BigDecimal(12.12), BigDecimal(13.13))),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }
  }

  "Posting processEditForm" should {

    "return a 404 when action == continue and iid is not found in journey data" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/missing-iid/edit").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "FR",
        "currency" -> "EUR",
        "costs[0]" -> "12.12"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND

    }

    "modify the relevant PPI in the JourneyData and redirect to next step when action == continue and iid is present" in new LocalSetup {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/iid0/edit").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "FR",
        "currency" -> "EUR",
        "costs[0]" -> "12.12"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).updatePurchase(
        meq(ProductPath("other-goods/books")),
        meq("iid0"),
        any(),
        any(),
        meq("FR"),
        meq("EUR"),
        meq(BigDecimal(12.12))
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

  }

}
