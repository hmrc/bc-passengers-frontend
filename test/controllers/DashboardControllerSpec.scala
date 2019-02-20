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


  trait LocalSetup {

    def travelDetailsJourneyData: JourneyData = JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, bringingDutyFree = None,  ageOver17 = Some(true), privateCraft = Some(false))
    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(),any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(cachedJourneyData)

      rt(app, req)
    }
  }

  val controller: DashboardController = app.injector.instanceOf[DashboardController]

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
    "redirect to the under nine pounds page if the total to declare is under nine pounds" in new LocalSetup {


      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","3.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("United States of America (the)", "US", isEu = false, Nil),
            ExchangeRate("1.20", "2018-10-29")))), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "1.00", "1.00", "3.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("1.00", "1.00", "1.00", "3.00"),
          withinFreeAllowance = false,
          limits = Map.empty
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/calculation")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
    }
  }

  "Calling GET .../calculation" should {
    "redirect to the over ninty seven thousand pounds page if the total to declare is over ninty seven thousand pounds" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("United States of America (the)", "US", isEu = false, Nil),
            ExchangeRate("1.20", "2018-10-29")))), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("1.00", "7.00", "90000.00", "98000.00"),
          withinFreeAllowance = false,
          limits = Map.empty
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/calculation")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £98000.00"
      content should include ("You cannot make payments for tax and duty above £97,000 using this service.")

    }
  }


  "Calling GET .../calculation" should {
    "redirect to the calculation done page with exchange rate message not includes if response only includes GBP currency" in new LocalSetup {


      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","300.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("GBP", "Great British Pounds (GBP)", Some("GBP"), Nil), Country("UK", "UK", isEu = false, Nil),
            ExchangeRate("1.20", "2018-10-29")))), Calculation("1.00","1.00","1.00","300.00"))), Calculation("1.00", "1.00", "1.00", "300.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("1.00", "1.00", "1.00", "300.00"), withinFreeAllowance = false,
          limits = Map.empty
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/calculation")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      content should not include "We use <a href=\"https://www.gov.uk/government/publications/hmrc-exchange-rates-for-2019-monthly\" target=\"_blank\">HMRC’s exchange rates"
      doc.title shouldBe  "You will need to pay £300.00 for goods purchased outside of the EU - Check tax on goods you bring into the UK - GOV.UK"
    }

    "redirect to the calculation done page with exchange rate message if response includes non GBP currency" in new LocalSetup {


      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","300.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("United States of America (the)", "US", isEu = false, Nil),
            ExchangeRate("1.20", "2018-10-29")))), Calculation("1.00","1.00","1.00","300.00"))), Calculation("1.00", "1.00", "1.00", "300.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("1.00", "1.00", "1.00", "300.00"), withinFreeAllowance = false,
          limits = Map.empty
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/calculation")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      content should include ("We use <a href=\"https://www.gov.uk/government/publications/hmrc-exchange-rates-for-2019-monthly\" target=\"_blank\">HMRC’s exchange rates")
      doc.title shouldBe  "You will need to pay £300.00 for goods purchased outside of the EU - Check tax on goods you bring into the UK - GOV.UK"
    }
  }


  "redirect to the nothing to declare done page if the total tax to pay was 0 and all of the items were within the free allowance" in new LocalSetup {


    override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
      calculatorResponse = Some(CalculatorResponse(
        Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","3.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("United States of America (the)", "US", isEu = false, Nil),
          ExchangeRate("1.20", "2018-10-29")))), Calculation("0.00","0.00","0.00","0.00"))), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "0.00"),
        withinFreeAllowance = true,
        limits = Map.empty
      ))
    ))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/calculation")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
  }

  "redirect to the under nine pound page if the total tax to pay was 0 but items were not within the free allowance (0 rated)" in new LocalSetup {

    override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
      calculatorResponse = Some(CalculatorResponse(
        Some(Alcohol(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(Band("B",List(Item("OGD/CLTHS/CHILD", "500.00",None,Some(5), Calculation("0.00","0.00","0.00","0.00"),Metadata("1 Children's clothing", "Children's clothing", "500.00",Currency("GBP", "British Pound (GBP)", Some("GBP"), Nil), Country("Barbados", "GBP", isEu = false, Nil),
          ExchangeRate("1.20", "2018-10-29")))), Calculation("0.00","0.00","0.00","0.00"))), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "0.00"),
        withinFreeAllowance = false,
        limits = Map.empty
      ))
    ))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/calculation")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
  }


  "redirect to the done page with a response containing a mixture of 0 rated and non-0 rated items" in new LocalSetup {

    override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
      calculatorResponse = Some(CalculatorResponse(
        Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","300.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("GBP", "Great British Pounds (GBP)", Some("GBP"), Nil), Country("UK", "UK", isEu = false, Nil),
          ExchangeRate("1.20", "2018-10-29")))), Calculation("1.00","1.00","1.00","300.00"))), Calculation("1.00", "1.00", "1.00", "300.00"))),
        Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(Band("B",List(Item("OGD/CLTHS/CHILD", "500.00",None,Some(5), Calculation("0.00","0.00","0.00","0.00"),Metadata("1 Children's clothing", "Children's clothing", "500.00",Currency("GBP", "British Pound (GBP)", Some("GBP"), Nil), Country("Barbados", "GBP", isEu = false, Nil),
          ExchangeRate("1.20", "2018-10-29")))), Calculation("0.00","0.00","0.00","0.00"))), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("1.00", "1.00", "1.00", "300.00"),
        withinFreeAllowance = false,
        limits = Map.empty
      ))
    ))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/calculation")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £300.00"
  }
}
