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

import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.no_need_to_use_service_gbni

class NoNeedToUseServiceGBNIViewSpec extends BaseViewSpec {

  val viewViaApply: HtmlFormat.Appendable = injected[no_need_to_use_service_gbni].apply(backLink = None)(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[no_need_to_use_service_gbni].render(
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[no_need_to_use_service_gbni].ref.f(None)(request, messages, appConfig)

  "NoNeedToUseServiceGBNIView" when
    renderViewTest(
      title = "You do not need to use this service - Check tax on goods you bring into the UK - GOV.UK",
      heading = "You do not need to use this service"
    )
}
