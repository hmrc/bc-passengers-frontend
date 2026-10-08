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

package views.purchased_products

import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.purchased_products.declaration_deleted

class DeclarationDeletedViewSpec extends BaseViewSpec {

  val viewViaApply: HtmlFormat.Appendable = injected[declaration_deleted].apply()(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[declaration_deleted].render(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[declaration_deleted].ref.f()(request, messages, appConfig)

  "DeclarationDeletedView" when
    renderViewTest(
      title = "Your declaration has been deleted - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Your declaration has been deleted"
    )
}
