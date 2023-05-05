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

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.{Html, HtmlFormat}
import util.BaseSpec
import views.html.amendments.no_further_amendment

class NoFurtherAmendmentViewSpec extends BaseSpec with Injecting {

  private val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private val appConfig: AppConfig                     = injected[AppConfig]
  private val messagesApi: MessagesApi                 = injected[MessagesApi]
  private val messages: Messages                       = messagesApi.preferred(request)

  private def document(html: Html): Document = Jsoup.parse(html.toString())

  private trait ViewFixture {
    val viewViaApply: HtmlFormat.Appendable = inject[no_further_amendment].apply(backLink = None)(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

    val viewViaRender: HtmlFormat.Appendable = inject[no_further_amendment].render(
      backLink = None,
      request = request,
      messages = messages,
      appConfig = appConfig
    )

    val viewViaF: HtmlFormat.Appendable =
      inject[no_further_amendment].f(None)(request, messages, appConfig)
  }

  "NoFurtherAmendmentView" when {
    ".apply" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaApply
        ).title shouldBe "You can no longer use this service to add goods to your declaration - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(
          viewViaApply
        ).select("h1").text shouldBe "You can no longer use this service to add goods to your declaration"
      }
    }

    ".render" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaRender
        ).title shouldBe "You can no longer use this service to add goods to your declaration - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(
          viewViaRender
        ).select("h1").text shouldBe "You can no longer use this service to add goods to your declaration"
      }
    }

    ".f" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaF
        ).title shouldBe "You can no longer use this service to add goods to your declaration - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(
          viewViaF
        ).select("h1").text shouldBe "You can no longer use this service to add goods to your declaration"
      }
    }
  }
}
