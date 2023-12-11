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

package views.templates

import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import views.BaseViewSpec
import views.html.templates.GovukLayoutWrapper

class GovukLayoutWrapperViewSpec extends BaseViewSpec {

  val viewViaApply: HtmlFormat.Appendable = injected[GovukLayoutWrapper].apply(
    pageTitle = Some("page title")
  )(
    contentBlock = Html("<h1>page heading</h1>")
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[GovukLayoutWrapper].render(
    pageTitle = Some("page title"),
    signOut = true,
    inlineScript = None,
    inlineLinkElem = None,
    customBackLink = true,
    backLink = None,
    timeout = true,
    contentBlock = Html("<h1>page heading</h1>"),
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[GovukLayoutWrapper].f(
    Some("page title"),
    true,
    None,
    None,
    true,
    None,
    true
  )(Html("<h1>page heading</h1>"))(request, messages, appConfig)

  "GovukLayoutWrapperView" when {
    renderViewTest(
      title = "page title",
      heading = "page heading"
    )

    "default language" should {
      "render the html lang as en" in {
        document(viewViaApply).select("html").attr("lang") shouldBe "en"
      }
    }

    "language is toggled to Welsh" should {
      "render the html lang as cy" in {
        val messages: Messages                  = injected[MessagesApi].preferred(
          request = FakeRequest().withCookies(Cookie("PLAY_LANG", "cy"))
        )
        val viewViaApply: HtmlFormat.Appendable = injected[GovukLayoutWrapper].apply()(
          contentBlock = Html("")
        )(
          request = request,
          messages = messages,
          appConfig = appConfig
        )

        document(viewViaApply).select("html").attr("lang") shouldBe "cy"
      }
    }
  }
}
