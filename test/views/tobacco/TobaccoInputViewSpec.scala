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

import controllers.TobaccoInputController
import models._
import play.api.data.Form
import play.twirl.api.{Html, HtmlFormat}
import views.BaseViewSpec
import views.html.tobacco.tobacco_input

class TobaccoInputViewSpec extends BaseViewSpec {

  private val productPath: ProductPath = ProductPath(path = "tobacco/cigars")

  private val productTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "cigars",
    name = "label.tobacco.cigars",
    rateID = "TOB/A1/CIGAR",
    templateId = "cigars",
    applicableLimits = List("L-CIGAR")
  )

  private val currencies: List[Currency] = List(
    Currency(
      code = "EUR",
      displayName = "title.euro_eur",
      valueForConversion = Some("EUR"),
      currencySynonyms = List("Europe", "European")
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
        "noOfSticks"     -> "10",
        "weightOrVolume" -> "50"
      )
    )

  val viewViaApply: HtmlFormat.Appendable = injected[tobacco_input].apply(
    form = validForm,
    product = productTreeLeaf,
    path = productPath,
    iid = Some("iid0"),
    countries = nonEuropeanCountries,
    countriesEU = europeanCountries,
    currencies = currencies,
    journeyStart = None
  )(
    content = Html("")
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[tobacco_input].render(
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

  val viewViaF: HtmlFormat.Appendable = injected[tobacco_input].f(
    validForm,
    productTreeLeaf,
    productPath,
    Some("iid0"),
    nonEuropeanCountries,
    europeanCountries,
    currencies,
    None
  )(Html(""))(request, messages, appConfig)

  "TobaccoInputView" when {
    renderViewTest(
      title = "Tell us about the cigars - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Tell us about the Cigars"
    )
  }
}
