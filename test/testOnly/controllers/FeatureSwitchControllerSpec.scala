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

package testOnly.controllers

import config.AppConfig
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.CSRFTokenHelper.*
import play.api.test.Helpers.*
import testOnly.views.html.FeatureSwitchView
import util.BaseSpec

class FeatureSwitchControllerSpec extends BaseSpec {

  private lazy val target =
    new FeatureSwitchController(injected[MessagesControllerComponents], injected[FeatureSwitchView])(
      injected[AppConfig]
    )

  override def afterEach(): Unit = {
    sys.props.remove("features.wine-still-or-sparkling")
    super.afterEach()
  }

  "Calling the .featureSwitch action" should {

    lazy val result = target.featureSwitch(enhancedFakeRequest("GET", "/test-only/feature-switch").withCSRFToken)

    "return 200" in {
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      contentType(result) shouldBe Some("text/html")
    }
  }

  "Calling the .submitFeatureSwitch action" should {

    "set the wine-still-or-sparkling feature and redirect back to the feature switch page" in {
      val result = target.submitFeatureSwitch(
        enhancedFakeRequest("POST", "/test-only/feature-switch")
          .withFormUrlEncodedBody("features.wine-still-or-sparkling" -> "true")
          .withCSRFToken
      )

      status(result)                                             shouldBe Status.SEE_OTHER
      redirectLocation(result)                                   shouldBe Some(routes.FeatureSwitchController.featureSwitch.url)
      injected[AppConfig].features.wineStillOrSparklingEnabled() shouldBe true
    }

    "disable the wine-still-or-sparkling feature when submitted unticked" in {
      val result = target.submitFeatureSwitch(
        enhancedFakeRequest("POST", "/test-only/feature-switch")
          .withFormUrlEncodedBody("features.wine-still-or-sparkling" -> "false")
          .withCSRFToken
      )

      status(result)                                             shouldBe Status.SEE_OTHER
      injected[AppConfig].features.wineStillOrSparklingEnabled() shouldBe false
    }
  }
}
