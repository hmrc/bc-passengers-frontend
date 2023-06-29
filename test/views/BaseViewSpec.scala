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

package views

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import services.ProductTreeService
import util.BaseSpec

import scala.jdk.CollectionConverters.CollectionHasAsScala

trait BaseViewSpec extends BaseSpec {

  val viewViaApply: HtmlFormat.Appendable
  val viewViaRender: HtmlFormat.Appendable
  val viewViaF: HtmlFormat.Appendable

  val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  val appConfig: AppConfig                     = injected[AppConfig]
  val messagesApi: MessagesApi                 = injected[MessagesApi]
  val messages: Messages                       = messagesApi.preferred(request)
  val productTreeService: ProductTreeService   = injected[ProductTreeService]

  def document(html: Html): Document = Jsoup.parse(html.toString())

  def getErrorTitle(doc: Document): String = doc.select(".govuk-error-summary__title").text()

  def getErrorsInSummary(doc: Document): List[(String, String)] = doc
    .select(".govuk-error-summary__list a")
    .asScala
    .map(element => element.attributes.get("href") -> element.text())
    .toList

  def getErrorsInFieldSet(doc: Document): List[String] = doc.select(".govuk-fieldset p").asScala.map(_.text()).toList

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
