package services

import javax.inject.{Inject, Singleton}
import models.JourneyData
import play.api.{Configuration, Environment}
import services.http.WsAllMethods
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.play.bootstrap.config.{AppName, ServicesConfig}



@Singleton
class LocalSessionCache @Inject() (
  override val http: WsAllMethods,
  environment: Environment,
  config: Configuration,
  servicesConfig: ServicesConfig,
  implicit val ec: ExecutionContext
) extends SessionCache {

  override lazy val defaultSource = AppName.fromConfiguration(config)
  override lazy val baseUri = servicesConfig.baseUrl("cachable.session-cache")
  override lazy val domain = servicesConfig.getConfString("cachable.session-cache.domain", "keystore")

  def cacheJourneyData(journeyData: JourneyData)(implicit hc: HeaderCarrier) = this.cache("journeyData", journeyData)

  def fetchAndGetJourneyData(implicit hc: HeaderCarrier) = this.fetchAndGetEntry[JourneyData]("journeyData")
}
