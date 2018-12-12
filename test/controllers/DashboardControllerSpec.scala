package controllers

import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import services.{PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class DashboardControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[PurchasedProductService])
  }

  val controller: DashboardController = app.injector.instanceOf[DashboardController]

  trait LocalSetup {

    def travelDetailsJourneyData: JourneyData = JourneyData(euCountryCheck = Some("nonEuOnly"), ageOver17 = Some(true), privateCraft = Some(false))
    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(),any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(cachedJourneyData)

      rt(app, req)
    }
  }

  "Calling GET .../dashboard" should {
    "start a new session if any travel details are missing" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(privateCraft = None))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/dashboard")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/new-session")

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }
  }


  "respond with 200 and display the page if all travel details exist" in new LocalSetup {

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData)

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/dashboard").withFormUrlEncodedBody("firstName" -> "Harry", "lastName" -> "Potter", "passportNumber" -> "801375812", "placeOfArrival" -> "Newcastle airport")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text() shouldBe "Tell us about your purchases"
    Option(doc.getElementById("start-again")) should not be None

  }


  "Calling GET .../calculation" should {
    "redirect to the declaration done page if there is something to declare" in new LocalSetup {


      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
        Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","3.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")),
          ExchangeRate("1.20", "2018-10-29")))), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "1.00", "1.00", "3.00"))),
        Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("1.00", "1.00", "1.00", "3.00")
      ))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/calculation")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "You will need to pay £3.00for goods purchased outside of the EU"
    }
  }


  "redirect to the nothing to declare done page if there is nothing to declare" in new LocalSetup {


    override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
      calculatorResponse = Some(CalculatorResponse(
        Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","3.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")),
          ExchangeRate("1.20", "2018-10-29")))), Calculation("0.00","0.00","0.00","0.00"))), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "0.00")
      ))))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/calculation")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text shouldBe "You have nothing to declare"
  }
}
