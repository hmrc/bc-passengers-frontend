/*
 * Copyright 2023 HM Revenue & Customs
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

package views.tobacco

import config.AppConfig
import controllers.TobaccoInputController
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.{Html, HtmlFormat}
import util.BaseSpec
import views.html.tobacco.tobacco_input

class TobaccoInputViewSpec extends BaseSpec with Injecting {

  private val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private val appConfig: AppConfig                     = injected[AppConfig]
  private val messagesApi: MessagesApi                 = injected[MessagesApi]
  private val messages: Messages                       = messagesApi.preferred(request)

  private val productPath: ProductPath = ProductPath(path = "tobacco/cigarettes")

  private val productTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "cigarettes",
    name = "label.tobacco.cigarettes",
    rateID = "TOB/A1/CIGRT",
    templateId = "cigarettes",
    applicableLimits = List("L-CIGRT")
  )

  private val currencies: List[Currency] = List(
    Currency(
      code = "USD",
      displayName = "title.usa_dollars_usd",
      valueForConversion = Some("USD"),
      currencySynonyms = List("USD", "USA", "US", "United States of America", "American")
    )
  )

  private val europeanCountries: List[Country] = List(
    Country(
      code = "FR",
      countryName = "title.france",
      alphaTwoCode = "FR",
      isEu = true,
      isCountry = true,
      countrySynonyms = Nil
    )
  )

  private val nonEuropeanCountries: List[Country] = List(
    Country(
      code = "EG",
      countryName = "title.egypt",
      alphaTwoCode = "EG",
      isEu = false,
      isCountry = true,
      countrySynonyms = Nil
    )
  )

  private val validForm: Form[TobaccoDto] = injected[TobaccoInputController].resilientForm
    .bind(
      Map(
        "noOfSticks"     -> "1000",
        "weightOrVolume" -> "500"
      )
    )

  private def document(html: Html): Document = Jsoup.parse(html.toString())

  private trait ViewFixture {
    val viewViaApply: HtmlFormat.Appendable = inject[tobacco_input].apply(
      form = validForm,
      product = productTreeLeaf,
      path = productPath,
      iid = Some("iid0"),
      countries = nonEuropeanCountries,
      countriesEU = europeanCountries,
      currencies = currencies,
      journeyStart = None
    )(content = Html(""))(request, messages, appConfig)

    val viewViaRender: HtmlFormat.Appendable = inject[tobacco_input].render(
      form = validForm,
      product = productTreeLeaf,
      path = productPath,
      iid = Some("iid0"),
      countries = nonEuropeanCountries,
      countriesEU = europeanCountries,
      currencies = currencies,
      journeyStart = None,
      content = Html(""),
      request = request,
      messages = messages,
      appConfig = appConfig
    )

    val viewViaF: HtmlFormat.Appendable =
      inject[tobacco_input].f(
        validForm,
        productTreeLeaf,
        productPath,
        Some("iid0"),
        nonEuropeanCountries,
        europeanCountries,
        currencies,
        None
      )(Html(""))(request, messages, appConfig)
  }

  "TobaccoInputView" when {
    ".apply" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaApply
        ).title shouldBe "Tell us about the cigarettes - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(viewViaApply).select("h1").text shouldBe "Tell us about the Cigarettes"
      }
    }

    ".render" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaRender
        ).title shouldBe "Tell us about the cigarettes - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(viewViaRender).select("h1").text shouldBe "Tell us about the Cigarettes"
      }
    }

    ".f" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaF
        ).title shouldBe "Tell us about the cigarettes - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(viewViaF).select("h1").text shouldBe "Tell us about the Cigarettes"
      }
    }
  }
}
