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

package views.purchased_products

import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.purchased_products.limit_exceed

class LimitExceedViewSpec extends BaseViewSpec {

  val viewViaApply: HtmlFormat.Appendable = injected[limit_exceed].apply(
    itemType = "label.tobacco.cigars",
    headerLabel = "heading.l-cigar.limit-exceeded"
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[limit_exceed].render(
    itemType = "label.tobacco.cigars",
    headerLabel = "heading.l-cigar.limit-exceeded",
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[limit_exceed].f(
    "label.tobacco.cigars",
    "heading.l-cigar.limit-exceeded"
  )(request, messages, appConfig)

  "LimitExceedView" when {
    renderViewTest(
      title =
        "You cannot use this service to declare more than 200 cigars - Check tax on goods you bring into the UK - GOV.UK",
      heading = "You cannot use this service to declare more than 200 cigars"
    )
  }
}
