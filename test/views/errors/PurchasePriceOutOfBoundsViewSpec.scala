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

package views.errors

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.{Html, HtmlFormat}
import util.BaseSpec
import views.html.errors.purchase_price_out_of_bounds

class PurchasePriceOutOfBoundsViewSpec extends BaseSpec with Injecting {

  private val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private val appConfig: AppConfig                     = injected[AppConfig]
  private val messagesApi: MessagesApi                 = injected[MessagesApi]
  private val messages: Messages                       = messagesApi.preferred(request)

  private def document(html: Html): Document = Jsoup.parse(html.toString())

  private trait ViewFixture {
    val viewViaApply: HtmlFormat.Appendable = inject[purchase_price_out_of_bounds].apply()(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

    val viewViaRender: HtmlFormat.Appendable = inject[purchase_price_out_of_bounds].render(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

    val viewViaF: HtmlFormat.Appendable =
      inject[purchase_price_out_of_bounds].f()(request, messages, appConfig)
  }

  "PurchasePriceOutOfBoundsView" when {
    ".apply" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaApply
        ).title shouldBe "You cannot use this service - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(viewViaApply).select("h1").text shouldBe "You cannot use this service"
      }
    }

    ".render" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaRender
        ).title shouldBe "You cannot use this service - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(viewViaRender).select("h1").text shouldBe "You cannot use this service"
      }
    }

    ".f" should {
      "display the correct title" in new ViewFixture {
        document(
          viewViaF
        ).title shouldBe "You cannot use this service - Check tax on goods you bring into the UK - GOV.UK"
      }

      "display the correct heading" in new ViewFixture {
        document(viewViaF).select("h1").text shouldBe "You cannot use this service"
      }
    }
  }
}
