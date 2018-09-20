package services

import javax.inject.{Inject, Singleton}
import models.JourneyData
import play.api.{Configuration, Environment}
import services.http.WsAllMethods
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._


@Singleton
class LocalSessionCache @Inject() (override val http: WsAllMethods, environment: Environment, config: Configuration) extends SessionCache with AppName with ServicesConfig {
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain", "keystore")

  override lazy val mode = environment.mode
  override protected lazy val appNameConfiguration = config
  override protected lazy val runModeConfiguration = config

  def cacheJourneyData(journeyData: JourneyData)(implicit hc: HeaderCarrier) = this.cache("journeyData", journeyData)

  def fetchAndGetJourneyData(implicit hc: HeaderCarrier) = this.fetchAndGetEntry[JourneyData]("journeyData")


}
