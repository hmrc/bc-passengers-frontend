/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package services


import connectors.Cache
import javax.inject.{Inject, Singleton}
import models.{JourneyData, UserInformation}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserInformationService @Inject() (val cache: Cache) {

  def storeUserInformation(journeyData: JourneyData, userInformation: UserInformation)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.copy(userInformation = Some(userInformation))

    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }
}
