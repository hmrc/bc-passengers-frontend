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

import forms.UKExcisePaidItemForm.form
import models.ProductPath
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.ukexcise_paid_item

class UKExcisePaidItemViewSpec extends BaseViewSpec {

  private val productPath: ProductPath = ProductPath(path = "tobacco/cigarettes")

  private val validForm: Form[Boolean] = form.bind(Map("uKExcisePaidItem" -> "true"))

  val viewViaApply: HtmlFormat.Appendable = injected[ukexcise_paid_item].apply(
    form = validForm,
    backLink = None,
    path = productPath,
    iid = "iid0"
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[ukexcise_paid_item].render(
    form = validForm,
    backLink = None,
    path = productPath,
    iid = "iid0",
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[ukexcise_paid_item].f(
    validForm,
    None,
    productPath,
    "iid0"
  )(request, messages, appConfig)

  "UKExcisePaidItemView" when {
    renderViewTest(
      title = "Did you pay UK excise duty when buying this item? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Did you pay UK excise duty when buying this item?"
    )
  }
}
