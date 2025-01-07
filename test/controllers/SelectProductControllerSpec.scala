/*
 * Copyright 2024 HM Revenue & Customs
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

import config.AppConfig
import connectors.Cache
import models.{JourneyData, ProductAlias, ProductPath, PurchasedProductInstance}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.Inspectors._
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import play.api.test.Helpers.{route => rt, _}
import repositories.BCPassengersSessionRepository
import services._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import views.html.errorTemplate

import scala.concurrent.{ExecutionContext, Future}

class SelectProductControllerSpec extends BaseSpec {

  val requiredJourneyData: JourneyData = JourneyData(
    prevDeclaration = Some(false),
    euCountryCheck = Some("nonEuOnly"),
    arrivingNICheck = Some(true),
    isVatResClaimed = None,
    isBringingDutyFree = None,
    bringingOverAllowance = Some(true),
    ageOver17 = Some(true),
    privateCraft = Some(false)
  )

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[SelectProductService].toInstance(mock(classOf[SelectProductService])))
    .overrides(bind[PurchasedProductService].toInstance(mock(classOf[PurchasedProductService])))
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach(): Unit = {
    reset(injected[Cache])
    reset(injected[SelectProductService])
  }

  val controllerHelpers: ControllerHelpers = new Object with ControllerHelpers {
    override def cache: Cache                                                 = mock(classOf[Cache])
    override def productTreeService: ProductTreeService                       = mock(classOf[ProductTreeService])
    override def calculatorService: CalculatorService                         = mock(classOf[CalculatorService])
    override def errorTemplate: errorTemplate                                 = mock(classOf[errorTemplate])
    override implicit def appConfig: AppConfig                                = mock(classOf[AppConfig])
    override implicit def ec: ExecutionContext                                = mock(classOf[ExecutionContext])
    override protected def controllerComponents: MessagesControllerComponents =
      mock(classOf[MessagesControllerComponents])
  }

  trait LocalSetup {

    def result: Future[Result]
    def content: String = contentAsString(result)
    def doc: Document   = Jsoup.parse(content)
    def h1: String      = doc.getElementsByTag("h1").text
    def title: String   = doc.title

    def cachedJourneyData: Option[JourneyData]

    def addSelectedProductsAsAliasesResult(): JourneyData = JourneyData()

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[SelectProductService].addSelectedProductsAsAliases(any(), any())(any())) thenReturn {
        Future.successful(addSelectedProductsAsAliasesResult())
      }

      when(injected[PurchasedProductService].clearWorkingInstance(any())(any(), any())) `thenReturn` Future.successful(
        cachedJourneyData.get
      )
      when(injected[Cache].fetch(any())) `thenReturn` Future.successful(cachedJourneyData)
      when(injected[Cache].storeJourneyData(any())(any())) `thenReturn` Future.successful(cachedJourneyData)

      rt(app, req)
    }

  }

  "Invoking clearAndAskProductSelection" should {

    "clear any selectedAliases from the Journey Data, cache it and redirect to askProductSelection with the correct path" in new LocalSetup {

      val localRequiredJourneyData: JourneyData = requiredJourneyData.copy(selectedAliases =
        List(
          ProductAlias("alcohol.beer", ProductPath("alcohol/beer"))
        )
      )

      override lazy val cachedJourneyData: Option[JourneyData] = Some(localRequiredJourneyData)

      override val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-new-goods/alcohol")).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol")

      verify(injected[Cache], times(1)).storeJourneyData(meq(localRequiredJourneyData.copy(selectedAliases = Nil)))(
        any()
      )

    }
  }

  "Invoking askProductSelection for branch items" should {

    "return the select products alcohol page given path /alcohol (branch)" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol")).get

      status(result) shouldBe OK
      h1             shouldBe "What type of alcohol do you want to add?"
      title          shouldBe "What type of alcohol do you want to add? - Check tax on goods you bring into the UK - GOV.UK"
      forAll(
        List("tokens-beer", "tokens-wine", "tokens-cider", "tokens-spirits", "tokens-wine", "tokens-sparkling-wine")
      ) { cb =>
        Option(doc.getElementById(cb)) should not be None
      }
    }

    "return the select products tobacco page given path /tobacco (branch)" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco")).get

      status(result) shouldBe OK
      h1             shouldBe "What type of tobacco do you want to add?"

      forAll(
        List(
          "tokens-cigars",
          "tokens-cigarettes",
          "tokens-cigarillos",
          "tokens-rolling-tobacco",
          "tokens-chewing-tobacco",
          "tokens-heated-tobacco"
        )
      ) { cb =>
        Option(doc.getElementById(cb)) should not be None
      }
    }

    "return the select products other goods page given path /other-goods" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/other-goods")).get

      status(result)                              shouldBe OK
      h1                                          shouldBe "What type of other goods do you want to add?"
      Option(doc.getElementById("tokens-antiques")) should not be None
    }

    Seq(
      ("other-goods/carpets-fabric", List("tokens-carpets", "tokens-fabrics"), "items of carpet or fabric"),
      ("other-goods/adult", List("tokens-adult-clothing", "tokens-adult-footwear"), "adult clothing or footwear"),
      (
        "other-goods/childrens",
        List("tokens-childrens-clothing", "tokens-childrens-footwear"),
        "children’s clothing or footwear"
      ),
      ("other-goods/electronic-devices", List("tokens-televisions", "tokens-other"), "electronic devices")
    ).foreach { case (path, idList, specificContent) =>
      s"return the select products page given path $path with two radio options" in new LocalSetup {
        override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

        override val result: Future[Result] = route(
          app,
          enhancedFakeRequest("GET", s"/check-tax-on-goods-you-bring-into-the-uk/select-goods/$path")
        ).get

        status(result) shouldBe OK
        h1             shouldBe s"What $specificContent do you want to add?"
        forAll(idList) { cb =>
          Option(doc.getElementById(cb)) should not be None
        }
      }
    }

    "return the technical error page given an incorrect path" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/invalid/path")
      ).get

      status(result) shouldBe NOT_FOUND
      h1             shouldBe "Sorry, there is a problem with the service"
    }

    "return the technical error page given a leaf path" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol/beer")
      ).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
      h1             shouldBe "Sorry, there is a problem with the service"
    }
  }

  "Invoking processSelectedProducts" should {

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol")
          .withFormUrlEncodedBody("value" -> "bad_value")
      ).get
      status(result) shouldBe BAD_REQUEST
    }

    "addSelectedProducts to keystore and then redirect to nextStep given valid product" in new LocalSetup {

      val localRequiredJourneyData: JourneyData = requiredJourneyData.copy(selectedAliases =
        List(
          ProductAlias("alcohol.beer", ProductPath("alcohol/beer"))
        )
      )

      override lazy val cachedJourneyData: Option[JourneyData] = Some(localRequiredJourneyData)

      override val result: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol")
          .withFormUrlEncodedBody("tokens" -> "beer")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[SelectProductService], times(1))
        .addSelectedProductsAsAliases(meq(localRequiredJourneyData), meq(List(ProductPath("alcohol/beer"))))(any())
      verify(injected[Cache], times(1)).fetch(any())
    }
  }

  "Invoking processProductSelectionOtherGoods" should {

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      override val result: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/other-goods")
          .withFormUrlEncodedBody("value" -> "bad_value")
      ).get
      status(result) shouldBe BAD_REQUEST
    }

    "addSelectedProducts to keystore and then redirect to searchGoods when the value is a ProductTreeBranch" in new LocalSetup {

      override def addSelectedProductsAsAliasesResult(): JourneyData = JourneyData(selectedAliases =
        List(
          ProductAlias("label.other-goods.carpets-fabric.fabrics", ProductPath("other-goods/carpets-fabric/fabrics"))
        )
      )

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/other-goods")
          .withFormUrlEncodedBody("tokens" -> "carpets-fabric")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us"
      )
    }

    "addSelectedProducts to keystore and then redirect to searchGoods when the value is a ProductTreeLeaf" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/other-goods")
          .withFormUrlEncodedBody("tokens" -> "car-seats")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us"
      )
    }

    Seq(
      ("other-goods/carpets-fabric", "carpets"),
      ("other-goods/carpets-fabric", "fabrics"),
      ("other-goods/adult", "adult-clothing"),
      ("other-goods/adult", "adult-footwear"),
      ("other-goods/childrens", "childrens-clothing"),
      ("other-goods/childrens", "childrens-footwear"),
      ("other-goods/electronic-devices", "televisions"),
      ("other-goods/electronic-devices", "other")
    ).foreach { case (path, token) =>
      s"redirect to other goods tell us page with path /enter-goods/other-goods/tell-us for the submitted token $token" in new LocalSetup {
        override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

        val result: Future[Result] = route(
          app,
          enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/select-goods/$path")
            .withFormUrlEncodedBody("tokens" -> token)
        ).get

        val redirectUrl: String = redirectLocation(result).get

        status(result) shouldBe SEE_OTHER
        redirectUrl    shouldBe "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us"

        val redirectResult: Future[Result] = route(app, enhancedFakeRequest("GET", redirectUrl)).get

        contentAsString(redirectResult) should include("Tell us about the Item of other goods")
      }
    }

    Seq(
      ("other-goods/carpets-fabric", "tokens-carpets", "Select carpets or fabrics"),
      ("other-goods/adult", "tokens-adult-clothing", "Select adult clothing or adult footwear"),
      ("other-goods/childrens", "tokens-childrens-clothing", "Select children’s clothing or children’s footwear"),
      ("other-goods/electronic-devices", "tokens-televisions", "Select televisions or all other electronic devices")
    ).foreach { case (path, firstToken, errorMessage) =>
      s"return 400 when no radio option is submitted for the path $path displaying error message" which {
        "links to the first radio option" in new LocalSetup {
          override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

          val result: Future[Result] = route(
            app,
            enhancedFakeRequest("POST", s"/check-tax-on-goods-you-bring-into-the-uk/select-goods/$path")
          ).get

          status(result) shouldBe BAD_REQUEST

          doc.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"

          doc
            .getElementsByClass("govuk-error-summary__body")
            .select(s"a[href=#$firstToken]")
            .html() shouldBe errorMessage
        }
      }
    }
  }

  "Invoking nextStep" should {

    trait NextStepSetup {

      def selectedProducts: List[ProductAlias]
      def journeyData: JourneyData = requiredJourneyData

      lazy val response: Future[Result] = {

        import play.api.test.Helpers.route

        when(injected[Cache].fetch(any())) thenReturn {
          Future.successful(Some(journeyData.copy(selectedAliases = selectedProducts)))
        }
        when(injected[SelectProductService].removeSelectedAlias(any())(any())) thenReturn {
          Future.successful(JourneyData())
        }

        when(injected[Cache].store(any())(any())) `thenReturn` Future.successful(JourneyData())

        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")).get
      }
    }

    "redirect to dashboard page when journeyData.selectedProducts returns an empty list" in new NextStepSetup {

      override val selectedProducts: List[ProductAlias] = Nil

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tell-us")
      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[SelectProductService], times(0)).removeSelectedAlias(any())(any())
    }

    "inform the user the item is not found when journeyData.selectedProducts contains an invalid path" in new NextStepSetup {

      override val selectedProducts: List[ProductAlias] =
        List(ProductAlias("other-goods.invalid", ProductPath("other-goods/invalid")))

      status(response) shouldBe NOT_FOUND
      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[SelectProductService], times(1)).removeSelectedAlias(any())(any())
    }

    "go to purchase input form when journeyData.selectedProducts contains a leaf" in new NextStepSetup {

      override val selectedProducts: List[ProductAlias] =
        List(ProductAlias("other-goods.books", ProductPath("other-goods/books")))

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tell-us")
      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[SelectProductService], times(1)).removeSelectedAlias(any())(any())
    }

    "redirect to selectProducts when journeyData.selectedProducts contains a branch" in new NextStepSetup {

      override val selectedProducts: List[ProductAlias] = List(ProductAlias("alcohol", ProductPath("alcohol")))

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol")
      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[SelectProductService], times(1)).removeSelectedAlias(any())(any())
    }

  }

  "Invoking cancel" should {

    trait CancelSetup {

      def selectedProducts: List[ProductAlias]

      def journeyData: JourneyData = requiredJourneyData

      lazy val response: Future[Result] = {
        import play.api.test.Helpers.route

        when(injected[Cache].fetch(any())) thenReturn {
          Future.successful(Some(journeyData.copy(selectedAliases = selectedProducts)))
        }
        when(injected[SelectProductService].removeSelectedAlias(any())(any())) thenReturn {
          Future.successful(JourneyData())
        }
        when(injected[Cache].store(any())(any())) `thenReturn` Future.successful(JourneyData())

        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/cancel")).get
      }
    }

    "redirect to next step after clearing working instance from cache when adding a product" in new CancelSetup {

      override val selectedProducts: List[ProductAlias] = List(ProductAlias("alcohol", ProductPath("alcohol")))
      val workingInstance: PurchasedProductInstance     =
        PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"))
      val incompleteGbNiPpi: PurchasedProductInstance   =
        PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"))
      val localRequiredJourneyData: JourneyData         = requiredJourneyData.copy(
        workingInstance = Some(workingInstance),
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        purchasedProductInstances = List(incompleteGbNiPpi)
      )
      override val journeyData: JourneyData             = localRequiredJourneyData
      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
      verify(injected[Cache], times(1)).store(any())(any())
    }

    "redirect to next step after clearing working instance from cache when editing a product" in new CancelSetup {

      override val selectedProducts: List[ProductAlias] = List(ProductAlias("alcohol", ProductPath("alcohol")))
      val workingInstance: PurchasedProductInstance     =
        PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"), cost = Some(BigDecimal(100)))
      val incompleteGbNiPpi: PurchasedProductInstance   =
        PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"))
      val localRequiredJourneyData: JourneyData         = requiredJourneyData.copy(
        workingInstance = Some(workingInstance),
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        purchasedProductInstances = List(incompleteGbNiPpi)
      )
      override val journeyData: JourneyData             = localRequiredJourneyData
      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
      verify(injected[Cache], times(1)).store(any())(any())
    }
  }

}
