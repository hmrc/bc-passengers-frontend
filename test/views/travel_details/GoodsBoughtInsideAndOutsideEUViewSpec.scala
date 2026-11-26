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

package views.travel_details

import models.BringingOverAllowanceDto
import models.BringingOverAllowanceDto.form
import play.api.data.Form
import play.twirl.api.HtmlFormat
import util.VapingProductsFeature
import views.BaseViewSpec
import views.html.travel_details.goods_bought_inside_and_outside_eu

class GoodsBoughtInsideAndOutsideEUViewSpec extends BaseViewSpec with VapingProductsFeature {

  private val validForm: Form[BringingOverAllowanceDto] = form.bind(Map("bringingOverAllowance" -> "true"))

  private def viewWith(vaping: Boolean): HtmlFormat.Appendable =
    injected[goods_bought_inside_and_outside_eu]
      .apply(validForm, None)(request, messages, appConfigWith(vapingProductsFeatureKey -> vaping))

  val viewViaApply: HtmlFormat.Appendable = injected[goods_bought_inside_and_outside_eu].apply(
    form = validForm,
    backLink = None
  )(request = request, messages = messages, appConfig = appConfig)

  val viewViaRender: HtmlFormat.Appendable = injected[goods_bought_inside_and_outside_eu].render(
    form = validForm,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[goods_bought_inside_and_outside_eu].ref.f(
    validForm,
    None
  )(request, messages, appConfig)

  "GoodsBoughtInsideAndOutsideEUView with the vaping toggle ON" should {

    lazy val view = viewWith(vaping = true)
    lazy val body = view.body

    "show the still-or-sparkling title header" in {
      document(view).select("h1").text shouldBe messages("heading.goods_brought_into_gb.still-or-sparkling")
    }

    "show the vaping personal-allowance intro" in {
      body should include(messages("text.gb.allowance.still-or-sparkling_vp"))
    }

    "show the under-17 inset with the vaping text" in {
      body should include(messages("text.gb.allowance.under_17.still-or-sparkling_vp"))
    }

    "show the 'Your allowance for' section headings" in {
      body should include(messages("text.alcohol_allowance.still-or-sparkling"))
      body should include(messages("text.tobacco_allowance.still-or-sparkling"))
      body should include(messages("text.other_goods_allowance.still-or-sparkling"))
    }

    "show the 'If you bring more than your allowance' sub-heading" in {
      body should include(messages("text.gb.allowance.over_heading.still-or-sparkling"))
    }

    "show the merged alcohol allowance bullets" in {
      body should include(messages("text.gb.allowance.alc_1.still-or-sparkling"))
      body should include(messages("text.gb.allowance.alc_2.still-or-sparkling"))
      body should include(messages("text.gb.allowance.alc_3.still-or-sparkling"))
    }

    "show the vaping products allowance section" in {
      body should include(messages("text.vaping_products_allowance"))
    }

    "show the 'use this service to value your other goods' block" in {
      body should include(messages("text.allowance.other_goods_value_intro.still-or-sparkling"))
      body should include(messages("text.allowance.other_goods_value_1.still-or-sparkling"))
      body should include(messages("text.allowance.other_goods_value_2.still-or-sparkling"))
      body should include(messages("text.allowance.other_goods_value_3.still-or-sparkling"))
    }

    "stack the Yes/No buttons" in {
      body should not include "govuk-radios--inline"
    }
  }

  "GoodsBoughtInsideAndOutsideEUView with the vaping toggle OFF" should {

    lazy val view = viewWith(vaping = false)
    lazy val body = view.body

    "show the original title header" in {
      document(view).select("h1").text shouldBe messages("heading.goods_brought_into_gb")
    }

    "show the original intro" in {
      body should include(messages("text.gb.allowance"))
    }

    "not show the under-17 inset at all" in {
      body should not include messages("text.gb.allowance.under_17.still-or-sparkling")
      body should not include messages("text.gb.allowance.under_17.still-or-sparkling_vp")
    }

    "show the original 'over allowance' text for alcohol and tobacco" in {
      body should include(messages("text.gb.allowance.msg_2"))
      body should include(messages("text.ni.allowance.msg_3"))
      body should include(messages("text.allowance.msg_5"))
      body should include(messages("text.allowance.msg_6"))
    }

    "not show the vaping products allowance section" in {
      body should not include messages("text.vaping_products_allowance")
    }

    "show the original plain other-goods bullet, not the restructured block" in {
      body should include(messages("text.allowance.otg_1"))
      body should include(messages("text.allowance.msg_7"))
      body should not include messages("text.allowance.other_goods_value_intro.still-or-sparkling")
    }

    "keep the Yes/No buttons inline" in {
      body should include("govuk-radios--inline")
    }
  }
}
