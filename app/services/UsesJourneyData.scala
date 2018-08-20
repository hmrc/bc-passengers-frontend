package services

import models.{JourneyData, PurchasedProduct, ProductPath}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait UsesJourneyData {

  def localSessionCache: LocalSessionCache

  private def loanJourneyData[A](block: JourneyData => A)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    localSessionCache.fetchAndGetJourneyData.map(jd => block(jd.getOrElse(JourneyData())))

  def loanAndUpdateJourneyData(block: JourneyData => JourneyData)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    loanJourneyData(block).flatMap(cacheJourneyData)


  def cacheJourneyData(journeyData: JourneyData)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    localSessionCache.cacheJourneyData(journeyData)

  def getJourneyData(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = localSessionCache.fetchAndGetJourneyData
}
