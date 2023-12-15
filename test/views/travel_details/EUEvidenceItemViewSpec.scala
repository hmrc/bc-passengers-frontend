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

package views.travel_details

import forms.EUEvidenceItemForm
import models.ProductPath
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.eu_evidence_item

class EUEvidenceItemViewSpec extends BaseViewSpec {

  private val productPath: ProductPath = ProductPath(path = "tobacco/cigars")

  private val validForm: Form[Boolean] = EUEvidenceItemForm.form.bind(Map("eUEvidenceItem" -> "true"))

  private val emptyForm: Form[Boolean] = EUEvidenceItemForm.form.bind(Map("eUEvidenceItem" -> ""))

  val viewViaApply: HtmlFormat.Appendable = injected[eu_evidence_item].apply(
    form = validForm,
    backLink = None,
    path = productPath,
    iid = "iid0"
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[eu_evidence_item].render(
    form = validForm,
    backLink = None,
    path = productPath,
    iid = "iid0",
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[eu_evidence_item].f(
    validForm,
    None,
    productPath,
    "iid0"
  )(request, messages, appConfig)

  private def buildView(form: Form[Boolean]): HtmlFormat.Appendable =
    injected[eu_evidence_item].apply(
      form = form,
      backLink = None,
      path = productPath,
      iid = "iid0"
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  "EUEvidenceItemView" when {
    renderViewTest(
      title =
        "Do you have evidence this item was originally produced or made in the EU? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Do you have evidence this item was originally produced or made in the EU?"
    )

    "formWithErrors" should {
      val expectedErrors = List(
        "#eUEvidenceItem-value-yes" -> messages("error.evidence_eu_item")
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
