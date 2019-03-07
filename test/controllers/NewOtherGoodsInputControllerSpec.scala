package controllers

import connectors.Cache
import models._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.data.Form
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import play.twirl.api.Html
import services.NewPurchaseService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class NewOtherGoodsInputControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[NewPurchaseService].toInstance(MockitoSugar.mock[NewPurchaseService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[views.html.new_other_goods.other_goods_input].toInstance(MockitoSugar.mock[views.html.new_other_goods.other_goods_input]))
    .build()

  override def beforeEach: Unit = {
    reset(injected[Cache], injected[NewPurchaseService], injected[views.html.new_other_goods.other_goods_input])
  }

  trait LocalSetup {

    lazy val cachedJourneyData = Some(JourneyData(
      Some("nonEuOnly"),
      isVatResClaimed = None,
      bringingDutyFree = None,
      privateCraft = Some(false),
      ageOver17 = Some(true),
      purchasedProductInstances = List(PurchasedProductInstance(
        ProductPath("other-goods/books"),
        "iid0",
        None,
        None,
        Some(Country("France", "FR", true, Nil)),
        Some("EUR"),
        Some(BigDecimal(12.99))
      ))
    ))

    val formCaptor = ArgumentCaptor.forClass(classOf[Form[WorkingPurchaseDataDto]])


    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(CacheMap("id", Map.empty))

      when(injected[NewPurchaseService].insertPurchases(any(),any(),any(),any(),any())(any())) thenReturn cachedJourneyData.get
      when(injected[NewPurchaseService].updatePurchase(any(),any(),any(),any(),any())(any())) thenReturn cachedJourneyData.get

      when(injected[views.html.new_other_goods.other_goods_input].apply(any(), any(), any(), any(), any(), any())(any(), any())) thenReturn Html("")

      rt(app, req)
    }
  }

  "Getting displayEditForm" should {

    "return a 404 when given an invalid iid" in new LocalSetup {


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/edit/missing-iid")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 500 when purchase is missing country" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        Some("nonEuOnly"),
        isVatResClaimed = None,
        bringingDutyFree = None,
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


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/iid0/tell-us")).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when missing currency" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        Some("nonEuOnly"),
        isVatResClaimed = None,
        bringingDutyFree = None,
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("other-goods/books"),
          "iid0",
          None,
          None,
          Some(Country("France", "FR", true, Nil)),
          None,
          Some(BigDecimal(12.99))
        ))
      ))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/iid0/tell-us")).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 404 when purchase has invalid product path" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        Some("nonEuOnly"),
        isVatResClaimed = None,
        bringingDutyFree = None,
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("invalid/product/path"),
          "iid0",
          None,
          None,
          Some(Country("France", "FR", true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        ))
      ))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/iid0/tell-us")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when all is ok" in new LocalSetup {


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/iid0/tell-us")).get
      status(result) shouldBe OK
    }
  }

  "Getting displayAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/invalid/path/tell-us")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when given a valid path" in new LocalSetup {

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us")).get
      status(result) shouldBe OK
    }
  }

  "Posting processAddForm" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/invalid/path/tell-us")).get
      status(result) shouldBe NOT_FOUND
    }

    "render the form with an extra cost input when action == add-cost" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "add-cost",
        "country" -> "France",
        "currency" -> "EUR",
        "costs[0]" -> "12.12"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe OK

      verify(injected[views.html.new_other_goods.other_goods_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe "France"
      formCaptor.getValue.data("currency") shouldBe "Euro (EUR)"
      formCaptor.getValue.data("costs[0]") shouldBe "12.12"
      formCaptor.getValue.data("costs[1]") shouldBe ""
    }

    "only render the form with up to 9 inputs when action == add-cost" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "add-cost",
        "country" -> "France",
        "currency" -> "EUR",
        "costs[0]" -> "12.10",
        "costs[1]" -> "12.11",
        "costs[2]" -> "12.12",
        "costs[3]" -> "12.13",
        "costs[4]" -> "12.14",
        "costs[5]" -> "12.15",
        "costs[6]" -> "12.16",
        "costs[7]" -> "12.17",
        "costs[8]" -> "12.18"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe OK

      verify(injected[views.html.new_other_goods.other_goods_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe "France"
      formCaptor.getValue.data("currency") shouldBe "Euro (EUR)"
      formCaptor.getValue.data("costs[0]") shouldBe "12.10"
      formCaptor.getValue.data("costs[8]") shouldBe "12.18"
      formCaptor.getValue.data.get("costs[9]") shouldBe None

    }

    "return a 400 if no action is supplied" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "country" -> "",
        "currency" -> ""
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and country not present" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "",
        "currency" -> "Euro (EUR)",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and country not valid" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "Not a real country",
        "currency" -> "Euro (EUR)",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and currency not present" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "France",
        "currency" -> "",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and currency not valid" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "France",
        "currency" -> "XXX",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when action == continue and cost not present" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "France",
        "currency" -> "Euro (EUR)"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a number of PPIs to the JourneyData and redirect to next step when action == continue and iid is not present" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "France",
        "currency" -> "Euro (EUR)",
        "costs[0]" -> "12.12",
        "costs[1]" -> "13.13"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("other-goods/adult/adult-clothing")),
        meq("France"),
        meq("EUR"),
        meq(List(BigDecimal(12.12), BigDecimal(13.13))),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }
  }

  "Posting processEditForm" should {

    "return a 404 when action == continue and iid is not found in journey data" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/edit/missing-iid").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "France",
        "currency" -> "Euro (EUR)",
        "costs[0]" -> "12.12"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND

    }

    "modify the relevant PPI in the JourneyData and redirect to next step when action == continue and iid is present" in new LocalSetup {

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/iid0/tell-us").withFormUrlEncodedBody(
        "action" -> "continue",
        "country" -> "France",
        "currency" -> "Euro (EUR)",
        "costs[0]" -> "12.12"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).updatePurchase(
        meq(ProductPath("other-goods/books")),
        meq("iid0"),
        meq("France"),
        meq("EUR"),
        meq(BigDecimal(12.12))
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

  }
}