/*
 * Copyright 2025 HM Revenue & Customs
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

import forms.AlcoholInputForm
import models.*
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.alcohol.alcohol_input

class AlcoholInputViewSpec extends BaseViewSpec {

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

  private val emptyForm: Form[AlcoholDto] = injected[AlcoholInputForm]
    .alcoholForm(productPath)
    .bind(
      Map(
        "weightOrVolume" -> "",
        "country"        -> "",
        "currency"       -> "",
        "cost"           -> ""
      )
    )

  private val emptyCountryForm: Form[AlcoholDto] = injected[AlcoholInputForm]
    .alcoholForm(productPath)
    .bind(
      Map(
        "weightOrVolume" -> "90",
        "country"        -> "",
        "currency"       -> "EUR",
        "cost"           -> "4,444.00"
      )
    )

  private val emptyVolumeForm: Form[AlcoholDto] = injected[AlcoholInputForm]
    .alcoholForm(productPath)
    .bind(
      Map(
        "weightOrVolume" -> "",
        "country"        -> "FR",
        "currency"       -> "EUR",
        "cost"           -> "4,444.00"
      )
    )

  private val emptyPriceForm: Form[AlcoholDto] = injected[AlcoholInputForm]
    .alcoholForm(productPath)
    .bind(
      Map(
        "weightOrVolume" -> "90",
        "country"        -> "FR",
        "currency"       -> "EUR",
        "cost"           -> ""
      )
    )

  private val validForm: Form[AlcoholDto] = injected[AlcoholInputForm]
    .alcoholForm(productPath)
    .bind(
      Map(
        "weightOrVolume" -> "90",
        "country"        -> "FR",
        "currency"       -> "EUR",
        "cost"           -> "4,444.00"
      )
    )

  val viewViaApply: HtmlFormat.Appendable = injected[alcohol_input].apply(
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

  val viewViaRender: HtmlFormat.Appendable = injected[alcohol_input].render(
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

  val viewViaF: HtmlFormat.Appendable = injected[alcohol_input].ref.f(
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

  val euOnlyView: HtmlFormat.Appendable = injected[alcohol_input].apply(
    form = validForm,
    backLink = None,
    customBackLink = false,
    product = productTreeLeaf,
    path = productPath,
    iid = Some("iid0"),
    countries = nonEuropeanCountries,
    countriesEU = europeanCountries,
    currencies = currencies,
    journeyStart = Some("euOnly")
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val noIidView: HtmlFormat.Appendable = injected[alcohol_input].apply(
    form = validForm,
    backLink = None,
    customBackLink = false,
    product = productTreeLeaf,
    path = productPath,
    iid = None,
    countries = nonEuropeanCountries,
    countriesEU = europeanCountries,
    currencies = currencies,
    journeyStart = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  private def buildView(form: Form[AlcoholDto]): HtmlFormat.Appendable =
    injected[alcohol_input].apply(
      form = form,
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

  val expectedEmptyFormErrors: Seq[(String, String)] = List(
    "#weightOrVolume" -> messages("error.required.volume.alcohol.wine"),
    "#country"        -> messages("error.country.invalid"),
    "#currency"       -> messages("error.currency.invalid"),
    "#cost"           -> messages("error.required.alcohol.wine")
  )

  val expectedEmptyVolumeErrors: Seq[(String, String)] = List(
    "#weightOrVolume" -> messages("error.required.volume.alcohol.wine")
  )

  val expectedEmptyCountryErrors: Seq[(String, String)] = List(
    "#country" -> messages("error.country.invalid")
  )

  val expectedEmptyPriceErrors: Seq[(String, String)] = List(
    "#cost" -> messages("error.required.alcohol.wine")
  )

  val invalidTestCases: Seq[(String, Form[AlcoholDto], Seq[(String, String)])] = Seq(
    Tuple3("Empty form", emptyForm, expectedEmptyFormErrors),
    Tuple3("Empty volume", emptyVolumeForm, expectedEmptyVolumeErrors),
    Tuple3("Empty price", emptyPriceForm, expectedEmptyPriceErrors),
    Tuple3("Empty country", emptyCountryForm, expectedEmptyCountryErrors)
  )

  "AlcoholInputView" when {
    renderViewTest(
      title = "Tell us about the wine - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Tell us about the Wine"
    )

    "formWithErrors" should {
      invalidTestCases.foreach { testCase =>
        s"have error prefix in title for ${testCase._1}" in {
          val doc = document(buildView(form = testCase._2))
          doc.title() should startWith(messages("label.error"))
        }

        s"have all info in error summary for ${testCase._1}" in {
          val doc = document(buildView(form = testCase._2))
          doc.title()                            should startWith(messages("label.error"))
          messages("label.there_is_a_problem") shouldBe getErrorTitle(doc)

          testCase._3 shouldBe getErrorsInSummary(doc)
        }

        s"have all errors in each input for ${testCase._1}" in {
          val doc = document(buildView(form = testCase._2))
          doc.title()                                                          should startWith(messages("label.error"))
          messages("label.there_is_a_problem")                               shouldBe getErrorTitle(doc)
          testCase._3.map(error => messages("label.error") + " " + error._2) shouldBe getErrorsInFieldSet(
            doc
          )
        }
      }
    }
  }
}
