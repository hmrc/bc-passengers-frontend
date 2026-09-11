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
import views.BaseViewSpec
import views.html.travel_details.goods_bought_inside_and_outside_eu

class GoodsBoughtInsideAndOutsideEUViewSpec extends BaseViewSpec {

  private val validForm: Form[BringingOverAllowanceDto] = form.bind(Map("bringingOverAllowance" -> "true"))

  private def viewWithToggle(enabled: Boolean): HtmlFormat.Appendable =
    injected[goods_bought_inside_and_outside_eu]
      .apply(validForm, None)(request, messages, appConfigWith("features.wine-still-or-sparkling" -> enabled))

  val viewViaApply: HtmlFormat.Appendable = injected[goods_bought_inside_and_outside_eu].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

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

  "GoodsBoughtInsideAndOutsideEUView with the wine-still-or-sparkling toggle ON" should {

    lazy val body = viewWithToggle(true).body

    "show title header" in {
      body should include(messages("heading.goods_brought_into_gb.still-or-sparkling"))
    }

    "show the personal-allowance intro" in {
      body should include(messages("text.gb.allowance.still-or-sparkling"))
    }

    "show a single combined under-17 inset (alcohol or tobacco), without the two original insets" in {
      body should include(messages("text.gb.allowance.under_17.still-or-sparkling"))
      body should not include messages("text.ni.allowance.msg_3")
      body should not include messages("text.allowance.msg_6")
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

    "stack the Yes/No buttons" in {
      body should not include "govuk-radios--inline"
    }
  }

  "GoodsBoughtInsideAndOutsideEUView with the wine-still-or-sparkling toggle OFF" should {

    lazy val body = viewWithToggle(false).body

    "show title header with" in {
      body should include(messages("heading.goods_brought_into_gb"))
    }

    "show the original intro and section heading" in {
      body should include(messages("text.gb.allowance"))
      body should include(messages("text.alcohol_allowance"))
    }

    "show the two original under-17 insets" in {
      body should include(messages("text.ni.allowance.msg_3"))
      body should include(messages("text.allowance.msg_6"))
    }

    "not show the restructured content" in {
      body should not include messages("text.gb.allowance.under_17.still-or-sparkling")
      body should include(messages("text.gb.allowance.alc_2"))
      body should not include messages("text.gb.allowance.alc_2.still-or-sparkling")
    }

    "keep the Yes/No buttons inline" in {
      body should include("govuk-radios--inline")
    }
  }
}
