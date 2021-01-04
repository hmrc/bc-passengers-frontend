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
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{status, route => rt, _}
import play.twirl.api.Html
import repositories.BCPassengersSessionRepository
import services.{CalculatorService, LimitUsageSuccessResponse, NewPurchaseService}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import views.html.tobacco._

import scala.concurrent.Future

class TobaccoInputControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[NewPurchaseService].toInstance(MockitoSugar.mock[NewPurchaseService]))
    .overrides(bind[CalculatorService].toInstance(MockitoSugar.mock[CalculatorService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[views.html.tobacco.no_of_sticks_input].toInstance(MockitoSugar.mock[views.html.tobacco.no_of_sticks_input]))
    .overrides(bind[views.html.tobacco.weight_or_volume_input].toInstance(MockitoSugar.mock[views.html.tobacco.weight_or_volume_input]))
    .overrides(bind[views.html.tobacco.no_of_sticks_weight_or_volume_input].toInstance(MockitoSugar.mock[views.html.tobacco.no_of_sticks_weight_or_volume_input]))
    .build()

  override def beforeEach: Unit = {
    reset(injected[Cache], injected[NewPurchaseService], injected[no_of_sticks_input])
    reset(injected[Cache], injected[NewPurchaseService], injected[weight_or_volume_input])
    reset(injected[Cache], injected[NewPurchaseService], injected[no_of_sticks_weight_or_volume_input])
  }

  trait LocalSetup {

    def productPath: ProductPath
    def weightOrVolume: Option[BigDecimal]
    def noOfSticks: Option[Int]

    lazy val cachedJourneyData = Some(JourneyData(
      prevDeclaration = Some(false),
      Some("nonEuOnly"),
      arrivingNICheck = Some(true),
      isVatResClaimed = Some(true),
      isBringingDutyFree = None,
      bringingOverAllowance = Some(true),
      privateCraft = Some(false),
      ageOver17 = Some(true),
      purchasedProductInstances = List(PurchasedProductInstance(
        productPath,
        "iid0",
        weightOrVolume,
        noOfSticks,
        Some(Country("FR", "title.france", "FR", true, Nil)),
        Some("EUR"),
        Some(BigDecimal(12.99))
      ))
    ))

    def fakeLimits: Map[String, String]

    val formCaptor = ArgumentCaptor.forClass(classOf[Form[TobaccoDto]])

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[Cache].store(any())(any())) thenReturn Future.successful(JourneyData())

      when(injected[CalculatorService].limitUsage(any())(any())) thenReturn Future.successful(LimitUsageSuccessResponse(fakeLimits))

      when(injected[NewPurchaseService].insertPurchases(any(), any(), any(), any(), any(), any(), any())(any())) thenReturn cachedJourneyData.get
      when(injected[NewPurchaseService].updatePurchase(any(), any(), any(), any(), any(), any(), any())(any())) thenReturn cachedJourneyData.get

      when(injected[no_of_sticks_input].apply(any(), any(), any(), any(), any(), any())(any(), any())) thenReturn Html("")
      when(injected[weight_or_volume_input].apply(any(), any(), any(), any(), any(), any())(any(), any())) thenReturn Html("")
      when(injected[no_of_sticks_weight_or_volume_input].apply(any(), any(), any(), any(), any(), any())(any(), any())) thenReturn Html("")

      rt(app, req)
    }
  }

  "Getting enter-goods/tobacco/*/tell-us" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/invalid/path/tell-us")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when given a valid path for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us")).get
      status(result) shouldBe OK
    }

    "return a 200 when given a valid path for rolling tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = None

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us")).get
      status(result) shouldBe OK
    }

    "return a 200 when given a valid path for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")).get
      status(result) shouldBe OK
    }

    "return a 200 when given a valid path for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us")).get
      status(result) shouldBe OK
    }

    "display default country and currency if set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      override lazy val cachedJourneyData = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          productPath,
          "iid0",
          weightOrVolume,
          noOfSticks,
          Some(Country("FR", "title.france", "FR", true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        )),
        defaultCountry = Some("FR"),
        defaultCurrency = Some("EUR")
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")).get

      status(result) shouldBe OK

      verify(injected[no_of_sticks_weight_or_volume_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe "FR"
      formCaptor.getValue.data("currency") shouldBe "EUR"
    }

    "not display default country and currency if not set in JourneyData" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      override lazy val cachedJourneyData = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          productPath,
          "iid0",
          weightOrVolume,
          noOfSticks,
          Some(Country("FR", "title.france", "FR", true, Nil)),
          Some("EUR"),
          Some(BigDecimal(12.99))
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us")).get

      status(result) shouldBe OK

      verify(injected[no_of_sticks_weight_or_volume_input], times(1))(formCaptor.capture(), any(), any(), any(), any(), any())(any(), any())

      formCaptor.getValue.data("country") shouldBe ""
      formCaptor.getValue.data("currency") shouldBe ""
    }
  }

  "Posting /enter-goods/tobacco/*/tell-us" should {

    "return a 404 when given an invalid path" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/invalid/path/tell-us")).get
      status(result) shouldBe NOT_FOUND
    }


    "return a 400 when country not present for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us").withFormUrlEncodedBody(
        "country" -> "",
        "currency" -> "EUR",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us").withFormUrlEncodedBody(
        "country" -> "Not a real country",
        "currency" -> "EUR",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "Not a valid currency",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "400",
        "cost" -> ""
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks not present for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfStick not valid for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "invalid noOfSticks",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI to the JourneyData and redirect to next step for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("tobacco/cigarettes")),
        any(),
        meq(Some(400)),
        meq("FR"),
        meq("EUR"),
        meq(List(BigDecimal(92.50))),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "return a 400 when country not present for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "",
        "currency" -> "EUR",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "Not a real country",
        "currency" -> "EUR",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "Not a valid currency",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "400",
        "cost" -> ""
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks not present for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfStick not valid for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "invalid noOfSticks",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI to the JourneyData and redirect to next step for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/heated-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("tobacco/heated-tobacco")),
        any(),
        meq(Some(400)),
        meq("FR"),
        meq("EUR"),
        meq(List(BigDecimal(92.50))),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "return a 400 when country not present for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = None

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "",
        "currency" -> "EUR",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = None

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "Not a real country",
        "currency" -> "EUR",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = None

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = None

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "Not a valid currency",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = None

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "400.0",
        "cost" -> ""
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not present for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = None

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not valid for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = None

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "Invalid weightOrVolume",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI to the JourneyData and redirect to next step for rolling-tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/rolling-tobacco")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = None

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/rolling-tobacco/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("tobacco/rolling-tobacco")),
        meq(Some(BigDecimal(0.4))),
        any(),
        meq("FR"),
        meq("EUR"),
        meq(List(BigDecimal(92.50))),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }

    "return a 400 when country not present for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "",
        "currency" -> "EUR",
        "noOfSticks" -> "50",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when country not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "Invalid country",
        "currency" -> "EUR",
        "noOfSticks" -> "50",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not present for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "",
        "noOfSticks" -> "50",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when currency not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "Invalid currency",
        "noOfSticks" -> "50",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when cost not present for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "50",
        "weightOrVolume" -> "400.0",
        "cost" -> ""
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not present for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "50",
        "weightOrVolume" -> "",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when weightOrVolume not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "50",
        "weightOrVolume" -> "Invalid weightOrVolume",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks not present for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "Invalid noOfSticks",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks and weightOrVolume not present for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "",
        "weightOrVolume" -> "",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "return a 400 when noOfSticks and weightOrVolume not valid for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "Invalid noOfSticks",
        "weightOrVolume" -> "Invalid weightOrVolume",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe BAD_REQUEST
    }

    "add a PPI to the JourneyData and redirect to next step for cigars" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(0.6))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigars/tell-us").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "50",
        "weightOrVolume" -> "400.0",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).insertPurchases(
        meq(ProductPath("tobacco/cigars")),
        meq(Some(BigDecimal(0.4))),
        meq(Some(50)),
        meq("FR"),
        meq("EUR"),
        meq(List(BigDecimal(92.50))),
        any()
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }
  }

  "Getting displayNoOfSticksEditForm" should {

    "return a 404 when given an invalid iid" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/missing-iid/edit")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 500 when purchase is missing country" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      override lazy val cachedJourneyData = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid0",
          None,
          Some(400),
          None,
          Some("EUR"),
          Some(BigDecimal(92.50))
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 when missing currency" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      override lazy val cachedJourneyData = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid0",
          None,
          Some(400),
          Some(Country("FR", "title.france", "FR", true, Nil)),
          None,
          Some(BigDecimal(92.50))
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 404 when purchase has invalid product path" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      override lazy val cachedJourneyData = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("tobacco/invalid/path"),
          "iid0",
          None,
          Some(400),
          Some(Country("FR", "title.france", "FR", true, Nil)),
          Some("EUR"),
          Some(BigDecimal(92.50))
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")).get
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 when all is ok for cigarettes" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      override lazy val cachedJourneyData = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("tobacco/cigarettes"),
          "iid0",
          None,
          Some(400),
          Some(Country("FR", "title.france", "FR", true, Nil)),
          Some("EUR"),
          Some(BigDecimal(92.50))
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")).get
      status(result) shouldBe OK
    }

    "return a 200 when all is ok for heated tobacco" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/heated-tobacco")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      override lazy val cachedJourneyData = Some(JourneyData(
        prevDeclaration = Some(false),
        Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        privateCraft = Some(false),
        ageOver17 = Some(true),
        purchasedProductInstances = List(PurchasedProductInstance(
          ProductPath("tobacco/heated-tobacco"),
          "iid0",
          None,
          Some(400),
          Some(Country("FR", "title.france", "FR", true, Nil)),
          Some("EUR"),
          Some(BigDecimal(92.50))
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit")).get
      status(result) shouldBe OK
    }
  }

  "Posting processEditForm" should {

    "return a 404 when iid is not found in journey data" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigarettes")
      override def weightOrVolume: Option[BigDecimal] = None
      override def noOfSticks: Option[Int] = Some(400)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/missing-iid/edit").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "noOfSticks" -> "400",
        "cost" -> "92.50"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe NOT_FOUND

    }

    "modify the relevant PPI in the JourneyData and redirect to next step" in new LocalSetup {

      override lazy val fakeLimits = Map[String, String]()

      override def productPath: ProductPath = ProductPath("tobacco/cigars")
      override def weightOrVolume: Option[BigDecimal] = Some(BigDecimal(20.0))
      override def noOfSticks: Option[Int] = Some(150)

      val req = EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/iid0/edit").withFormUrlEncodedBody(
        "country" -> "FR",
        "currency" -> "EUR",
        "weightOrVolume" -> "400.0",
        "noOfSticks" -> "50",
        "cost" -> "98.00"
      )

      val result: Future[Result] = route(app, req).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

      verify(injected[NewPurchaseService], times(1)).updatePurchase(
        meq(ProductPath("tobacco/cigars")),
        meq("iid0"),
        meq(Some(BigDecimal(0.4))),
        meq(Some(50)),
        meq("FR"),
        meq("EUR"),
        meq(BigDecimal(98.00))
      )(any())

      verify(injected[Cache], times(1)).store(any())(any())
    }
  }
}
