/*
 * Copyright 2026 HM Revenue & Customs
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

import models.GoodsTypeDto
import models.GoodsTypeDto.form
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.purchased_products.add_item

class AddItemViewSpec extends BaseViewSpec {

  private val validForm: Form[GoodsTypeDto] = form.bind(Map("goodsType" -> "alcohol"))

  val viewViaApply: HtmlFormat.Appendable = injected[add_item].apply(validForm)(request, messages, appConfig)

  val viewViaRender: HtmlFormat.Appendable =
    injected[add_item].render(validForm, request, messages, appConfig)

  val viewViaF: HtmlFormat.Appendable = injected[add_item].ref.f(validForm)(request, messages, appConfig)

  "AddItemView" when {
    renderViewTest(
      title = "Which type of goods do you want to add? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Which type of goods do you want to add?"
    )

    "show the three goods types" in {
      val doc = document(viewViaApply)

      doc.select("#goodsType-alcohol").attr("value")     shouldBe "alcohol"
      doc.select("#goodsType-tobacco").attr("value")     shouldBe "tobacco"
      doc.select("#goodsType-other-goods").attr("value") shouldBe "other-goods"
    }
  }
}
