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

package config

import org.scalatest.matchers.should.Matchers
import util.BaseSpec

class AppConfigSpec extends BaseSpec with Matchers {

  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val appConfig: AppConfig = injected[AppConfig]

  "AppConfig" should {
    "read isVapingJourneyEnabled as true when toggle.isVapingJourneyEnabled is true" in {
      when(mockAppConfig.isVapingJourneyEnabled).thenReturn(true)
      mockAppConfig.isVapingJourneyEnabled shouldBe true
    }

    "read isVapingJourneyEnabled as false when toggle.isVapingJourneyEnabled is false" in {
      when(mockAppConfig.isVapingJourneyEnabled).thenReturn(false)
      mockAppConfig.isVapingJourneyEnabled shouldBe false
    }

    "return the correct declareGoodsUrl" in {
      appConfig.declareGoodsUrl shouldBe "https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods"
    }

    "return the correct govUKUrl" in {
      appConfig.govUK shouldBe "https://www.gov.uk"
    }

    "read isWineStillOrSparklingEnabled as true when features.wine-still-or-sparkling is true" in {
      appConfigWith("features.wine-still-or-sparkling" -> true).isWineStillOrSparklingEnabled shouldBe true
    }

    "read isWineStillOrSparklingEnabled as false when features.wine-still-or-sparkling is false" in {
      appConfigWith("features.wine-still-or-sparkling" -> false).isWineStillOrSparklingEnabled shouldBe false
    }
  }
}
