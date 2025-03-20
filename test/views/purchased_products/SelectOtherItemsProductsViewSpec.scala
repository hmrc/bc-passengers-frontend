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

package views.purchased_products

import models.SelectProductsDto.form
import models.{ProductPath, SelectProductsDto}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.purchased_products.select_products

class SelectOtherItemsProductsViewSpec extends BaseViewSpec {

  private val productPath: ProductPath = ProductPath(path = "other-goods")

  private val validForm: Form[SelectProductsDto] = form
    .fill(SelectProductsDto(List("televisions")))

  private val emptyForm: Form[SelectProductsDto] = form.bind(Map.empty[String, String])

  private val items = List(
    ("label.other-goods.electronic-devices.televisions", "televisions"),
    ("label.other-goods.electronic-devices.other", "other")
  )

  private def buildView(form: Form[SelectProductsDto]): HtmlFormat.Appendable =
    injected[select_products].apply(
      selectProductsForm = form,
      items = items,
      path = productPath,
      None
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val viewViaApply: HtmlFormat.Appendable = buildView(form = validForm)

  val viewViaRender: HtmlFormat.Appendable = injected[select_products].render(
    selectProductsForm = validForm,
    items = items,
    path = productPath,
    request = request,
    messages = messages,
    appConfig = appConfig,
    backLink = None
  )

  val viewViaF: HtmlFormat.Appendable = injected[select_products].ref.f(
    validForm,
    items,
    productPath,
    None
  )(request, messages, appConfig)

  "SelectOtherItemsProductsViewSpec" when {
    renderViewTest(
      title = "What type of other goods do you want to add? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "What type of other goods do you want to add?"
    )

    "formWithErrors" should {

      "have error prefix in title" in {
        val doc = document(buildView(form = emptyForm))
        doc.title() should startWith(messages("label.error"))
      }

      "have all info in error summary" in {
        val doc = document(buildView(form = emptyForm))
        doc.title()                            should startWith(messages("label.error"))
        messages("label.there_is_a_problem") shouldBe getErrorTitle(doc)
        List(
          "#tokens-label.other-goods.electronic-devices.televisions" -> messages("error.required.other-goods")
        )                                    shouldBe getErrorsInSummary(doc)
      }

      "have all errors in each input" in {
        val doc = document(buildView(form = emptyForm))
        doc.title()                                                                    should startWith(messages("label.error"))
        messages("label.there_is_a_problem")                                         shouldBe getErrorTitle(doc)
        List(messages("label.error") + " " + messages("error.required.other-goods")) shouldBe getErrorsInFieldSet(doc)
      }
    }
  }
}
