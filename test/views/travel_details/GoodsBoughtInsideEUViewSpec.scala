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

import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.goods_bought_inside_eu

class GoodsBoughtInsideEUViewSpec extends BaseViewSpec {

  val viewViaApply: HtmlFormat.Appendable = injected[goods_bought_inside_eu].apply(backLink = None)(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[goods_bought_inside_eu].render(
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[goods_bought_inside_eu].f(None)(request, messages, appConfig)

  "GoodsBoughtInsideEUView" when {
    renderViewTest(
      title = "You do not need to tell us about your goods - Check tax on goods you bring into the UK - GOV.UK",
      heading = "You do not need to tell us about your goods"
    )
  }
}
