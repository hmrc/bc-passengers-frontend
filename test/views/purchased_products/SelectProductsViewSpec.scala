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

class SelectProductsViewSpec extends BaseViewSpec {

  private val productPath: ProductPath = ProductPath(path = "other-goods/electronic-devices")

  private val validForm: Form[SelectProductsDto] = form.bind(
    Map("tokens" -> "televisions")
  )

  val viewViaApply: HtmlFormat.Appendable = injected[select_products].apply(
    selectProductsForm = validForm,
    items = List(("label.other-goods.electronic-devices.televisions", "televisions")),
    path = productPath,
    None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[select_products].render(
    selectProductsForm = validForm,
    items = List(("televisions", "label.other-goods.electronic-devices.televisions")),
    path = productPath,
    request = request,
    messages = messages,
    appConfig = appConfig,
    backLink = None
  )

  val viewViaF: HtmlFormat.Appendable = injected[select_products].ref.f(
    validForm,
    List(("televisions", "label.other-goods.electronic-devices.televisions")),
    productPath,
    None
  )(request, messages, appConfig)

  "SelectProductsView" when
    renderViewTest(
      title = "What electronic devices do you want to add? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "What electronic devices do you want to add?"
    )
}
