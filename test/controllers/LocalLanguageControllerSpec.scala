/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import org.mockito.Mockito._
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.language.LanguageUtils
import util.BaseSpec

class LocalLanguageControllerSpec extends BaseSpec {

  private val localLanguageController: LocalLanguageController = new LocalLanguageController(
    languageUtils = mock(classOf[LanguageUtils]),
    controllerComponent = mock(classOf[ControllerComponents]),
    messagesApi = mock(classOf[MessagesApi])
  )

  "LocalLanguageController" when {
    ".languageMap" should {
      "return the correct mapped languages" in
        localLanguageController.languageMap.shouldBe(
          Map(
            "english" -> Lang("en"),
            "cymraeg" -> Lang("cy")
          )
        )
    }

    ".fallbackURL" should {
      "return the show dashboard url as the fallback url" in
        localLanguageController.fallbackURL.shouldBe(controllers.routes.DashboardController.showDashboard.url)
    }
  }
}
