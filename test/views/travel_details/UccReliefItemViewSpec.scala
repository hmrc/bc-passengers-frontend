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

package views.travel_details

import forms.UccReliefItemForm.form
import models.ProductPath
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.ucc_relief_item

class UccReliefItemViewSpec extends BaseViewSpec {

  private val productPath: ProductPath = ProductPath(path = "tobacco/chewing-tobacco")

  private val validForm: Form[Boolean] = form.bind(Map("isUccRelief" -> "true"))

  val viewViaApply: HtmlFormat.Appendable = injected[ucc_relief_item].apply(
    form = validForm,
    backLink = None,
    path = productPath,
    iid = "iid0"
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[ucc_relief_item].render(
    form = validForm,
    backLink = None,
    path = productPath,
    iid = "iid0",
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[ucc_relief_item].f(
    validForm,
    None,
    productPath,
    "iid0"
  )(request, messages, appConfig)

  "UccReliefItemView" when {
    renderViewTest(
      title = "Tax and duty exemptions for non-UK residents - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Tax and duty exemptions for non-UK residents"
    )
  }
}
