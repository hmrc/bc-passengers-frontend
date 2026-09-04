/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import util.BaseSpec

class ViewUtilsSpec extends BaseSpec {

  private val messagesApi: MessagesApi    = injected[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(FakeRequest())
  private val welshMessages: Messages     = messagesApi.preferred(Seq(Lang("cy")))

  private val toggleOn: Boolean  = appConfigWith(
    "features.wine-still-or-sparkling" -> true
  ).isWineStillOrSparklingEnabled
  private val toggleOff: Boolean =
    appConfigWith("features.wine-still-or-sparkling" -> false).isWineStillOrSparklingEnabled

  "ViewUtils.toggledMessage" when {

    "the wine-still-or-sparkling toggle is ON" should {

      "return the still-or-sparkling copy" in {
        ViewUtils.toggledMessage("text.gb.allowance.alc_2", toggleOn) shouldBe
          messages("text.gb.allowance.alc_2.still-or-sparkling")
        ViewUtils.toggledMessage("text.gb.allowance.alc_1", toggleOn) shouldBe
          messages("text.gb.allowance.alc_1.still-or-sparkling")
      }

      "return the still-or-sparkling copy in Welsh" in {
        ViewUtils.toggledMessage("text.gb.allowance.alc_2", toggleOn)(welshMessages) shouldBe
          welshMessages("text.gb.allowance.alc_2.still-or-sparkling")
      }
    }

    "the wine-still-or-sparkling toggle is OFF" should {

      "return the original copy" in {
        ViewUtils.toggledMessage("text.gb.allowance.alc_2", toggleOff) shouldBe messages("text.gb.allowance.alc_2")
        ViewUtils.toggledMessage("text.gb.allowance.alc_1", toggleOff) shouldBe messages("text.gb.allowance.alc_1")
      }
    }
  }
}
