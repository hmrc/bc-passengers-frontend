package config

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

@Singleton
class AppConfig @Inject() (val runModeConfiguration: Configuration, environment: Environment, runMode: RunMode, servicesConfig: ServicesConfig) {

  private def loadConfig(key: String) = runModeConfiguration.get[String](key)

  private val contactHost = runModeConfiguration.getOptional[String]("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = loadConfig("appName")

  lazy val googleTagManagerId: String = loadConfig("google-tag-manager.id")
  lazy val reportAProblemPartialUrl = runMode.envPath(s"contact/problem_reports_ajax?service=$contactFormServiceIdentifier")(other = contactHost)
  lazy val reportAProblemNonJSUrl = runMode.envPath(s"/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier")(other = contactHost)

  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

  lazy val maxOtherGoods: Int = runModeConfiguration.getOptional[Int]("max-other-goods-items").getOrElse(50)
  lazy val minPaymentAmount: Int = runModeConfiguration.getOptional[Int]("min-payment-amount").getOrElse(9)
  lazy val paymentLimit: Int = runModeConfiguration.getOptional[Int]("payment-limit").getOrElse(97000)

  // Feature Flags
  lazy val isVatResJourneyEnabled: Boolean = runModeConfiguration.get[Boolean]("features.vat-res")
  lazy val isIrishBorderQuestionEnabled: Boolean = runModeConfiguration.get[Boolean]("features.ireland")
}
