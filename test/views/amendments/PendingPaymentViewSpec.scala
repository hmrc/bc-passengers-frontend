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

package views.amendments

import forms.PendingPaymentForm
import models._
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.amendments.pending_payment

class PendingPaymentViewSpec extends BaseViewSpec {

  private val weightOrVolume: BigDecimal = 40

  private val validForm: Form[Boolean] = PendingPaymentForm.form.bind(Map("pendingPayment" -> "true"))

  private val emptyForm: Form[Boolean] = PendingPaymentForm.form.bind(Map("pendingPayment" -> ""))

  private val calculation: Calculation = Calculation(
    excise = "0.00",
    customs = "0.00",
    vat = "0.00",
    allTax = "0.00"
  )

  private val descriptionLabels: DescriptionLabels = DescriptionLabels(
    description = "label.X_litres_X",
    args = List("40", "label.alcohol.wine")
  )

  private val currency: Currency = Currency(
    code = "GBP",
    displayName = "title.british_pounds_gbp",
    valueForConversion = None,
    currencySynonyms = List("England", "Scotland", "Wales", "Northern Ireland", "British", "sterling", "pound", "GB")
  )

  private val country: Country = Country(
    code = "GB",
    countryName = "title.united_kingdom",
    alphaTwoCode = "GB",
    isEu = false,
    isCountry = true,
    countrySynonyms = List("England", "Scotland", "Wales", "Northern Ireland", "GB", "UK")
  )

  private val exchangeRate: ExchangeRate = ExchangeRate(
    rate = "1.20",
    date = "2023-05-06"
  )

  private val metadata: Metadata = Metadata(
    description = "40 litres wine",
    name = "label.alcohol.wine",
    cost = "100.00",
    descriptionLabels = descriptionLabels,
    currency = currency,
    country = country,
    exchangeRate = exchangeRate,
    originCountry = None
  )

  private val items: List[Item] = List(
    Item(
      rateId = "ALC/A3/WINE",
      purchaseCost = "100.00",
      noOfUnits = None,
      weightOrVolume = Some(weightOrVolume),
      calculation = calculation,
      metadata = metadata,
      isVatPaid = Some(false),
      isCustomPaid = Some(false),
      isExcisePaid = Some(false),
      isUccRelief = Some(false)
    )
  )

  private val calculatorResponseDto: CalculatorResponseDto = CalculatorResponseDto(
    items = items,
    calculation = calculation,
    allItemsUseGBP = true
  )

  val viewViaApply: HtmlFormat.Appendable = injected[pending_payment].apply(
    form = validForm,
    calculatorResponseDto = calculatorResponseDto,
    deltaCalc = Some(calculation),
    oldAllTax = "0.00",
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[pending_payment].render(
    form = validForm,
    calculatorResponseDto = calculatorResponseDto,
    deltaCalc = Some(calculation),
    oldAllTax = "0.00",
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[pending_payment].f(
    validForm,
    calculatorResponseDto,
    Some(calculation),
    "0.00",
    None
  )(request, messages, appConfig)

  private def buildView(form: Form[Boolean]): HtmlFormat.Appendable =
    injected[pending_payment].apply(
      form = form,
      calculatorResponseDto = calculatorResponseDto,
      deltaCalc = Some(calculation),
      oldAllTax = "0.00",
      backLink = None
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  "PendingPaymentView" when {
    renderViewTest(
      title =
        "You have an incomplete payment for your declaration for £0.00 - Check tax on goods you bring into the UK - GOV.UK",
      heading = "You have an incomplete payment for your declaration for £0.00"
    )

    "formWithErrors" should {
      val expectedErrors = List(
        "#pendingPayment-value-yes" -> messages("error.pay_now_if_you_want_to")
      )

      "have error prefix in title" in {
        val doc = document(buildView(form = emptyForm))
        doc.title() should startWith(messages("label.error"))
      }

      "have all info in error summary" in {
        val doc = document(buildView(form = emptyForm))
        doc.title()                            should startWith(messages("label.error"))
        messages("label.there_is_a_problem") shouldBe getErrorTitle(doc)

        expectedErrors shouldBe getErrorsInSummary(doc)
      }

      "have all errors in each input" in {
        val doc = document(buildView(form = emptyForm))
        doc.title()                                                             should startWith(messages("label.error"))
        messages("label.there_is_a_problem")                                  shouldBe getErrorTitle(doc)
        expectedErrors.map(error => messages("label.error") + " " + error._2) shouldBe getErrorsInFieldSet(doc)
      }
    }
  }
}
