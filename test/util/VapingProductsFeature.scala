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

package util

import config.AppConfig

trait VapingProductsFeature { self: BaseSpec =>

  protected val vapingProductsFeatureKey: String = "toggle.isVapingJourneyEnabled"

  protected def appConfigVpToggle(enabled: Boolean): AppConfig = appConfigWith(vapingProductsFeatureKey -> enabled)

  protected lazy val appConfigVpToggleOn: AppConfig  = appConfigVpToggle(enabled = true)
  protected lazy val appConfigVpToggleOff: AppConfig = appConfigVpToggle(enabled = false)

  protected lazy val vpToggleOn: Boolean  = appConfigVpToggleOn.isVapingJourneyEnabled
  protected lazy val vpToggleOff: Boolean = appConfigVpToggleOff.isVapingJourneyEnabled
}
