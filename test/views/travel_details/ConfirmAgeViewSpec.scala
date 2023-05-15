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

import models.AgeOver17Dto
import models.AgeOver17Dto.form
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.confirm_age

class ConfirmAgeViewSpec extends BaseViewSpec {

  private val validForm: Form[AgeOver17Dto] = form.bind(Map("ageOver17" -> "true"))

  val viewViaApply: HtmlFormat.Appendable = injected[confirm_age].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[confirm_age].render(
    form = validForm,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[confirm_age].f(
    validForm,
    None
  )(request, messages, appConfig)

  "ConfirmAgeView" when {
    renderViewTest(
      title = "Are you aged 17 or over? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Are you aged 17 or over?"
    )
  }
}
