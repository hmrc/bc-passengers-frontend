package connectors

import javax.inject.{Inject, Singleton}
import models.JourneyData
import play.api.{Configuration, Environment}
import services.http.WsAllMethods
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.bootstrap.config.{AppName, ServicesConfig}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class Cache @Inject()(
  override val http: WsAllMethods,
  environment: Environment,
  config: Configuration,
  servicesConfig: ServicesConfig,
  implicit val ec: ExecutionContext
) extends SessionCache {

  override lazy val defaultSource = AppName.fromConfiguration(config)
  override lazy val baseUri = servicesConfig.baseUrl("cachable.session-cache")
  override lazy val domain = servicesConfig.getConfString("cachable.session-cache.domain", "keystore")

  def store(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[CacheMap] = this.cache("journeyData", journeyData)

  def fetch(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = this.fetchAndGetEntry[JourneyData]("journeyData")
}
