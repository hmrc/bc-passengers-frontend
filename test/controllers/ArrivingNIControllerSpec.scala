package controllers

import config.AppConfig
import connectors.Cache
import models.JourneyData
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, route, status}
import play.api.test.Helpers._
import services.TravelDetailsService
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class ArrivingNIControllerSpec extends BaseSpec {

  val mockTravelDetailService = MockitoSugar.mock[TravelDetailsService]
  val mockCache = MockitoSugar.mock[Cache]
  val mockAppConfig = MockitoSugar.mock[AppConfig]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(mockTravelDetailService))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[AppConfig].toInstance((mockAppConfig)))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTravelDetailService, mockCache, mockAppConfig)
  }
  "loadArrivingNIPage" should {
    "load the page" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some("nonEuOnly")))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Is your final destination Northern Ireland?"
    }

    "loading the page and populate data from keyStore" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some("nonEuOnly"),Some(true)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Is your final destination Northern Ireland?"
      doc.select("#arrivingNI-true").hasAttr("checked") shouldBe true
    }

    "redirect the page" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }
  }

  "postArrivingNIPage" should {

    "redirect to .../goods-bought-outside-eu when user says they have only arrived from countries outside EU when isVatResJourneyEnabled is true" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true
      when(mockTravelDetailService.storeArrivingNI(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni").withFormUrlEncodedBody("arrivingNI" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-bought-outside-eu")

      verify(mockTravelDetailService, times(1)).storeArrivingNI(any())(meq(true))(any())
    }


    "redirect to .../did-you-claim-tax-back when user says they have arrived from EU when isVatResJourneyEnabled is true" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true
      when(mockTravelDetailService.storeArrivingNI(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni").withFormUrlEncodedBody("arrivingNI" -> "false")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")


      verify(mockTravelDetailService, times(1)).storeArrivingNI(any())(meq(false))(any())
    }

    "redirect to .../goods-bought-outside-eu when user says they have only arrived from countries outside EU when isVatResJourneyEnabled is false" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn false
      when(mockTravelDetailService.storeArrivingNI(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni").withFormUrlEncodedBody("arrivingNI" -> "false")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-bought-outside-eu")

            verify(mockTravelDetailService, times(1)).storeArrivingNI(any())(meq(false))(any())
    }


    "redirect to .../goods-bought-inside-eu  when user says they have arrived from EU when isVatResJourneyEnabled is false" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn false
      when(mockTravelDetailService.storeArrivingNI(any())(any())(any())) thenReturn cachedJourneyData



      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni").withFormUrlEncodedBody("arrivingNI" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-bought-inside-eu")


      verify(mockTravelDetailService, times(1)).storeArrivingNI(any())(meq(true))(any())
    }

    "return a bad request when user selects an invalid value" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("None"))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn false

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni").withFormUrlEncodedBody("arrivingNI" -> "dummy")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Is your final destination Northern Ireland?"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#arrivingNI]").html() shouldBe "Select yes if your final destination is Northern Ireland"
      doc.getElementById("arrivingNI").getElementsByClass("error-message").html() shouldBe "Select yes if your final destination is Northern Ireland"
      verify(mockTravelDetailService, times(0)).storeArrivingNI(any())(any())(any())
    }

  }

}
