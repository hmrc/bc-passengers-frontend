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

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Cookie, Request}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.{Html, HtmlFormat}
import util.BaseSpec
import views.html.templates.GovukLayoutWrapper

class GovukLayoutWrapperViewSpec extends BaseSpec with Injecting {

  private val appConfig: AppConfig     = injected[AppConfig]
  private val messagesApi: MessagesApi = injected[MessagesApi]

  private def document(html: Html): Document = Jsoup.parse(html.toString())

  private class ViewFixture(request: Request[AnyContentAsEmpty.type] = FakeRequest()) {
    val messages: Messages = messagesApi.preferred(request)

    val viewViaApply: HtmlFormat.Appendable = inject[GovukLayoutWrapper].apply()(
      contentBlock = Html("<h1 class=\"govuk-heading-xl\">page heading</h1>")
    )(request, messages, appConfig)

    val viewViaRender: HtmlFormat.Appendable = inject[GovukLayoutWrapper].render(
      pageTitle = None,
      signOut = true,
      inlineScript = None,
      inlineLinkElem = None,
      backLink = None,
      timeout = true,
      contentBlock = Html("<h1 class=\"govuk-heading-xl\">page heading</h1>"),
      request = request,
      messages = messages,
      appConfig = appConfig
    )

    val viewViaF: HtmlFormat.Appendable =
      inject[GovukLayoutWrapper].f(
        None,
        true,
        None,
        None,
        None,
        true
      )(Html("<h1 class=\"govuk-heading-xl\">page heading</h1>"))(request, messages, appConfig)
  }

  "GovukLayoutWrapperView" when {
    ".apply" should {
      "display the correct heading" in new ViewFixture {
        document(viewViaApply).select("h1").text shouldBe "page heading"
      }

      "by default render the html lang as en" in new ViewFixture {
        document(viewViaApply).select("html").attr("lang") shouldBe "en"
      }

      "render the html lang as cy if language is toggled to Welsh" in new ViewFixture(
        request = FakeRequest().withCookies(Cookie("PLAY_LANG", "cy"))
      ) {
        document(viewViaApply).select("html").attr("lang") shouldBe "cy"
      }
    }

    ".render" should {
      "display the correct heading" in new ViewFixture {
        document(viewViaRender).select("h1").text shouldBe "page heading"
      }
    }

    ".f" should {
      "display the correct heading" in new ViewFixture {
        document(viewViaF).select("h1").text shouldBe "page heading"
      }
    }
  }
}
