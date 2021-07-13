/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import connectors.Cache
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import repositories.BCPassengersSessionRepository
import services.{CalculatorService, PurchasedProductService}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class DashboardControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[CalculatorService].toInstance(MockitoSugar.mock[CalculatorService]))
    .build()

  

  override def beforeEach: Unit = {
    reset(injected[Cache], injected[PurchasedProductService])
    reset(injected[Cache], injected[CalculatorService])
  }

  trait LocalSetup {

    def travelDetailsJourneyData: JourneyData = JourneyData(prevDeclaration = Some(false), euCountryCheck = Some("nonEuOnly"), arrivingNICheck = Some(true), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false))
    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(),any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
      rt(app, req)
    }
  }

  val controller: DashboardController = app.injector.instanceOf[DashboardController]

  "Calling GET .../tell-us" should {
    "redirect to start if travel details are missing" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(privateCraft = None))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")

      verify(controller.cache, times(1)).fetch(any())
    }
  }

  "respond with 200, display the page if all travel details exist & display button's text for declaration:Calculate taxes and duties" in new LocalSetup {
    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))
    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us").withFormUrlEncodedBody("firstName" -> "Harry", "lastName" -> "Potter", "passportNumber" -> "801375812", "placeOfArrival" -> "Newcastle airport")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text() shouldBe "Tell us about your goods"
    doc.getElementsByClass("button margin-top-30").text() shouldBe "Calculate taxes and duties"
  }

  "respond with 200, display the page if all travel details exist & display button's text for Amendment:Calculate change in taxes and duties" in new LocalSetup {
    lazy val oldAlcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None,None,None, isEditable = Some(false))
    lazy val oldPurchasedProductInstances: List[PurchasedProductInstance] = List(oldAlcohol)
    lazy val calculation = Calculation("1.00","1.00","1.00","3.00")
    lazy val liabilityDetails = LiabilityDetails("32.0","0.0","126.4","158.40")
    lazy val declarationResponse = DeclarationResponse(calculation = calculation, oldPurchaseProductInstances = oldPurchasedProductInstances, liabilityDetails = liabilityDetails)

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false),purchasedProductInstances = List(alcohol,tobacco,other),declarationResponse=Some(declarationResponse)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))
    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us").withFormUrlEncodedBody("firstName" -> "Harry", "lastName" -> "Potter", "passportNumber" -> "801375812", "placeOfArrival" -> "Newcastle airport")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text() shouldBe "Tell us about your additional goods"
    doc.getElementsByClass("button margin-top-30").text() shouldBe "Calculate change in taxes and duties"}

    "respond with 200 and check if line showing foreign currencies accepted is shown on tell-us page " in new LocalSetup {

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData)
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(None)
    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us").withFormUrlEncodedBody("firstName" -> "Harry", "lastName" -> "Potter", "passportNumber" -> "801375812", "placeOfArrival" -> "Newcastle airport")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementById("foreign-currency").text() shouldBe "Our online calculator accepts most foreign currencies and will work out the tax due in British pounds."

  }


  "Calling GET .../tax-due" should {
    "redirect to the under nine pounds page if the total to declare is under nine pounds" in new LocalSetup {


      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","3.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "1.00", "1.00", "3.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("1.00", "1.00", "1.00", "3.00"),
          withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = false
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £3.00"
      doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due"
      doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
    }
  }

  "Calling GET .../tax-due" should {
    "redirect to the over ninety seven thousand pounds page if the total to declare is over ninety seven thousand pounds" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("1.00", "7.00", "90000.00", "98000.00"),
          withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = true
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £98,000.00"
      doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due Declaring these goods"
      doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
      content should include ("You cannot make payments for tax and duty above £97,000 using this service.")
      content should include ("When you arrive in the UK, go to the red &#x27;goods to declare&#x27; channel or the red point phone, or speak to Border Force to declare these goods.")

    }
    "redirect to the over ninety seven thousand pounds page if the amendment total to declare is over ninety seven thousand pounds" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        prevDeclaration = Some(true),
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("1.00", "7.00", "90000.00", "98000.00"),
          withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = true
        )),
        declarationResponse = Some(DeclarationResponse(Calculation("1.00", "7.00", "90000.00", "98000.00"),
          LiabilityDetails("32.0","0.0","126.4","158.40"),
          List(PurchasedProductInstance(ProductPath("other-goods/adult/adult-footwear"),"UnOGll",None,None,None,None,Some("GBP"),Some(500), None, Some(false),Some(false),None,Some(false),None,Some(false))))),
        deltaCalculation = Some(Calculation("1.00", "7.00", "90000.00", "98000.00"))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £98,000.00"
      doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due Declaring these goods"
      doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
      content should include ("You cannot make payments for tax and duty above £97,000 using this service.")
      content should include ("When you arrive in the UK, go to the red &#x27;goods to declare&#x27; channel or the red point phone, or speak to Border Force to declare these additional goods.")

    }

    "On the tax due page, for amendment, show the amount required for the current amendment and not the cumulative amount" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        prevDeclaration = Some(true),
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2021-06-21"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("100.00", "100.00", "100.00", "300.00"),
          withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = true
        )),
        declarationResponse = Some(DeclarationResponse(Calculation("100.00", "100.00", "100.00", "300.00"),
          LiabilityDetails("32.0","0.0","126.4","158.40"),
          List(PurchasedProductInstance(ProductPath("other-goods/adult/adult-footwear"),"UnOGll",None,None,None,None,Some("GBP"),Some(500), Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))), Some(false),Some(false),None,Some(false),None,Some(false))))),
        deltaCalculation = Some(Calculation("102.00", "102.00", "102.00", "306.00"))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £306.00"}

      "On the tax due page, for Zero pound amendment, show the amount required for the current amendment as 0 and not the cumulative amount" in new LocalSetup {

        override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
          euCountryCheck = Some("greatBritain"),
          arrivingNICheck = Some(true),
          calculatorResponse = Some(CalculatorResponse(
            Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
              ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
            Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
            Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
            Calculation("0.00", "0.00", "0.00", "0.00"),
            withinFreeAllowance = false,
            limits = Map.empty,
            isAnyItemOverAllowance = true
          )),
          declarationResponse = Some(DeclarationResponse(Calculation("0.00", "0.00", "0.00", "0.00"),
            LiabilityDetails("32.0","0.0","126.4","158.40"),
            List(PurchasedProductInstance(ProductPath("other-goods/adult/adult-footwear"),"UnOGll",None,None,None,None,Some("GBP"),Some(500), Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))), Some(false),Some(false),None,Some(false),None,Some(false))))),
          deltaCalculation = Some(Calculation("0.00", "0.00", "0.00", "0.00"))
        ))

        val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

        status(result) shouldBe OK

        val content: String = contentAsString(result)
        val doc: Document = Jsoup.parse(content)

        doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"}

    "redirect to the Zero to declare page if the amendment total zero pound" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("0.00", "0.00", "0.00", "0.00"),
          withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = true
        )),
        declarationResponse = Some(DeclarationResponse(Calculation("0.00", "0.00", "0.00", "0.00"),
          LiabilityDetails("32.0","0.0","126.4","158.40"),
          List(PurchasedProductInstance(ProductPath("other-goods/adult/adult-footwear"),"UnOGll",None,None,None,None,Some("GBP"),Some(500), Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))), Some(false),Some(false),None,Some(false),None,Some(false))))),
        deltaCalculation = Some(Calculation("0.00", "0.00", "0.00", "0.00"))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
      doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due"
      doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
      content should include ("You still need to declare your additional goods. You will not be charged anything when you amend your declaration.")

    }

    "redirect to the declaration page if the amendment total is zero pound" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        prevDeclaration = Some(true),
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("0.00", "0.00", "0.00", "0.00"),
          withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = true
        )),
        declarationResponse = Some(DeclarationResponse(Calculation("0.00", "0.00", "0.00", "0.00"),
          LiabilityDetails("32.0","0.0","126.4","158.40"),
          List(PurchasedProductInstance(ProductPath("other-goods/adult/adult-footwear"),"UnOGll",None,None,None,None,Some("GBP"),Some(500), Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))), Some(false),Some(false),None,Some(false),None,Some(false))))),
        deltaCalculation = Some(Calculation("0.00", "0.00", "0.00", "0.00"))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
      doc.getElementsByClass("button").text() shouldBe "Amend your declaration"
      doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due"
      doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
      content should include ("You still need to declare your additional goods. You will not be charged anything when you amend your declaration.")

    }

    "redirect to the declaration page if the amendment total between 9 pound and 97k" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        prevDeclaration = Some(true),
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("100.00", "100.00", "100.00", "300.00"),
          withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = true
        )),
        declarationResponse = Some(DeclarationResponse(Calculation("100.00", "100.00", "100.00", "300.00"),
          LiabilityDetails("32.0","0.0","126.4","158.40"),
          List(PurchasedProductInstance(ProductPath("other-goods/adult/adult-footwear"),"UnOGll",None,None,None,None,Some("GBP"),Some(500), Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))), Some(false),Some(false),None,Some(false),None,Some(false))))),
        deltaCalculation = Some(Calculation("100.00", "100.00", "100.00", "300.00"))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £300.00"
      doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due"
      doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
      content should include ("You can amend your declaration from 5 days before your scheduled time of arrival in the UK.")
      doc.getElementsByClass("button").text() shouldBe "Amend your declaration and pay online"
    }

    "redirect to the declaration page if the amendment total is within free allowance" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        prevDeclaration = Some(true),
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("0.00", "0.00", "0.00", "0.00"),
          withinFreeAllowance = true,
          limits = Map.empty,
          isAnyItemOverAllowance = true
        )),
        declarationResponse = Some(DeclarationResponse(Calculation("0.00", "0.00", "0.00", "0.00"),
          LiabilityDetails("32.0","0.0","126.4","158.40"),
          List(PurchasedProductInstance(ProductPath("other-goods/adult/adult-footwear"),"UnOGll",None,None,None,None,Some("GBP"),Some(500), Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))), Some(false),Some(false),None,Some(false),None,Some(false))))),
        deltaCalculation = Some(Calculation("0.00", "0.00", "0.00", "0.00"))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
      doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due What you need to do next"
      doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
      content should include ("All the items you added are included within your personal allowances")
    }

    "redirect to the declaration page if the amendment total is zero and declaration is over allowance" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","7.00","90000.00","90000.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","3.00"))), Calculation("1.00", "7.00", "90000.00", "98000.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("0.00", "0.00", "0.00", "0.00"),
          withinFreeAllowance = true,
          limits = Map.empty,
          isAnyItemOverAllowance = true
        )),
        declarationResponse = Some(DeclarationResponse(Calculation("0.00", "0.00", "0.00", "0.00"),
          LiabilityDetails("32.0","0.0","126.4","158.40"),
          List(PurchasedProductInstance(ProductPath("other-goods/adult/adult-footwear"),"UnOGll",None,None,None,None,Some("GBP"),Some(500), Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))), Some(false),Some(false),None,Some(false),None,Some(false))))),
        deltaCalculation = Some(Calculation("0.00", "0.00", "0.00", "0.00")),
        euCountryCheck = Some("greatBritain")
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
      doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due"
      doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
    }

  }


  "Calling GET .../tax-due" should {
    "redirect to the calculation done page with exchange rate message not includes if response only includes GBP currency" in new LocalSetup {


      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","300.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("GBP", "Great British Pounds (GBP)", Some("GBP"), Nil), Country("UK", "UK", "UK", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","300.00"))), Calculation("1.00", "1.00", "1.00", "300.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("1.00", "1.00", "1.00", "300.00"), withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = false
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      content should not include "We use <a href=\"https://www.gov.uk/government/publications/hmrc-exchange-rates-for-2019-monthly\" target=\"_blank\">HMRC’s exchange rates"
      doc.title shouldBe  "Tax due on these goods - Check tax on goods you bring into the UK - GOV.UK"
    }

    "redirect to the calculation done page with exchange rate message if response includes non GBP currency" in new LocalSetup {


      override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
        calculatorResponse = Some(CalculatorResponse(
          Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","300.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","300.00"))), Calculation("1.00", "1.00", "1.00", "300.00"))),
          Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
          Calculation("1.00", "1.00", "1.00", "300.00"), withinFreeAllowance = false,
          limits = Map.empty,
          isAnyItemOverAllowance = false
        ))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

      status(result) shouldBe OK

      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      content should include ("<a href = \"https://www.gov.uk/government/collections/exchange-rates-for-customs-and-vat\" target=\"_blank\">HMRC’s exchange rates")
      doc.title shouldBe  "Tax due on these goods - Check tax on goods you bring into the UK - GOV.UK"
    }
  }


  "redirect to the nothing to declare done page if the total tax to pay was 0 and all of the items were within the free allowance" in new LocalSetup {


    override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
      calculatorResponse = Some(CalculatorResponse(
        Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","3.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
          ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("0.00","0.00","0.00","0.00"))), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "0.00"),
        withinFreeAllowance = true,
        limits = Map.empty,
        isAnyItemOverAllowance = false
      ))
    ))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
    doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due What you need to do next"
    doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
  }

  "redirect to declare your goods page if the total tax to pay is 0 and items were over free allowance in GB-NI journey and has zero tax liability" in new LocalSetup {


    override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
      euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), calculatorResponse = Some(CalculatorResponse(
        Some(Alcohol(List(Band("A",List(Item("Adult Clothing", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","3.00"),Metadata("Adult clothing", "Adult clothing", "1.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
          ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("0.00","0.00","0.00","0.00"))), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "0.00"),
        withinFreeAllowance = true,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      ))
    ))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
    doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due"
    doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
  }

  "redirect to the under nine pound page if the total tax to pay was 0 but items were not within the free allowance (0 rated)" in new LocalSetup {

    override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
      calculatorResponse = Some(CalculatorResponse(
        Some(Alcohol(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(Band("B",List(Item("OGD/CLTHS/CHILD", "500.00",None,Some(5), Calculation("0.00","0.00","0.00","0.00"),Metadata("1 Children's clothing", "Children's clothing", "500.00",Currency("GBP", "British Pound (GBP)", Some("GBP"), Nil), Country("GBP", "Barbados", "GBP", isEu = false, isCountry = true, Nil),
          ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("0.00","0.00","0.00","0.00"))), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "0.00"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      ))
    ))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £0.00"
    doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due What you need to do next"
    doc.getElementsByClass("table-heading").text() shouldBe "Item Price Purchased in Tax due Item Customs Excise VAT"
  }


  "redirect to the done page with a response containing a mixture of 0 rated and non-0 rated items" in new LocalSetup {

    override lazy val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(
      calculatorResponse = Some(CalculatorResponse(
        Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "1.00",None,Some(5), Calculation("1.00","1.00","1.00","300.00"),Metadata("5 litres cider", "Cider", "1.00",Currency("GBP", "Great British Pounds (GBP)", Some("GBP"), Nil), Country("UK", "UK", "UK", isEu = false, isCountry = true, Nil),
          ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("1.00","1.00","1.00","300.00"))), Calculation("1.00", "1.00", "1.00", "300.00"))),
        Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(Band("B",List(Item("OGD/CLTHS/CHILD", "500.00",None,Some(5), Calculation("0.00","0.00","0.00","0.00"),Metadata("1 Children's clothing", "Children's clothing", "500.00",Currency("GBP", "British Pound (GBP)", Some("GBP"), Nil), Country("GBP", "Barbados", "GBP", isEu = false, isCountry = true, Nil),
          ExchangeRate("1.20", "2018-10-29"),None),None,None,None,None)), Calculation("0.00","0.00","0.00","0.00"))), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("1.00", "1.00", "1.00", "300.00"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = false
      ))
    ))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tax-due")).get

    status(result) shouldBe OK

    val content: String = contentAsString(result)
    val doc: Document = Jsoup.parse(content)

    doc.getElementsByTag("h1").text shouldBe "Tax due on these goods £300.00"
    doc.getElementsByClass("heading-medium").text() shouldBe "Breakdown of tax and duty due"
  }

  "display the vat,excise and tax exempt flags as No against items for GBNI Journey" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false,isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None, Some(false),None,Some(false),Some(false))
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), Some(20), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None, Some(false),None,Some(false),Some(false))
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("CHF"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(false),None,Some(false),Some(false))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("vat-paid").text() shouldBe "VAT paid No"
    tobaccoItem.getElementsByClass("vat-paid").text() shouldBe "VAT paid No"
    otherItem.getElementsByClass("vat-paid").text() shouldBe "VAT paid No"

    alcoholItem.getElementsByClass("excise-paid").text() shouldBe "Excise paid No"
    tobaccoItem.getElementsByClass("excise-paid").text() shouldBe "Excise paid No"
    otherItem.getElementsByClass("tax-exempt").text() shouldBe "Tax exempt No"

  }
  "display the produced_in and made_in flags with desired values against items for EU Journey" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"
    tobaccoItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"
    otherItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"

    alcoholItem.getElementsByClass("producedin-country").text() shouldBe "Produced in Egypt"
    tobaccoItem.getElementsByClass("producedin-country").text() shouldBe "Produced in Egypt"
    otherItem.getElementsByClass("madein-country").text() shouldBe "Made in Egypt"
  }


  "Display only purchased in and don't display the produced_in and made_in flags when they don't have any desired values against items for EU Journey" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), None, Some("AUD"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"
    tobaccoItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"
    otherItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"

    alcoholItem.getElementsByClass("producedin-country").text() shouldBe ""
    tobaccoItem.getElementsByClass("producedin-country").text() shouldBe ""
    otherItem.getElementsByClass("madein-country").text() shouldBe ""
  }

  "display the purchased in, produced_in flags with desired values against items for EU Journey, when the made_in flag has no data against it" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), None, Some("AUD"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"
    tobaccoItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"
    otherItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"

    alcoholItem.getElementsByClass("producedin-country").text() shouldBe "Produced in Egypt"
    tobaccoItem.getElementsByClass("producedin-country").text() shouldBe "Produced in Egypt"
    otherItem.getElementsByClass("madein-country").text() shouldBe ""
  }

  "Display the purchased in, made_in flags with desired values against items for EU Journey, when the produced_in flag has no data against it" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"
    tobaccoItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"
    otherItem.getElementsByClass("product-country").text() shouldBe "Purchased in France"

    alcoholItem.getElementsByClass("producedin-country").text() shouldBe ""
    tobaccoItem.getElementsByClass("producedin-country").text() shouldBe ""
    otherItem.getElementsByClass("madein-country").text() shouldBe "Made in Egypt"
  }

  "display the evidence flags with true values against items for EU Journey" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,None,None,Some(true))
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,None,None,Some(true))
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some("AUD"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,None,Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("has_got_evidence").text() shouldBe "Evidence of origin Yes"
    tobaccoItem.getElementsByClass("has_got_evidence").text() shouldBe "Evidence of origin Yes"
    otherItem.getElementsByClass("has_got_evidence").text() shouldBe "Evidence of origin Yes"
  }


  "Don't display the evidence flags when it has not been defined for EU Journey" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,None,None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,None,None,None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some("AUD"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,None,None)

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("has_got_evidence").text() shouldBe ""
    tobaccoItem.getElementsByClass("has_got_evidence").text() shouldBe ""
    otherItem.getElementsByClass("has_got_evidence").text() shouldBe ""
  }



  "display the vat,excise and tax exempt flags as Yes against items for GBNI Journey" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), Some(20), Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None, Some(true),None,Some(true),None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false,isCountry = true, Nil)), None, Some("CHF"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("vat-paid").text() shouldBe "VAT paid Yes"
    tobaccoItem.getElementsByClass("vat-paid").text() shouldBe "VAT paid Yes"
    otherItem.getElementsByClass("vat-paid").text() shouldBe "VAT paid Yes"

    alcoholItem.getElementsByClass("excise-paid").text() shouldBe "Excise paid Yes"
    tobaccoItem.getElementsByClass("excise-paid").text() shouldBe "Excise paid Yes"
    otherItem.getElementsByClass("tax-exempt").text() shouldBe "Tax exempt Yes"
  }

  "display the vat, excise and tax exempt flags as Empty against items for GBNI Journey" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false,isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None,None,None,None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), Some(20), Some(Country("EG", "title.egypt", "EG", isEu = false,isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)),None,None,None,None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("CHF"), Some(5432),None,None,None)

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("vat-paid").text() shouldBe "VAT paid"
    tobaccoItem.getElementsByClass("vat-paid").text() shouldBe "VAT paid"
    otherItem.getElementsByClass("vat-paid").text() shouldBe "VAT paid"

    alcoholItem.getElementsByClass("excise-paid").text() shouldBe "Excise paid"
    tobaccoItem.getElementsByClass("excise-paid").text() shouldBe "Excise paid"
    otherItem.getElementsByClass("tax-exempt").text() shouldBe "Tax exempt"
  }

  "not display the vat, excise and tax exempt flags as Empty against items for non GBNI Journey" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None,None,None,None)
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), Some(20), Some(Country("EG", "title.egypt", "EG", isEu = false,isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)),None,None,None,None)
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None,Some("CHF"), Some(5432),None,None,None)

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItem.getElementsByClass("vat-paid").isEmpty shouldBe true
    tobaccoItem.getElementsByClass("vat-paid").isEmpty shouldBe true
    otherItem.getElementsByClass("vat-paid").isEmpty shouldBe true

    alcoholItem.getElementsByClass("excise-paid").isEmpty shouldBe true
    tobaccoItem.getElementsByClass("excise-paid").isEmpty shouldBe true
    otherItem.getElementsByClass("tax-exempt").isEmpty shouldBe true
  }

  "not display the tax exempt flags against other items for GBNI Journey for UK Residents" in new LocalSetup {

    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry =  true, Nil)), None, Some("CHF"), Some(5432), Some(OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques"))), Some(true),None,None,Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(true), purchasedProductInstances = List(other)
    ))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)
      .getElementsByClass("govuk-check-your-answers").get(0)


    otherItem.getElementsByClass("vat-paid").isEmpty shouldBe false
    otherItem.getElementsByClass("tax-exempt").isEmpty shouldBe true
  }

  "display edit links for new items for GBNI Journey for UK Residents" in new LocalSetup {

    val alcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None,None,None, isEditable = Some(true))
    val tobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), Some(20), Some(Country("EG", "title.egypt", "EG", isEu = false,isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)),None,None,None, isEditable = Some(true))
    val other: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None,Some("CHF"), Some(5432),None,None,None, isEditable = Some(true))

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(purchasedProductInstances = List(alcohol,tobacco,other)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = alcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = tobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = other, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)

    alcoholItem.getElementsByClass("edit-link").isEmpty shouldBe false
    tobaccoItem.getElementsByClass("edit-link").isEmpty shouldBe false
    otherItem.getElementsByClass("edit-link").isEmpty shouldBe false
  }

  "display old items(amendment journey) for GBNI Journey for UK Residents" in new LocalSetup {

    val oldAlcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)), None,None,None, isEditable = Some(false))
    val oldTobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), Some(20), Some(Country("EG", "title.egypt", "EG", isEu = false,isCountry = true, Nil)), None, Some("AUD"), Some(BigDecimal(10.234)),None,None,None, isEditable = Some(false))
    val oldOther: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None,Some("CHF"), Some(5432),None,None,None, isEditable = Some(false))

    val oldPurchasedProductInstances: List[PurchasedProductInstance] = List(oldAlcohol, oldTobacco, oldOther)

    val calculation = Calculation("1.00","1.00","1.00","3.00")

    val liabilityDetails = LiabilityDetails("32.0","0.0","126.4","158.40")

    val declarationResponse = DeclarationResponse(calculation = calculation, oldPurchaseProductInstances = oldPurchasedProductInstances, liabilityDetails = liabilityDetails)

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), declarationResponse =  Some(declarationResponse)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = oldAlcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = oldTobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = oldOther, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)

    alcoholItem.getElementsByClass("previous-alcohol").text() shouldBe "Previously declared alcohol"
    tobaccoItem.getElementsByClass("previous-tobacco").text() shouldBe "Previously declared tobacco"
    otherItem.getElementsByClass("previous-othergoods").text() shouldBe "Previously declared other goods"

    alcoholItem.getElementsByClass("edit-link").isEmpty shouldBe true
    tobaccoItem.getElementsByClass("edit-link").isEmpty shouldBe true
    otherItem.getElementsByClass("edit-link").isEmpty shouldBe true

    val alcoholItemCheck: Element = alcoholItem.getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItemCheck: Element = tobaccoItem.getElementsByClass("govuk-check-your-answers").get(0)
    val otherItemCheck: Element = otherItem.getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItemCheck.getElementsByClass("vat-paid").isEmpty shouldBe false
    tobaccoItemCheck.getElementsByClass("vat-paid").isEmpty shouldBe false
    otherItemCheck.getElementsByClass("vat-paid").isEmpty shouldBe false

    alcoholItemCheck.getElementsByClass("excise-paid").isEmpty shouldBe false
    tobaccoItemCheck.getElementsByClass("excise-paid").isEmpty shouldBe false
    otherItemCheck.getElementsByClass("tax-exempt").isEmpty shouldBe false
  }

  "display old items(amendment journey) for EU Journey" in new LocalSetup {

    val oldAlcohol: PurchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)), None,None,None, isEditable = Some(false))
    val oldTobacco: PurchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), Some(20), Some(Country("EG", "title.egypt", "EG", isEu = false,isCountry = true, Nil)), Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), Some("AUD"), Some(BigDecimal(10.234)),None,None,None, isEditable = Some(false))
    val oldOther: PurchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/antiques"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),Some("CHF"), Some(5432),None,None,None, isEditable = Some(false))

    val oldPurchasedProductInstances: List[PurchasedProductInstance] = List(oldAlcohol, oldTobacco, oldOther)

    val calculation = Calculation("1.00","1.00","1.00","3.00")

    val liabilityDetails = LiabilityDetails("32.0","0.0","126.4","158.40")

    val declarationResponse = DeclarationResponse(calculation = calculation, oldPurchaseProductInstances = oldPurchasedProductInstances, liabilityDetails = liabilityDetails)

    override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), declarationResponse =  Some(declarationResponse)))

    val csr: CalculatorServiceRequest  = CalculatorServiceRequest(isPrivateCraft = false, isAgeOver17 = false, isArrivingNI = false,
      List(PurchasedItem(purchasedProductInstance = oldAlcohol, productTreeLeaf = ProductTreeLeaf("","","","alcohol",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = oldTobacco, productTreeLeaf = ProductTreeLeaf("","","","tobacco",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10)),
        PurchasedItem(purchasedProductInstance = oldOther, productTreeLeaf = ProductTreeLeaf("","","","other-goods",List.empty), exchangeRate = ExchangeRate("",""), currency = Currency("","",None, List.empty), gbpCost = BigDecimal(10))))
    when(injected[CalculatorService].journeyDataToCalculatorRequest(any(), any())(any())) thenReturn Future.successful(Some(csr))

    val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

    status(result) shouldBe OK
    val content: String = contentAsString(result)

    val doc: Document = Jsoup.parse(content)
    val alcoholItem: Element = doc.getElementsByClass("alcohol").get(0)
    val tobaccoItem: Element = doc.getElementsByClass("tobacco").get(0)
    val otherItem: Element = doc.getElementsByClass("other-goods").get(0)

    alcoholItem.getElementsByClass("previous-alcohol").text() shouldBe "Previously declared alcohol"
    tobaccoItem.getElementsByClass("previous-tobacco").text() shouldBe "Previously declared tobacco"
    otherItem.getElementsByClass("previous-othergoods").text() shouldBe "Previously declared other goods"

    alcoholItem.getElementsByClass("edit-link").isEmpty shouldBe true
    tobaccoItem.getElementsByClass("edit-link").isEmpty shouldBe true
    otherItem.getElementsByClass("edit-link").isEmpty shouldBe true

    val alcoholItemCheck: Element = alcoholItem.getElementsByClass("govuk-check-your-answers").get(0)
    val tobaccoItemCheck: Element = tobaccoItem.getElementsByClass("govuk-check-your-answers").get(0)
    val otherItemCheck: Element = otherItem.getElementsByClass("govuk-check-your-answers").get(0)

    alcoholItemCheck.getElementsByClass("producedin-country").isEmpty shouldBe false
    tobaccoItemCheck.getElementsByClass("producedin-country").isEmpty shouldBe false
    otherItemCheck.getElementsByClass("madein-country").isEmpty shouldBe false
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/tell-us when in pending payment journey" should {

    "Display the previous-declaration page" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData] = Some(travelDetailsJourneyData.copy(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(true), amendState = Some("pending-payment")))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/tell-us")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

}
