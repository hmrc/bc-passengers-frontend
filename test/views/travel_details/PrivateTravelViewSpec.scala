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

import models.PrivateCraftDto
import models.PrivateCraftDto.form
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.private_travel

class PrivateTravelViewSpec extends BaseViewSpec {

  private val validForm: Form[PrivateCraftDto] = form.bind(Map("privateCraft" -> "true"))

  val viewViaApply: HtmlFormat.Appendable = injected[private_travel].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[private_travel].render(
    form = validForm,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[private_travel].f(
    validForm,
    None
  )(request, messages, appConfig)

  "PrivateTravelView" when {
    renderViewTest(
      title =
        "Are you arriving in the UK by private aircraft or private boat? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Are you arriving in the UK by private aircraft or private boat?"
    )
  }
}
