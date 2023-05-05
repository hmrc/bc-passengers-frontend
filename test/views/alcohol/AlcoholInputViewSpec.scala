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

package views.alcohol

import config.AppConfig
import controllers.AlcoholInputController
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.{Html, HtmlFormat}
import util.BaseSpec
import views.html.alcohol.alcohol_input

class AlcoholInputViewSpec extends BaseSpec with Injecting {

  private val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private val appConfig: AppConfig                     = injected[AppConfig]
  private val messagesApi: MessagesApi                 = injected[MessagesApi]
  private val messages: Messages                       = messagesApi.preferred(request)

  private val productPath: ProductPath = ProductPath(path = "alcohol/wine")

  private val productTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "wine",
    name = "label.alcohol.wine",
    rateID = "ALC/A3/WINE",
    templateId = "alcohol",
    applicableLimits = List("L-WINE")
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

  private val validForm: Form[AlcoholDto] = injected[AlcoholInputController]
    .alcoholForm(productPath)
    .bind(
      Map(
        "weightOrVolume" -> "90",
        "country"        -> "FR",
        "currency"       -> "EUR",
        "cost"           -> "4,444.00"
      )
    )

  private def document(html: Html): Document = Jsoup.parse(html.toString())

  private trait ViewFixture {
    val viewViaApply: HtmlFormat.Appendable = inject[alcohol_input].apply(
      form = validForm,
      product = productTreeLeaf,
      path = productPath,
      iid = Some("iid0"),
      countries = nonEuropeanCountries,
      countriesEU = europeanCountries,
      currencies = currencies,
      journeyStart = None
    )(request, messages, appConfig)

    val viewViaRender: HtmlFormat.Appendable = inject[alcohol_input].render(
      form = validForm,
      product = productTreeLeaf,
      path = productPath,
      iid = Some("iid0"),
      countries = nonEuropeanCountries,
      countriesEU = europeanCountries,
      currencies = currencies,
      journeyStart = None,
      request = request,
      messages = messages,
      appConfig = appConfig
    )

    val viewViaF: HtmlFormat.Appendable =
      inject[alcohol_input].f(
        validForm,
        productTreeLeaf,
        productPath,
        Some("iid0"),
        nonEuropeanCountries,
        europeanCountries,
        currencies,
        None
      )(request, messages, appConfig)
  }

  "AlcoholInputView" when {
    ".apply" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaApply
        ).title shouldBe "Tell us about the wine - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(viewViaApply).select("h1").text shouldBe "Tell us about the Wine"
      }
    }

    ".render" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaRender
        ).title shouldBe "Tell us about the wine - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(viewViaRender).select("h1").text shouldBe "Tell us about the Wine"
      }
    }

    ".f" should {
      "display the correct title" in new ViewFixture {
        document(viewViaF).title shouldBe "Tell us about the wine - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(viewViaF).select("h1").text shouldBe "Tell us about the Wine"
      }
    }
  }
}
