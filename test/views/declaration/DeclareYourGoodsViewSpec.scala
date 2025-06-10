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

package views.declaration

import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.declaration.declare_your_goods

class DeclareYourGoodsViewSpec extends BaseViewSpec {

  val viewViaApply: HtmlFormat.Appendable = injected[declare_your_goods].apply(
    isAmendment = true,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[declare_your_goods].render(
    isAmendment = true,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[declare_your_goods].ref.f(
    true,
    None
  )(request, messages, appConfig)

  "DeclareYourGoodsView" when
    renderViewTest(
      title = "Amend your declaration - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Amend your declaration"
    )
}
