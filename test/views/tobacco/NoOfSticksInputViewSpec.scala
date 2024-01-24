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

package views.tobacco

import controllers.TobaccoInputController
import models._
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.tobacco.no_of_sticks_input

class NoOfSticksInputViewSpec extends BaseViewSpec {

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

  private val validForm: Form[TobaccoDto] = injected[TobaccoInputController]
    .noOfSticksForm(productPath)
    .bind(
      Map(
        "noOfSticks" -> "500",
        "country"    -> "FR",
        "currency"   -> "EUR",
        "cost"       -> "100.00"
      )
    )

  val viewViaApply: HtmlFormat.Appendable = injected[no_of_sticks_input].apply(
    form = validForm,
    backLink = None,
    customBackLink = false,
    product = productTreeLeaf,
    path = productPath,
    iid = Some("iid0"),
    countries = nonEuropeanCountries,
    countriesEU = europeanCountries,
    currencies = currencies,
    journeyStart = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[no_of_sticks_input].render(
    form = validForm,
    backLink = None,
    customBackLink = false,
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

  val viewViaF: HtmlFormat.Appendable = injected[no_of_sticks_input].f(
    validForm,
    None,
    false,
    productTreeLeaf,
    productPath,
    Some("iid0"),
    nonEuropeanCountries,
    europeanCountries,
    currencies,
    None
  )(request, messages, appConfig)

  "NoOfSticksInputView" when {
    renderViewTest(
      title = "Tell us about the cigarettes - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Tell us about the Cigarettes"
    )
  }
}
