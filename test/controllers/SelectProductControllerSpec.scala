/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import connectors.Cache
import models.{JourneyData, ProductAlias, ProductPath}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.Inspectors._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import repositories.BCPassengersSessionRepository
import services._
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future
import scala.language.postfixOps


class SelectProductControllerSpec extends BaseSpec {

  val requiredJourneyData = JourneyData(euCountryCheck = Some("nonEuOnly"), arrivingNICheck = Some(true), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false))


  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[SelectProductService].toInstance(MockitoSugar.mock[SelectProductService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[Cache], injected[SelectProductService])
  }


  trait LocalSetup {

    def result: Future[Result]
    def content: String = contentAsString(result)
    def doc: Document = Jsoup.parse(content)
    def h1: String = doc.getElementsByTag("h1").text
    def title: String = doc.title

    def cachedJourneyData: Option[JourneyData]

    def addSelectedProductsAsAliasesResult: JourneyData = JourneyData()


    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[SelectProductService].addSelectedProductsAsAliases(any(),any())(any())) thenReturn {
        Future.successful(addSelectedProductsAsAliasesResult)
      }

      when(injected[PurchasedProductService].clearWorkingInstance(any())(any(),any())) thenReturn Future.successful(cachedJourneyData.get)
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[Cache].storeJourneyData(any())(any())) thenReturn Future.successful(cachedJourneyData)

      rt(app, req)
    }

  }

  "Invoking clearAndAskProductSelection" should {

    "clear any selectedAliases from the Journey Data, cache it and redirect to askProductSelection with the correct path" in new LocalSetup {

      val localRequiredJourneyData = requiredJourneyData.copy(selectedAliases = List(
        ProductAlias("alcohol.beer", ProductPath("alcohol/beer"))
      ))

      override lazy val cachedJourneyData: Option[JourneyData] = Some(localRequiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-new-goods/alcohol")).get

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol")

        verify(injected[Cache], times(1)).storeJourneyData(meq(localRequiredJourneyData.copy(selectedAliases = Nil)))(any())

    }
  }

  "Invoking askProductSelection for branch items" should {

    "return the select products alcohol page given path /alcohol (branch)" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol")).get

      status(result) shouldBe OK
      h1 shouldBe "What type of alcohol are you bringing into the UK?"
      title shouldBe "What type of alcohol are you bringing into the UK? - Check tax on goods you bring into the UK - GOV.UK"
      forAll(List("beer", "wine", "cider", "spirits", "wine", "sparkling-wine")) { cb =>
        Option(doc.getElementById(cb)) should not be None
      }
    }


    "return the select products tobacco page given path /tobacco (branch)" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco")).get

      status(result) shouldBe OK
      h1 shouldBe "What type of tobacco are you bringing into the UK?"

      forAll(List("cigars", "cigarettes", "cigarillos", "rolling-tobacco", "chewing-tobacco")) { cb =>
        Option(doc.getElementById(cb)) should not be None
      }
    }


    "return the select products other goods page given path /other-goods (branch" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/other-goods")).get

      status(result) shouldBe OK
      h1 shouldBe "What other goods are you bringing into the UK?"
      Option(doc.getElementById("antiques")) should not be None
    }

    "return the select products page given path /other-goods/carpets-cotton-fabric" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/other-goods/carpets-fabric")).get

      status(result) shouldBe OK
      h1 shouldBe "What items of carpet or fabric are you bringing into the UK?"
      forAll(List("carpets", "fabrics")) { cb =>
        Option(doc.getElementById(cb)) should not be None
      }
    }

    "return the technical error page given an incorrect path" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/invalid/path")).get

      status(result) shouldBe NOT_FOUND
      h1 shouldBe "Technical problem"
    }

    "return the technical error page given a leaf path" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol/beer")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
      h1 shouldBe "Technical problem"
    }
  }

  "Invoking processSelectedProducts" should {

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol").withFormUrlEncodedBody("value" -> "bad_value")).get
      status(result) shouldBe BAD_REQUEST
    }

    "addSelectedProducts to keystore and then redirect to nextStep given valid checkbox values" in new LocalSetup {

      val localRequiredJourneyData = requiredJourneyData.copy(selectedAliases = List(
        ProductAlias("alcohol.beer", ProductPath("alcohol/beer")),
        ProductAlias("alcohol.cider", ProductPath("alcohol/cider")),
        ProductAlias("tobacco.cigars", ProductPath("tobacco/cigars"))
      ))

      override lazy val cachedJourneyData: Option[JourneyData] = Some(localRequiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol").withFormUrlEncodedBody("tokens[0]" -> "beer")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[SelectProductService], times(1)).addSelectedProductsAsAliases(meq(localRequiredJourneyData), meq(List(ProductPath("alcohol/beer"))))(any())
      verify(injected[Cache], times(1)).fetch(any())

    }
  }

  "Invoking processProductSelectionOtherGoods" should {

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/other-goods").withFormUrlEncodedBody("value" -> "bad_value")).get
      status(result) shouldBe BAD_REQUEST
    }

    "addSelectedProducts to keystore and then redirect to searchGoods when the value is a ProductTreeBranch" in new LocalSetup {

      override lazy val addSelectedProductsAsAliasesResult = JourneyData(selectedAliases = List(
        ProductAlias("label.carpets-fabric", ProductPath("other-goods/carpets-fabric"))
      ))


      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/other-goods").withFormUrlEncodedBody("tokens[0]" -> "carpets-fabric")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "addSelectedProducts to keystore and then redirect to searchGoods when the value is a ProductTreeLeaf" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/other-goods").withFormUrlEncodedBody("tokens[0]" -> "car-seats")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/other-goods/add")

    }
  }

  "Invoking nextStep" should {

    trait NextStepSetup {

      def selectedProducts: List[ProductAlias]

      lazy val response = {

        import play.api.test.Helpers.route

        when(injected[Cache].fetch(any())) thenReturn{
          Future.successful(Some(requiredJourneyData.copy( selectedAliases = selectedProducts )))
        }
        when(injected[SelectProductService].removeSelectedAlias(any())(any())) thenReturn{
          Future.successful(JourneyData())
        }

        route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")).get
      }
    }

    "redirect to dashboard page when journeyData.selectedProducts returns an empty list" in new NextStepSetup {

      override val selectedProducts = Nil

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tell-us")
      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[SelectProductService], times(0)).removeSelectedAlias(any())(any())
    }

    "inform the user the item is not found when journeyData.selectedProducts contains an invalid path" in new NextStepSetup {

      override val selectedProducts = List(ProductAlias("other-goods.invalid", ProductPath("other-goods/invalid")))

      status(response) shouldBe NOT_FOUND
      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[SelectProductService], times(1)).removeSelectedAlias(any())(any())
    }

    "go to purchase input form when journeyData.selectedProducts contains a leaf" in new NextStepSetup {

      override val selectedProducts = List(ProductAlias("other-goods.books", ProductPath("other-goods/books")))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/books/tell-us")
      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[SelectProductService], times(1)).removeSelectedAlias(any())(any())
    }

    "redirect to selectProducts when journeyData.selectedProducts contains a branch" in new NextStepSetup {

      override val selectedProducts = List(ProductAlias("alcohol", ProductPath("alcohol")))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol")
      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[SelectProductService], times(1)).removeSelectedAlias(any())(any())
    }
  }
}
