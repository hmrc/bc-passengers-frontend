package services

import javax.inject.{Inject, Singleton}
import models.{JourneyData, ProductPath, UserInformation}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserInformationService @Inject() (val localSessionCache: LocalSessionCache) extends UsesJourneyData {

  def storeUserInformation(journeyData: JourneyData, userInformation: UserInformation)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.copy(userInformation = Some(userInformation))

    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }
}
