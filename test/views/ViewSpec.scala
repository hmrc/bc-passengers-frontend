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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html

import scala.jdk.CollectionConverters.IterableHasAsScala

trait ViewSpec {

  def document(html: Html): Document = Jsoup.parse(html.toString())

  def getErrorTitle(doc: Document): String = doc.select(".govuk-error-summary__title").text()

  def getErrorsInSummary(doc: Document): List[(String, String)] = doc
    .select(".govuk-error-summary__list a")
    .asScala
    .map(element => element.attributes.get("href") -> element.text())
    .toList

  def getErrorsInFieldSet(doc: Document): List[String] = doc.select(".govuk-error-message").asScala.map(_.text()).toList

}
