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

  lazy val analyticsToken = loadConfig("google-analytics.token")
  lazy val analyticsHost = loadConfig("google-analytics.host")
  lazy val googleTagManagerId: String = loadConfig("google-tag-manager.id")
  lazy val reportAProblemPartialUrl = runMode.envPath(s"contact/problem_reports_ajax?service=$contactFormServiceIdentifier")(other = contactHost)
  lazy val reportAProblemNonJSUrl = runMode.envPath(s"/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier")(other = contactHost)

  lazy val betaFeedbackUrl = s"$contactHost/contact/contact-hmrc-unauthenticated?service=$contactFormServiceIdentifier"

}