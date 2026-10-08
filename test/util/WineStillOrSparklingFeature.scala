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

trait WineStillOrSparklingFeature { self: BaseSpec =>

  protected val wineStillOrSparklingKey: String = "features.wine-still-or-sparkling"

  protected def appConfigToggle(enabled: Boolean): AppConfig = appConfigWith(wineStillOrSparklingKey -> enabled)

  protected lazy val appConfigToggleOn: AppConfig  = appConfigToggle(enabled = true)
  protected lazy val appConfigToggleOff: AppConfig = appConfigToggle(enabled = false)

  protected lazy val toggleOn: Boolean  = appConfigToggleOn.isWineStillOrSparklingEnabled
  protected lazy val toggleOff: Boolean = appConfigToggleOff.isWineStillOrSparklingEnabled
}
