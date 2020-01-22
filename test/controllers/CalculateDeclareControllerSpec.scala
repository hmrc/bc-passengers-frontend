package controllers

import connectors.Cache
import models._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import services._
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.language.postfixOps


class CalculateDeclareControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[CalculatorService].toInstance(MockitoSugar.mock[CalculatorService]))
    .overrides(bind[UserInformationService].toInstance(MockitoSugar.mock[UserInformationService]))
    .overrides(bind[PayApiService].toInstance(MockitoSugar.mock[PayApiService]))
    .overrides(bind[DeclarationService].toInstance(MockitoSugar.mock[DeclarationService]))
    .overrides(bind[DateTimeProviderService].toInstance(MockitoSugar.mock[DateTimeProviderService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(
      injected[Cache],
      injected[PurchasedProductService],
      injected[UserInformationService],
      injected[PayApiService],
      injected[DeclarationService],
      injected[DateTimeProviderService],
      injected[TravelDetailsService]
    )
  }

  trait LocalSetup {

    def payApiResponse: PayApiServiceResponse
    def declarationServiceResponse: DeclarationServiceResponse

    def cachedJourneyData: Future[Option[JourneyData]] = Future.successful(Some(JourneyData(Some("nonEuOnly"), None, None, Some(true))))

    when(injected[Cache].fetch(any())) thenReturn cachedJourneyData

    lazy val crBelowLimit= CalculatorResponse(
      Some(Alcohol(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(Tobacco(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(OtherGoods(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc","100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Calculation("0.00", "0.00", "0.00", "1.00"),
      withinFreeAllowance = true,
      limits = Map.empty
    )

    lazy val crWithinLimit = CalculatorResponse(
      Some(Alcohol(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(Tobacco(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(OtherGoods(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc","100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Calculation("0.00", "0.00", "0.00", "18567.00"),
      withinFreeAllowance = true,
      limits = Map.empty
    )

    lazy val crAboveLimit= CalculatorResponse(
      Some(Alcohol(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "100000.00"))),
      Some(Tobacco(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "100000.00"))),
      Some(OtherGoods(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc","100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "88.00", "306038.56", "306126.56"))
      ), Calculation("0.00", "88.00", "306038.56", "306126.56"))),
      Calculation("0.00", "88.00", "306038.56", "306126.56"),
      withinFreeAllowance = true,
      limits = Map.empty
    )
    
    lazy val ui = UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-11-12"), LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa")))

    lazy val dt = DateTime.parse("2018-11-23T06:21:00Z")

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(),any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[UserInformationService].storeUserInformation(any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[Cache].fetch(any())) thenReturn cachedJourneyData
      when(injected[PayApiService].requestPaymentUrl(any(),any(), any(), any(), any())(any())) thenReturn Future.successful(payApiResponse)
      when(injected[TravelDetailsService].storeIrishBorder(any())(any())(any())) thenReturn Future.successful(Some(JourneyData()))
      when(injected[DeclarationService].submitDeclaration(any(),any(), any(), any(), any(), any())(any(), any())) thenReturn Future.successful(declarationServiceResponse)
      when(injected[DateTimeProviderService].now) thenReturn dt
      when(injected[CalculatorService].calculate(any())(any(),any())) thenReturn Future.successful(CalculatorServiceSuccessResponse(CalculatorResponse(None, None, None, Calculation("0.00", "0.00", "0.00", "0.00"), true, Map.empty)))
      when(injected[CalculatorService].storeCalculatorResponse(any(), any())(any())) thenReturn Future.successful(JourneyData())

      rt(app, req)
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with tax less than minPaymentAmount" should {

    "Display the where-goods-bought page" in new LocalSetup {
      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crBelowLimit))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/user-information with tax less than minPaymentAmount" should {

    "Display the where-goods-bought page" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crBelowLimit))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with tax greater than nine pounds and less than 90,000" should {

    "Display the where-goods-bought page" in new LocalSetup {
      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crWithinLimit))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Declare your goods"
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/user-information with tax greater than nine pounds and less than 90,000" should {

    "Display the where-goods-bought page" in new LocalSetup {
      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crWithinLimit))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get


      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Enter your details"
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/declare-your-goods with tax greater than £90,000" should {

    "Display the where-goods-bought page" in new LocalSetup {
      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crAboveLimit))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/user-information with tax greater £90,000" should {

    "Display the where-goods-bought page" in new LocalSetup {
      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crAboveLimit))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }
  }

  "Calling POST /check-tax-on-goods-you-bring-into-the-uk/enter-details" should {

    "Return BAD REQUEST and display the user information form when invalid form input is sent" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when first name is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "123456789012345678901234567890123451234",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when last name is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "123456789012345678901234567890123451234",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when passport number is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "12345678901234567890123456789012345612345",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when place of arrival is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "123456789012345678901234567890123456123456",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when the date is invalid" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crWithinLimit))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "abc",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return INTERNAL_SERVER_ERROR but still store valid user information, when the payment service request fails" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crWithinLimit), userInformation = Some(ui))))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
    }

    "Cache the submitted user information and redirect payment url when valid form input is sent and the payment service request is successful" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crWithinLimit), userInformation = Some(ui))))
      override lazy val payApiResponse = PayApiServiceSuccessResponse("http://example.com/payment-journey")
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe "http://example.com/payment-journey"

      verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
    }
  }

  "calling GET .../ireland-to-northern-ireland" should {

    "return the ireland to northern ireland page unpopulated if there is no ireland answer in keystore" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(None)
      override lazy val payApiResponse = null
      override lazy val declarationServiceResponse = null

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#irishBorder-true").hasAttr("checked") shouldBe false
      doc.select("#irishBorder-false").hasAttr("checked") shouldBe false

      verify(injected[Cache], times(1)).fetch(any())
    }

    "return the ireland to northern ireland page pre-populated yes if there is ireland answer true in keystore" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(privateCraft = Some(true), ageOver17 = Some(true), irishBorder = Some(true))))
      override lazy val payApiResponse = null
      override lazy val declarationServiceResponse = null

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#irishBorder-true").hasAttr("checked") shouldBe true
      doc.select("#irishBorder-false").hasAttr("checked") shouldBe false

      verify(injected[Cache], times(1)).fetch(any())
    }

    "return the ireland to northern ireland page pre-populated no if there is ireland answer false in keystore" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(privateCraft = Some(false), ageOver17 = Some(false), irishBorder = Some(false))))
      override lazy val payApiResponse = null
      override lazy val declarationServiceResponse = null

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#irishBorder-false").hasAttr("checked") shouldBe true
      doc.select("#irishBorder-true").hasAttr("checked") shouldBe false

      verify(injected[Cache], times(1)).fetch(any())
    }

  }

  "Calling POST .../ireland-to-northern-ireland" should {

    "redirect to /check-tax-on-goods-you-bring-into-the-uk/calculation" in new LocalSetup {

      override lazy val cachedJourneyData = Future.successful(Some(JourneyData(irishBorder = Some(false), privateCraft = Some(false))))
      override lazy val payApiResponse = null
      override lazy val declarationServiceResponse = null

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland").withFormUrlEncodedBody("irishBorder" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/calculation")

      verify(injected[TravelDetailsService], times(1)).storeIrishBorder(any())(meq(true))(any())
    }

    "return bad request when given invalid data" in new LocalSetup {


      override lazy val payApiResponse = null
      override lazy val declarationServiceResponse = null

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(injected[TravelDetailsService], times(0)).storeIrishBorder(any())(any())(any())

    }

    "return top error summary box when trying to submit a blank form" in new LocalSetup {

      override lazy val payApiResponse = null
      override lazy val declarationServiceResponse = null

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#irishBorder]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#irishBorder]").html()).get shouldBe "Select yes if you are entering Northern Ireland from Ireland"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"

    }

    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val payApiResponse = null
      override lazy val declarationServiceResponse = null

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/ireland-to-northern-ireland")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=irishBorder]").parents.find(_.tagName == "fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=irishBorder]").parents.find(_.tagName == "fieldset").get.select(".error-message").html() shouldBe "Select yes if you are entering Northern Ireland from Ireland"
    }

  }
}
