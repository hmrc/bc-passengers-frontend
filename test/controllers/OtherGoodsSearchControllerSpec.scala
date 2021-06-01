/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import connectors.Cache
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.data.Form
import play.api.http.Writeable
import play.api.inject.bind
import org.jsoup.nodes.Document
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import play.twirl.api.Html
import repositories.BCPassengersSessionRepository
import services.{DeclarationServiceSuccessResponse, PayApiServiceSuccessResponse}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import views.html.other_goods.other_goods_input

import scala.concurrent.Future

class OtherGoodsSearchControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[other_goods_input].toInstance(MockitoSugar.mock[other_goods_input]))
    .build()

  override def beforeEach: Unit = {
    reset(injected[Cache], injected[other_goods_input])
  }

  trait LocalSetup {
    lazy val declarationResponse = DeclarationResponse(calculation = calculation, oldPurchaseProductInstances = oldPurchasedProductInstances, liabilityDetails = liabilityDetails)
    lazy val oldPurchasedProductInstances: List[PurchasedProductInstance] = List(oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks,oldBooks)
    lazy val calculation = Calculation("1.00","1.00","1.00","3.00")
    lazy val oldBooks: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/books"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None,None,None, isEditable = Some(false))
    lazy val liabilityDetails = LiabilityDetails("32.0","0.0","126.4","158.40")
    val requiredJourneyData: JourneyData = JourneyData(
      prevDeclaration = Some(false),
      Some("nonEuOnly"),
      arrivingNICheck = Some(true),
      isVatResClaimed = None,
      isBringingDutyFree = None,
      bringingOverAllowance = Some(true),
      privateCraft = Some(false),
      ageOver17 = Some(true),
      selectedAliases = List(ProductAlias("label.gold", ProductPath("other-goods/jewellery"))),
      purchasedProductInstances = List(PurchasedProductInstance(
        ProductPath("other-goods/books"),
        "iid0",
        None,
        None,
        Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
        None,
        Some("EUR"),
        Some(BigDecimal(12.99))
      ))
    )

    def cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

    val formCaptor: ArgumentCaptor[Form[OtherGoodsDto]] = ArgumentCaptor.forClass(classOf[Form[OtherGoodsDto]])

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[Cache].storeJourneyData(any())(any())) thenReturn Future.successful(cachedJourneyData)

      when(injected[other_goods_input].apply(any(), any(), any(), any(), any(), any(), any(), any())(any(), any())) thenReturn Html("")

      rt(app, req)
    }
  }




  "Calling clearAndSearchGoods" should {

    "Clear the contents of selectedAliases then redirect to searchGoods" in new LocalSetup {

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add-new")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/other-goods/add")

      verify(injected[Cache], times(1)).storeJourneyData(meq(requiredJourneyData.copy(selectedAliases = Nil)))(any())
    }
  }


  "Calling searchGoods" should {

    "Return 200" in new LocalSetup {

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add")).get
      status(result) shouldBe OK
    }
  }

  "Calling processSearchGoods" should {

    "Return BAD_REQUEST when no form data is submitted" in new LocalSetup {

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add")).get

      status(result) shouldBe BAD_REQUEST
    }

    "Return BAD_REQUEST when action=add but no searchTerm supplied" in new LocalSetup {
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add").withFormUrlEncodedBody(
        "action" -> "add"
      )).get

      status(result) shouldBe BAD_REQUEST
    }

    "Return BAD_REQUEST when action=add but 50 items are already there in oldpurchasedproductinstance" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(prevDeclaration = Some(true), euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), declarationResponse = Some(declarationResponse)))
      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add")).get
      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)
      doc.getElementsByClass("panel-border-wide").text() shouldBe "You cannot use this service to declare more than 50 other goods. You must declare any goods over this limit in person to Border Force when you arrive in the UK. Use the red ‘goods to declare’ channel or the red-point phone."
    }

    "Return SEE_OTHER when action=add and a valid searchTerm is supplied" in new LocalSetup {
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add").withFormUrlEncodedBody(
        "action" -> "add",
        "searchTerm" -> "label.other-goods.gold"
      )).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/other-goods/add#searchTerm")
    }

    "Return SEE_OTHER when action is not supplied and a valid searchTerm is supplied" in new LocalSetup {
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add").withFormUrlEncodedBody(
        "searchTerm" -> "label.other-goods.gold"
      )).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/other-goods/add#searchTerm")
    }

    "Return BAD_REQUEST when action=continue and an no goods are added" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(selectedAliases = Nil))
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add").withFormUrlEncodedBody(
        "action" -> "continue"
      )).get

      status(result) shouldBe BAD_REQUEST
    }

    "Return BAD_REQUEST when action=add and an invalid searchTerm is supplied" in new LocalSetup {
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add").withFormUrlEncodedBody(
        "action" -> "add",
        "searchTerm" -> "anything"
      )).get

      status(result) shouldBe BAD_REQUEST
    }

    "Return BAD_REQUEST when action is not supplied and an invalid searchTerm is supplied" in new LocalSetup {
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add").withFormUrlEncodedBody(
        "searchTerm" -> "anything"
      )).get

      status(result) shouldBe BAD_REQUEST
    }
    
    "Return SEE_OTHER when action=continue" in new LocalSetup {
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add").withFormUrlEncodedBody(
        "action" -> "continue"
      )).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
    }

    "Return BAD_REQUEST when remove=string" in new LocalSetup {
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add").withFormUrlEncodedBody(
        "remove" -> "string"
      )).get

      status(result) shouldBe BAD_REQUEST
    }

    "Return SEE_OTHER when remove=0" in new LocalSetup {
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/other-goods/add").withFormUrlEncodedBody(
        "remove" -> "0"
      )).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/other-goods/add")
    }

  }
}
