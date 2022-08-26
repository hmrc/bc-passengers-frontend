/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (val runModeConfiguration: Configuration, servicesConfig: ServicesConfig) {

  private def loadConfig(key: String) = runModeConfiguration.get[String](key)

  private val contactHost                  = runModeConfiguration.getOptional[String]("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = loadConfig("appName")
  lazy val govUK: String                   = servicesConfig.getString("urls.govUK")

  lazy val betaFeedbackUrl         = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  lazy val declareGoodsUrl: String = servicesConfig.getString("external.url")

  lazy val maxOtherGoods: Int    = runModeConfiguration.getOptional[Int]("max-other-goods-items").getOrElse(50)
  lazy val minPaymentAmount: Int = runModeConfiguration.getOptional[Int]("min-payment-amount").getOrElse(9)
  lazy val paymentLimit: Int     = runModeConfiguration.getOptional[Int]("payment-limit").getOrElse(97000)

  // Feature Flags
  lazy val isVatResJourneyEnabled: Boolean       = runModeConfiguration.get[Boolean]("features.vat-res")
  lazy val isIrishBorderQuestionEnabled: Boolean = runModeConfiguration.get[Boolean]("features.ireland")
  lazy val isAmendmentsEnabled: Boolean          = runModeConfiguration.get[Boolean]("features.amendments")
  lazy val timeout: Int                          = servicesConfig.getInt("timeout.timeout")
  lazy val countdown: Int                        = servicesConfig.getInt("timeout.countdown")

  def languageMap: Map[String, Lang] = Map("english" -> Lang("en"), "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage: String => Call = (lang: String) =>
    controllers.routes.LocalLanguageController.switchToLanguage(lang)

  lazy val languageTranslationEnabled: Boolean = runModeConfiguration.get[Seq[String]]("play.i18n.langs").contains("cy")

  val gtmContainer: String = servicesConfig.getString("tracking-consent-frontend.gtm.container")

}
