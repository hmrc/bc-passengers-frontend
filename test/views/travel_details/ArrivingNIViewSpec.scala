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

import forms.ArrivingNIForm.validateForm
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.arriving_ni

class ArrivingNIViewSpec extends BaseViewSpec {

  private val validForm: Form[Boolean] = validateForm().bind(Map("arrivingNI" -> "true"))

  val viewViaApply: HtmlFormat.Appendable = injected[arriving_ni].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[arriving_ni].render(
    form = validForm,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[arriving_ni].f(
    validForm,
    None
  )(request, messages, appConfig)

  "ArrivingNIView" when {
    renderViewTest(
      title = "Is your final destination Northern Ireland? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Is your final destination Northern Ireland?"
    )
  }
}
