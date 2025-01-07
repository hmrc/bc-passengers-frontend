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
import models.JourneyData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.{CalculatorService, TravelDetailsService}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class TravelDetailsControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[TravelDetailsService].toInstance(mock(classOf[TravelDetailsService])))
    .overrides(bind[CalculatorService].toInstance(mock(classOf[CalculatorService])))
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .overrides(bind[AppConfig].toInstance(mock(classOf[AppConfig])))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach(): Unit = {
    reset(injected[TravelDetailsService])
    reset(injected[Cache])
    when(
      injected[AppConfig].declareGoodsUrl
    ) `thenReturn` "https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods"
  }

  trait LocalSetup {

    val controller: TravelDetailsController = app.injector.instanceOf[TravelDetailsController]

    def cachedJourneyData: Future[Option[JourneyData]] = Future.successful(Some(JourneyData()))

    when(injected[Cache].fetch(any())) `thenReturn` cachedJourneyData
    when(injected[TravelDetailsService].storeAgeOver17(any())(any())(any())) `thenReturn` Future.successful(
      Some(JourneyData())
    )
    when(injected[TravelDetailsService].storeBringingDutyFree(any())(any())(any())) `thenReturn` Future.successful(
      Some(JourneyData())
    )
    when(injected[TravelDetailsService].storeBringingOverAllowance(any())(any())(any())) `thenReturn` Future.successful(
      Some(JourneyData())
    )
    when(injected[TravelDetailsService].storeEuCountryCheck(any())(any())(any())) `thenReturn` Future.successful(
      Some(JourneyData())
    )
    when(injected[TravelDetailsService].storeIrishBorder(any())(any())(any())) `thenReturn` Future.successful(
      Some(JourneyData())
    )
    when(injected[TravelDetailsService].storePrivateCraft(any())(any())(any())) `thenReturn` Future.successful(
      Some(JourneyData())
    )
    when(injected[TravelDetailsService].storeVatResCheck(any())(any())(any())) `thenReturn` Future.successful(
      Some(JourneyData())
    )
  }

  "calling GET .../where-goods-bought" should {

    "return the select eu country check page with the previous choice populated if there is one in the keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"))))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Where are you bringing in goods from?"

      doc
        .getElementById("euCountryCheck-hint")
        .text() shouldBe "If you are bringing in goods from both EU and non-EU countries, only select non-EU countries below."

      doc.getElementsByAttributeValueMatching("name", "euCountryCheck").asScala.length shouldBe 3

      doc.select("#euCountryCheck-nonEu").hasAttr("checked") shouldBe true

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the select eu country check page with the previous choice not populated if there is not one in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Option[JourneyData]] = Future.successful(None)

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get
      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                             shouldBe "Where are you bringing in goods from?"
      doc.select("#euCountryCheck-euonly").hasAttr("checked")       shouldBe false
      doc.select("#euCountryCheck-noneuonly").hasAttr("checked")    shouldBe false
      doc.select("#euCountryCheck-greatBritain").hasAttr("checked") shouldBe false

      doc
        .getElementById("euCountryCheck-eu-item-hint")
        .text() shouldBe "Austria, Belgium, Bulgaria, Croatia, Cyprus, Czech Republic, Denmark, Estonia, Finland, France, Germany, Greece, Hungary, Ireland, Italy, Latvia, Lithuania, Luxembourg, Malta, Netherlands, Poland, Portugal, Romania, Slovakia, Slovenia, Spain and Sweden."
      doc
        .getElementById("euCountryCheck-nonEu-item-hint")
        .text() shouldBe "This includes the Channel Islands, British Overseas Territories (including Gibraltar), the north of Cyprus and the Canary Islands."
      doc
        .getElementById("euCountryCheck-gb-item-hint")
        .text() shouldBe "This is only if you are bringing goods to Northern Ireland from Great Britain or the Isle of Man."

      verify(controller.cache, times(1)).fetch(any())
    }

    "show the back link to previous declaration page when the amendments feature is on" in new LocalSetup {

      when(injected[AppConfig].isAmendmentsEnabled) `thenReturn` true

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)
      doc.getElementById("back").attr("href") shouldBe "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration"

      verify(controller.cache, times(1)).fetch(any())
    }

    "show the back link to gov uk declare goods start page page when the amendments feature is off" in new LocalSetup {

      when(injected[AppConfig].isAmendmentsEnabled) `thenReturn` false

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)
      doc.getElementById("back").attr("href") shouldBe "https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods"

      verify(controller.cache, times(1)).fetch(any())
    }
  }

  "calling POST .../where-goods-bought" should {

    "redirect to .../goods-bought-into-northern-ireland-inside-eu when user selects country in EU" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"))))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
          .withFormUrlEncodedBody("euCountryCheck" -> "euOnly")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(any())(meq("euOnly"))(any())
    }

    "redirect to .../goods-bought-outside-eu when user says they have only arrived from countries outside EU" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"))))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
          .withFormUrlEncodedBody("euCountryCheck" -> "nonEuOnly")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(any())(meq("nonEuOnly"))(any())
    }

    "redirect to .../goods-bought-inside-and-outside-eu when user says they have arrived from both EU and ROW countries" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(Some(JourneyData(euCountryCheck = Some("both"))))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
          .withFormUrlEncodedBody("euCountryCheck" -> "both")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(any())(meq("both"))(any())
    }

    "return bad request when given invalid data" in new LocalSetup {
      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
          .withFormUrlEncodedBody("value" -> "badValue")
      ).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeEuCountryCheck(any())(any())(any())

    }

    "return error summary box on the page head when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None)))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      Option(
        doc.getElementsByClass("govuk-error-summary__body").select("a[href=#euCountryCheck]")
      ).isEmpty                                               shouldBe false
      doc.select("a[href=#euCountryCheck-eu]").text()         shouldBe "Select where you are bringing in goods from"
      doc.select("h2").hasClass("govuk-error-summary__title") shouldBe true
      doc.select(".govuk-error-summary__title").text()        shouldBe "There is a problem"
    }

    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None)))

      val response: Future[Result] =
        route(app, enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      Option(
        doc.getElementsByClass("govuk-error-summary__body").select("a[href=#euCountryCheck]")
      ).isEmpty                                                                                     shouldBe false
      doc.getElementById("euCountryCheck-error").getElementsByClass("govuk-visually-hidden").text() shouldBe "Error:"
    }
  }

  "calling GET /check-tax-on-goods-you-bring-into-the-uk/duty-free-eu" should {
    def test(bringingOverAllowance: Option[Boolean]): Unit =
      s"load the duty free allowance question EU page when bringingOverAllowance is $bringingOverAllowance" in new LocalSetup {
        override lazy val cachedJourneyData: Future[Some[JourneyData]] =
          Future.successful(
            Some(
              JourneyData(
                prevDeclaration = Some(false),
                euCountryCheck = Some("euOnly"),
                arrivingNICheck = Some(false),
                isVatResClaimed = Some(false),
                isBringingDutyFree = Some(true),
                bringingOverAllowance = bringingOverAllowance
              )
            )
          )

        val response: Future[Result] = route(
          app,
          enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-eu")
        ).get
        status(response) shouldBe OK

        val content: String = contentAsString(response)
        val doc: Document   = Jsoup.parse(content)

        doc.select("h1").text() shouldBe "Are you bringing in goods over your allowances?"
      }

    Seq(Some(true), None).foreach(test)
  }

  "calling GET /check-tax-on-goods-you-bring-into-the-uk/duty-free-mix" should {
    def test(bringingOverAllowance: Option[Boolean]): Unit =
      s"load the duty free allowance question mix page when bringingOverAllowance is $bringingOverAllowance" in new LocalSetup {
        override lazy val cachedJourneyData: Future[Some[JourneyData]] =
          Future.successful(
            Some(
              JourneyData(
                prevDeclaration = Some(false),
                euCountryCheck = Some("both"),
                arrivingNICheck = Some(false),
                isVatResClaimed = Some(false),
                isBringingDutyFree = Some(true),
                bringingOverAllowance = bringingOverAllowance
              )
            )
          )

        val response: Future[Result] = route(
          app,
          enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-mix")
        ).get
        status(response) shouldBe OK

        val content: String = contentAsString(response)
        val doc: Document   = Jsoup.parse(content)

        doc.select("h1").text() shouldBe "Are you bringing in goods over your allowances?"
      }

    Seq(Some(true), None).foreach(test)
  }

  "calling GET /check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland" should {
    def test(bringingOverAllowance: Option[Boolean]): Unit =
      s"load the goods bought outside EU page when bringingOverAllowance is $bringingOverAllowance" in new LocalSetup {
        override lazy val cachedJourneyData: Future[Some[JourneyData]] =
          Future.successful(
            Some(
              JourneyData(
                prevDeclaration = Some(false),
                euCountryCheck = Some("nonEuOnly"),
                arrivingNICheck = Some(true),
                bringingOverAllowance = bringingOverAllowance
              )
            )
          )

        val response: Future[Result] = route(
          app,
          enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland")
        ).get
        status(response) shouldBe OK

        val content: String = contentAsString(response)
        val doc: Document   = Jsoup.parse(content)

        doc.select("h1").text() shouldBe "Goods brought into Northern Ireland"
      }

    Seq(Some(true), None).foreach(test)
  }

  "calling POST /check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland" should {
    "redirect to the private travel page when bringing in goods over the indicated allowances" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("nonEuOnly"),
              arrivingNICheck = Some(true)
            )
          )
        )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland")
          .withFormUrlEncodedBody("bringingOverAllowance" -> "true")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/private-travel")
    }

    "redirect to the no need to use service page when bringing in goods under the indicated allowances" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("nonEuOnly"),
              arrivingNICheck = Some(true)
            )
          )
        )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland")
          .withFormUrlEncodedBody("bringingOverAllowance" -> "false")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/no-need-to-use-service")
    }

    "return bad request when given invalid data" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("nonEuOnly"),
              arrivingNICheck = Some(true)
            )
          )
        )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland")
          .withFormUrlEncodedBody("value" -> "badValue")
      ).get

      status(response) shouldBe BAD_REQUEST
    }
  }

  "calling GET /check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-great-britain-iom" should {
    def test(bringingOverAllowance: Option[Boolean]): Unit =
      s"load the goods bought inside and outside EU page when bringingOverAllowance is $bringingOverAllowance" in new LocalSetup {
        override lazy val cachedJourneyData: Future[Some[JourneyData]] =
          Future.successful(
            Some(
              JourneyData(
                prevDeclaration = Some(false),
                euCountryCheck = Some("nonEuOnly"),
                arrivingNICheck = Some(false),
                bringingOverAllowance = bringingOverAllowance
              )
            )
          )

        val response: Future[Result] = route(
          app,
          enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-great-britain-iom")
        ).get
        status(response) shouldBe OK

        val content: String = contentAsString(response)
        val doc: Document   = Jsoup.parse(content)

        doc.select("h1").text() shouldBe "Goods brought into Great Britain or the Isle of Man"
      }

    Seq(Some(true), None).foreach(test)
  }

  "calling POST /check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-great-britain-iom" should {
    "redirect to the private travel page when bringing in goods over the indicated allowances" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("nonEuOnly"),
              arrivingNICheck = Some(false)
            )
          )
        )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-great-britain-iom")
          .withFormUrlEncodedBody("bringingOverAllowance" -> "true")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/private-travel")
    }

    "redirect to the no need to use service page when bringing in goods under the indicated allowances" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("nonEuOnly"),
              arrivingNICheck = Some(false)
            )
          )
        )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-great-britain-iom")
          .withFormUrlEncodedBody("bringingOverAllowance" -> "false")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/no-need-to-use-service")
    }

    "return bad request when given invalid data" in new LocalSetup {
      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("nonEuOnly"),
              arrivingNICheck = Some(false)
            )
          )
        )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-great-britain-iom")
          .withFormUrlEncodedBody("value" -> "badValue")
      ).get

      status(response) shouldBe BAD_REQUEST
    }
  }

  "calling GET .../goods-bought-into-northern-ireland-inside-eu" should {

    "return the goods bought inside the eu interrupt page" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] =
        Future.successful(Some(JourneyData(Some(false), Some("euOnly"), Some(true), None, None)))

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/goods-bought-into-northern-ireland-inside-eu"
        )
      ).get
      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.text() should include("You do not need to tell us about your goods")
    }
  }

  "calling GET .../no-need-to-use-service" should {

    "return no need to use this service page" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(JourneyData(Some(false), Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(false)))
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/no-need-to-use-service")).get
      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.text() should include("You do not need to use this service")
    }
  }

  "Calling GET .../private-travel" should {

    "return the private craft page unpopulated if there is no age answer in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(JourneyData(None, Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(true)))
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe OK
      val doc: Document = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-value-yes").hasAttr("checked") shouldBe false
      doc.select("#privateCraft-value-no").hasAttr("checked")  shouldBe false

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the private craft page pre populated no if there is answer false in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(
            None,
            Some("nonEuOnly"),
            Some(true),
            None,
            None,
            None,
            None,
            None,
            None,
            Some(true),
            privateCraft = Some(false)
          )
        )
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe OK
      val doc: Document = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-value-yes").hasAttr("checked") shouldBe false
      doc.select("#privateCraft-value-no").hasAttr("checked")  shouldBe true

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the private craft page pre populated yes if there is age answer true in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(
            None,
            Some("nonEuOnly"),
            Some(true),
            None,
            None,
            None,
            None,
            None,
            None,
            Some(true),
            privateCraft = Some(true)
          )
        )
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe OK
      val doc: Document = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-value-yes").hasAttr("checked") shouldBe true
      doc.select("#privateCraft-value-no").hasAttr("checked")  shouldBe false

      verify(controller.cache, times(1)).fetch(any())
    }
  }

  "Calling POST .../private-travel" should {

    "redirect to /check-tax-on-goods-you-bring-into-the-uk/confirm-age" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(
            None,
            Some("nonEuOnly"),
            Some(true),
            None,
            None,
            None,
            None,
            None,
            None,
            Some(true),
            privateCraft = Some(true)
          )
        )
      )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel").withFormUrlEncodedBody(
          "privateCraft" -> "true"
        )
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/confirm-age")

      verify(controller.travelDetailsService, times(1)).storePrivateCraft(any())(meq(true))(any())
    }

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(JourneyData(None, Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(true)))
      )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel").withFormUrlEncodedBody(
          "value" -> "bad_value"
        )
      ).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storePrivateCraft(any())(any())(any())

    }

    "return error summary box on the page head when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(JourneyData(None, Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(true)))
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      Option(
        doc
          .getElementsByClass("govuk-error-summary__body")
          .select("a[href=#privateCraft-error]")
      ).isEmpty                                        shouldBe false
      doc
        .select("a[href=#privateCraft-value-yes]")
        .text()                                        shouldBe "Select yes if you are arriving in the UK by private transport"
      doc
        .select("h2")
        .hasClass("govuk-error-summary__title")        shouldBe true
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
    }

    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(JourneyData(None, Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(true)))
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc
        .getElementById("privateCraft-error")
        .html() shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Select yes if you are arriving in the UK by private transport"

    }

  }

  "calling GET .../confirm-age" should {

    "return the confirm age page unpopulated if there is no age answer in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(None, Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(true), Some(true))
        )
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked")  shouldBe false
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe false

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the confirm age page pre-populated yes if there is age answer true in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(
            None,
            Some("nonEuOnly"),
            Some(true),
            None,
            None,
            None,
            None,
            None,
            None,
            Some(true),
            Some(true),
            ageOver17 = Some(true)
          )
        )
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.select("#ageOver17-value-yes").hasAttr("checked") shouldBe true
      doc.select("#ageOver17-value-no").hasAttr("checked")  shouldBe false

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the confirm age page pre-populated no if there is age answer false in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(
            None,
            Some("nonEuOnly"),
            Some(true),
            None,
            None,
            None,
            None,
            None,
            None,
            Some(true),
            Some(true),
            ageOver17 = Some(false)
          )
        )
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.select("#ageOver17-value-yes").hasAttr("checked") shouldBe false
      doc.select("#ageOver17-value-no").hasAttr("checked")  shouldBe true

      verify(controller.cache, times(1)).fetch(any())
    }

  }

  "Calling POST .../confirm-age" should {

    "redirect to /check-tax-on-goods-you-bring-into-the-uk/tell-us when subsequent journey data is present" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(None, Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(true), Some(true))
        )
      )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age").withFormUrlEncodedBody(
          "ageOver17" -> "true"
        )
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tell-us")

      verify(controller.travelDetailsService, times(1)).storeAgeOver17(any())(meq(true))(any())
    }

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(None, Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(true), Some(true))
        )
      )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age").withFormUrlEncodedBody(
          "value" -> "badValue"
        )
      ).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeAgeOver17(any())(any())(any())

    }

    "return top error summary box when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(None, Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(true), Some(true))
        )
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      Option(
        doc.getElementsByClass("govuk-error-summary").select("a[href=#ageOver17-value-yes]")
      ).isEmpty                                                                     shouldBe false
      Option(
        doc.getElementsByClass("govuk-error-summary").select("a[href=#ageOver17-value-yes]").html()
      ).get                                                                         shouldBe "Select yes if you are aged 17 or over"
      Option(
        doc.getElementsByClass("govuk-error-summary").select("h2").hasClass("govuk-error-summary__title")
      ).get                                                                         shouldBe true
      Option(doc.getElementsByClass("govuk-error-summary").select("h2").html()).get shouldBe "There is a problem"

    }

    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(
          JourneyData(None, Some("nonEuOnly"), Some(true), None, None, None, None, None, None, Some(true), Some(true))
        )
      )

      val response: Future[Result] =
        route(app, enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc
        .getElementById("ageOver17-error")
        .html() shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Select yes if you are aged 17 or over"
    }

  }

  "Calling GET .../check-tax-on-goods-you-bring-into-the-uk" should {

    "redirect to where-good-bought, changing session id, keep any session data for bcpaccess when amendments feature is off" in new LocalSetup {

      when(injected[AppConfig].isAmendmentsEnabled) `thenReturn` false

      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk").withSession("bcpaccess" -> "true")
      val sessionId: Option[String]                        = fakeRequest.session.get("sessionId")
      val response: Future[Result]                         = route(app, fakeRequest).get

      status(response)                        shouldBe SEE_OTHER
      redirectLocation(response)              shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
      session(response).data.get("bcpaccess") shouldBe Some("true")
      session(response).data.get("sessionId")   should not be sessionId

    }

    "redirect to previous-declaration, changing session id, keep any session data for bcpaccess when amendments feature is on" in new LocalSetup {

      when(injected[AppConfig].isAmendmentsEnabled) `thenReturn` true

      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk").withSession("bcpaccess" -> "true")
      val sessionId: Option[String]                        = fakeRequest.session.get("sessionId")

      val response: Future[Result] = route(app, fakeRequest).get

      status(response)                        shouldBe SEE_OTHER
      redirectLocation(response)              shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
      session(response).data.get("bcpaccess") shouldBe Some("true")
      session(response).data.get("sessionId")   should not be sessionId

    }

  }

  "calling GET .../keepAlive" should {
    "return a response OK" in new LocalSetup {
      when(injected[Cache].updateUpdatedAtTimestamp(any())) `thenReturn` Future
        .successful(JsObject.empty)
      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/keep-alive")).get
      status(response) shouldBe OK

    }

    "return a response INTERNAL_SERVER_ERROR" in new LocalSetup {
      when(injected[Cache].updateUpdatedAtTimestamp(any())) `thenReturn` Future
        .failed(new Exception("failed updating timestamp"))
      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/keep-alive")).get
      status(response) shouldBe INTERNAL_SERVER_ERROR

    }
  }

  "Invoking GET .../gb-ni-no-need-to-use-service" should {

    "load no need to use service gbni page if they have paid VAT and excise and are a UK resident" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
        Some(JourneyData(Some(false), Some("greatBritain"), Some(true), Some(true), Some(true), Some(true)))
      )

      val response: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-no-need-to-use-service")
      ).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document   = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "You do not need to use this service"

      verify(controller.cache, times(1)).fetch(any())
    }
  }

  "Calling POST /check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back" should {
    def test(page: String, claimedVatRes: Boolean, locationRoute: String): Unit =
      s"redirect to the $page page" when {
        s"claimedVatRes is set to $claimedVatRes" in new LocalSetup {
          override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
            Some(
              JourneyData(
                prevDeclaration = Some(false),
                euCountryCheck = Some("euOnly"),
                arrivingNICheck = Some(false),
                isVatResClaimed = Some(claimedVatRes)
              )
            )
          )

          val response: Future[Result] = route(
            app,
            enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")
              .withFormUrlEncodedBody(
                "claimedVatRes" -> s"$claimedVatRes"
              )
          ).get

          status(response)               shouldBe SEE_OTHER
          redirectLocation(response).get shouldBe s"/check-tax-on-goods-you-bring-into-the-uk/$locationRoute"
        }
      }

    val input: Seq[(String, Boolean, String)] = Seq(
      ("private travel", true, "private-travel"),
      ("duty free", false, "duty-free")
    )

    input.foreach(args => test.tupled(args))
  }

  "Calling POST /check-tax-on-goods-you-bring-into-the-uk/duty-free" should {
    def test(page: String, isBringingDutyFree: Boolean, euCountryCheck: String, locationRoute: String): Unit =
      s"redirect to the $page page" when {
        s"isBringingDutyFree is set to $isBringingDutyFree and euCountryCheck is set to $euCountryCheck" in new LocalSetup {
          override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(
            Some(
              JourneyData(
                prevDeclaration = Some(false),
                euCountryCheck = Some(euCountryCheck),
                arrivingNICheck = Some(false),
                isVatResClaimed = Some(false),
                isBringingDutyFree = Some(isBringingDutyFree)
              )
            )
          )

          val response: Future[Result] = route(
            app,
            enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free")
              .withFormUrlEncodedBody(
                "isBringingDutyFree" -> s"$isBringingDutyFree"
              )
          ).get

          status(response)               shouldBe SEE_OTHER
          redirectLocation(response).get shouldBe s"/check-tax-on-goods-you-bring-into-the-uk/$locationRoute"
        }
      }

    val input: Seq[(String, Boolean, String, String)] = Seq(
      ("goods bought inside EU", false, "euOnly", "goods-bought-into-northern-ireland-inside-eu"),
      ("goods bought inside and outside EU", false, "both", "goods-brought-into-great-britain-iom"),
      ("duty free allowance question EU", true, "euOnly", "duty-free-eu"),
      ("duty free allowance question mix", true, "both", "duty-free-mix")
    )

    input.foreach(args => test.tupled(args))
  }
}
