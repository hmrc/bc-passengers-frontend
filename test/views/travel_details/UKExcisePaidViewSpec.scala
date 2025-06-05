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

import forms.UKExcisePaidForm.form
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.ukexcise_paid

class UKExcisePaidViewSpec extends BaseViewSpec {

  private val validForm: Form[Boolean] = form.bind(Map("isUKVatExcisePaid" -> "true"))

  val viewViaApply: HtmlFormat.Appendable = injected[ukexcise_paid].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[ukexcise_paid].render(
    form = validForm,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[ukexcise_paid].ref.f(
    validForm,
    None
  )(request, messages, appConfig)

  "UKExcisePaidView" when
    renderViewTest(
      title =
        "Did you pay both UK VAT and excise duty when buying all of your goods? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Did you pay both UK VAT and excise duty when buying all of your goods?"
    )
}
