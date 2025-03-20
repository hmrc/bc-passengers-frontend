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

import models.BringingOverAllowanceDto
import models.BringingOverAllowanceDto.form
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.duty_free_allowance_question_eu

class DutyFreeAllowanceQuestionEUViewSpec extends BaseViewSpec {

  private val validForm: Form[BringingOverAllowanceDto] = form.bind(Map("bringingOverAllowance" -> "true"))

  val viewViaApply: HtmlFormat.Appendable = injected[duty_free_allowance_question_eu].apply(
    form = validForm,
    mixEuRow = true,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[duty_free_allowance_question_eu].render(
    form = validForm,
    mixEuRow = true,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[duty_free_allowance_question_eu].ref.f(
    validForm,
    true,
    None
  )(request, messages, appConfig)

  "DutyFreeAllowanceQuestionEUView" when {
    renderViewTest(
      title = "You may need to declare goods brought in from EU countries",
      heading = "Are you bringing in goods over your allowances?"
    )
  }
}
