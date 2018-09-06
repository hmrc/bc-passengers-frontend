package config

import javax.inject.{Inject, Singleton}

import play.api.{Configuration, Environment}
import play.api.Mode.Mode
import uk.gov.hmrc.play.config.ServicesConfig

@Singleton
class AppConfig @Inject() (val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {

  override protected def mode: Mode = environment.mode

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactHost = runModeConfiguration.getString("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = loadConfig("appName")

  lazy val analyticsToken = loadConfig("google-analytics.token")
  lazy val analyticsHost = loadConfig("google-analytics.host")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"


  lazy val betaFeedbackUrl = s"$contactHost/contact/contact-hmrc-unauthenticated?service=$contactFormServiceIdentifier"

}