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

import models.JourneyData
import play.api.test.FakeRequest
import util.BaseSpec

class LocalContextSpec extends BaseSpec {

  private val localContext: LocalContext = LocalContext(
    request = FakeRequest(),
    sessionId = "sessionId",
    journeyData = Some(JourneyData())
  )

  "LocalContext" when {
    ".getJourneyData" should {
      "return journey data" in {
        localContext.getJourneyData shouldBe JourneyData()
      }

      "throw RuntimeException" in {
        intercept[RuntimeException] {
          localContext.copy(journeyData = None).getJourneyData
        }.getMessage shouldBe "no journey data."
      }
    }
  }
}
