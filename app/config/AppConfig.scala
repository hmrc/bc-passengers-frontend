/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

@Singleton
class AppConfig @Inject() (val runModeConfiguration: Configuration, runMode: RunMode, servicesConfig: ServicesConfig) {

  private def loadConfig(key: String) = runModeConfiguration.get[String](key)

  private val contactHost = runModeConfiguration.getOptional[String]("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = loadConfig("appName")

  lazy val reportAProblemPartialUrl: String = runMode.envPath(s"contact/problem_reports_ajax?service=$contactFormServiceIdentifier")(other = contactHost)
  lazy val reportAProblemNonJSUrl: String = runMode.envPath(s"/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier")(other = contactHost)

  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  lazy val declareGoodsUrl: String = servicesConfig.getString("external.url")

  lazy val maxOtherGoods: Int = runModeConfiguration.getOptional[Int]("max-other-goods-items").getOrElse(50)
  lazy val minPaymentAmount: Int = runModeConfiguration.getOptional[Int]("min-payment-amount").getOrElse(9)
  lazy val paymentLimit: Int = runModeConfiguration.getOptional[Int]("payment-limit").getOrElse(97000)

  // Feature Flags
  lazy val isVatResJourneyEnabled: Boolean = runModeConfiguration.get[Boolean]("features.vat-res")
  lazy val isIrishBorderQuestionEnabled: Boolean = runModeConfiguration.get[Boolean]("features.ireland")
  lazy val isAmendmentsEnabled: Boolean = runModeConfiguration.get[Boolean]("features.amendments")

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage: String => Call = (lang: String) => controllers.routes.LocalLanguageController.switchToLanguage(lang)

  lazy val languageTranslationEnabled: Boolean = runModeConfiguration.get[Seq[String]]("play.i18n.langs").contains("cy")

  val gtmContainer: String = servicesConfig.getString("tracking-consent-frontend.gtm.container")

}
