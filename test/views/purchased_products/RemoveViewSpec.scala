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

import models.ConfirmRemoveDto.form
import models.{ConfirmRemoveDto, ProductPath}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.purchased_products.remove

class RemoveViewSpec extends BaseViewSpec {

  private val productPath: ProductPath = ProductPath(path = "tobacco/cigars")

  private val validForm: Form[ConfirmRemoveDto] = form.bind(Map("confirmRemove" -> "true"))

  val viewViaApply: HtmlFormat.Appendable = injected[remove].apply(
    form = validForm,
    path = productPath,
    iid = "iid0"
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[remove].render(
    form = validForm,
    path = productPath,
    iid = "iid0",
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[remove].f(
    validForm,
    productPath,
    "iid0"
  )(request, messages, appConfig)

  "RemoveView" when {
    renderViewTest(
      title = "Do you want to remove these cigars? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Do you want to remove these cigars?"
    )
  }
}
