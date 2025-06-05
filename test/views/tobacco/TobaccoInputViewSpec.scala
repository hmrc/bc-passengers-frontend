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

package views.tobacco

import forms.TobaccoInputForm
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

  private val emptyForm: Form[TobaccoDto] =
    injected[TobaccoInputForm]
      .cigarAndCigarilloForm(productPath)
      .bind(
        Map(
          "noOfSticks"     -> "",
          "weightOrVolume" -> "",
          "country"        -> "",
          "originCountry"  -> "",
          "currency"       -> "",
          "cost"           -> ""
        )
      )

  private val emptySticksForm: Form[TobaccoDto] =
    injected[TobaccoInputForm]
      .cigarAndCigarilloForm(productPath)
      .bind(
        Map(
          "noOfSticks"     -> "",
          "weightOrVolume" -> "50",
          "country"        -> "FR",
          "currency"       -> "EUR",
          "cost"           -> "50"
        )
      )

  private val emptyVolumeForm: Form[TobaccoDto] =
    injected[TobaccoInputForm]
      .cigarAndCigarilloForm(productPath)
      .bind(
        Map(
          "noOfSticks"     -> "10",
          "weightOrVolume" -> "",
          "country"        -> "FR",
          "currency"       -> "EUR",
          "cost"           -> "50"
        )
      )

  private val emptyPriceForm: Form[TobaccoDto] =
    injected[TobaccoInputForm]
      .cigarAndCigarilloForm(productPath)
      .bind(
        Map(
          "noOfSticks"     -> "10",
          "weightOrVolume" -> "50",
          "country"        -> "FR",
          "currency"       -> "EUR",
          "cost"           -> ""
        )
      )

  private val validForm: Form[TobaccoDto] =
    injected[TobaccoInputForm].resilientForm
      .bind(
        Map(
          "noOfSticks"     -> "10",
          "weightOrVolume" -> "50"
        )
      )

  val viewViaApply: HtmlFormat.Appendable = injected[tobacco_input].apply(
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
    content = Html("")
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[tobacco_input].render(
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
    content = Html(""),
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[tobacco_input].ref.f(
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
  )(Html(""))(request, messages, appConfig)

  val euOnlyView: HtmlFormat.Appendable = injected[tobacco_input].apply(
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
    content = Html("")
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  private def buildView(form: Form[TobaccoDto]): HtmlFormat.Appendable =
    injected[tobacco_input].apply(
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
      content = Html("")
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val expectedEmptyFormErrors: Seq[(String, String)] = List(
    "#noOfSticks"     -> messages("error.no_of_sticks.required.tobacco.cigars"),
    "#weightOrVolume" -> messages("error.weight_or_volume.required.tobacco.cigars"),
    "#country"        -> messages("error.country.invalid"),
    "#currency"       -> messages("error.currency.invalid"),
    "#cost"           -> messages("error.required.tobacco.cigars")
  )

  val expectedEmptyVolumeErrors: Seq[(String, String)] = List(
    "#weightOrVolume" -> messages("error.weight_or_volume.required.tobacco.cigars")
  )

  val expectedEmptySticksErrors: Seq[(String, String)] = List(
    "#noOfSticks" -> messages("error.no_of_sticks.required.tobacco.cigars")
  )

  val expectedEmptyPriceErrors: Seq[(String, String)] = List(
    "#cost" -> messages("error.required.tobacco.cigars")
  )

  val invalidTestCases: Seq[(String, Form[TobaccoDto], Seq[(String, String)])] = Seq(
    Tuple3("Empty form", emptyForm, expectedEmptyFormErrors),
    Tuple3("Empty no of sticks", emptySticksForm, expectedEmptySticksErrors),
    Tuple3("Empty volume", emptyVolumeForm, expectedEmptyVolumeErrors),
    Tuple3("Empty price", emptyPriceForm, expectedEmptyPriceErrors)
  )

  "TobaccoInputView" when {
    renderViewTest(
      title = "Tell us about the cigars - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Tell us about the Cigars"
    )

    "formWithErrors" should
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
      }
  }
}
