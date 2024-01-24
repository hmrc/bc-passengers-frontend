/*
 * Copyright 2024 HM Revenue & Customs
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

package views

import config.AppConfig
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import services.ProductTreeService
import util.BaseSpec

trait BaseViewSpec extends BaseSpec with ViewSpec {

  val viewViaApply: HtmlFormat.Appendable
  val viewViaRender: HtmlFormat.Appendable
  val viewViaF: HtmlFormat.Appendable

  val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  val appConfig: AppConfig                     = injected[AppConfig]
  val messagesApi: MessagesApi                 = injected[MessagesApi]
  val messages: Messages                       = messagesApi.preferred(request)
  val productTreeService: ProductTreeService   = injected[ProductTreeService]

  def renderViewTest(title: String, heading: String): Unit = {
    ".apply" should {
      "display the correct title" in {
        document(
          viewViaApply
        ).title shouldBe title
      }

      "display the correct heading" in {
        document(
          viewViaApply
        ).select("h1").text shouldBe heading
      }
    }

    ".render" should {
      "display the correct title" in {
        document(
          viewViaRender
        ).title shouldBe title
      }

      "display the correct heading" in {
        document(
          viewViaRender
        ).select("h1").text shouldBe heading
      }
    }

    ".f" should {
      "display the correct title" in {
        document(
          viewViaF
        ).title shouldBe title
      }

      "display the correct heading" in {
        document(
          viewViaF
        ).select("h1").text shouldBe heading
      }
    }
  }
}
