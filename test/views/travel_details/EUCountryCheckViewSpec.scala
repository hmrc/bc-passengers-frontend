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

import models.EuCountryCheckDto
import models.EuCountryCheckDto.form
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.eu_country_check

class EUCountryCheckViewSpec extends BaseViewSpec {

  private val validForm: Form[EuCountryCheckDto] = form.bind(Map("euCountryCheck" -> "euOnly"))

  val viewViaApply: HtmlFormat.Appendable = injected[eu_country_check].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[eu_country_check].render(
    form = validForm,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[eu_country_check].f(
    validForm,
    None
  )(request, messages, appConfig)

  "EUCountryCheckView" when {
    renderViewTest(
      title = "Where are you bringing in goods from? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Where are you bringing in goods from?"
    )
  }
}
