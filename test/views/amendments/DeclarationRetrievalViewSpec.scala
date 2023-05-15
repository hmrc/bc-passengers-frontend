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

package views.amendments

import models.DeclarationRetrievalDto
import models.DeclarationRetrievalDto.form
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.amendments.declaration_retrieval

class DeclarationRetrievalViewSpec extends BaseViewSpec {

  private val validForm: Form[DeclarationRetrievalDto] = form().bind(
    Map(
      "lastName"        -> "smith",
      "referenceNumber" -> "XJPR5768524625"
    )
  )

  val viewViaApply: HtmlFormat.Appendable = injected[declaration_retrieval].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[declaration_retrieval].render(
    form = validForm,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[declaration_retrieval].f(
    validForm,
    None
  )(request, messages, appConfig)

  "DeclarationRetrievalView" when {
    renderViewTest(
      title = "Add goods to your previous declaration - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Add goods to your previous declaration"
    )
  }
}
