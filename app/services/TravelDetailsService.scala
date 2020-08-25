package services

import connectors.Cache
import javax.inject.Inject
import models.JourneyData
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


class TravelDetailsService @Inject() (
  val cache: Cache,
  implicit val ec: ExecutionContext
) {

  def storeEuCountryCheck(journeyData: Option[JourneyData])(countryChoice: String)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(jd) if !jd.euCountryCheck.contains(countryChoice) =>

        cache.storeJourneyData(jd.copy(
          euCountryCheck = Some(countryChoice),
          isVatResClaimed = None,
          isBringingDutyFree = None,
          bringingOverAllowance = None,
          privateCraft = None,
          ageOver17 = None,
          purchasedProductInstances = Nil,
          defaultCountry = None,
          defaultCurrency = None
        ))

      case None =>
        cache.storeJourneyData( JourneyData(euCountryCheck = Some(countryChoice)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storeVatResCheck(journeyData: Option[JourneyData])(isVatResClaimed: Boolean)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.isVatResClaimed.contains(isVatResClaimed) =>

        cache.storeJourneyData(journeyData.copy(
          isVatResClaimed = Some(isVatResClaimed),
          isBringingDutyFree = None,
          bringingOverAllowance = None,
          privateCraft = None,
          ageOver17 = None
        ))

      case None =>
        cache.storeJourneyData( JourneyData(isVatResClaimed = Some(isVatResClaimed)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storeBringingDutyFree(journeyData: Option[JourneyData])(isBringingDutyFree: Boolean)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.isBringingDutyFree.contains(isBringingDutyFree) =>

        cache.storeJourneyData(journeyData.copy(
          isBringingDutyFree = Some(isBringingDutyFree),
          bringingOverAllowance = None,
          privateCraft = None,
          ageOver17 = None
        ))

      case None =>
        cache.storeJourneyData(JourneyData(isBringingDutyFree = Some(isBringingDutyFree)))

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storeBringingOverAllowance(journeyData: Option[JourneyData])(bringingOverAllowance: Boolean)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.bringingOverAllowance.contains(bringingOverAllowance) =>

        cache.storeJourneyData(journeyData.copy(
          bringingOverAllowance = Some(bringingOverAllowance),
          privateCraft = None,
          ageOver17 = None
        ))

      case None =>
        cache.storeJourneyData( JourneyData(bringingOverAllowance = Some(bringingOverAllowance)))

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storePrivateCraft(journeyData: Option[JourneyData])(privateCraft: Boolean)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.privateCraft.contains(privateCraft) =>

        cache.storeJourneyData(journeyData.copy(
          privateCraft = Some(privateCraft),
          ageOver17 = None
        ))

      case None =>
        cache.storeJourneyData( JourneyData(privateCraft = Some(privateCraft)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storeAgeOver17(journeyData: Option[JourneyData])(ageOver17: Boolean)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.ageOver17.contains(ageOver17) =>

        cache.storeJourneyData(journeyData.copy(
          ageOver17 = Some(ageOver17)
        ))

      case None =>
        cache.storeJourneyData( JourneyData(ageOver17 = Some(ageOver17)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storeIrishBorder(journeyData: Option[JourneyData])(irishBorder: Boolean)
                      (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.irishBorder.contains(irishBorder) =>

        cache.storeJourneyData(journeyData.copy(
          irishBorder = Some(irishBorder)
        ))

      case None =>
        cache.storeJourneyData( JourneyData(irishBorder = Some(irishBorder)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storeArrivingNI(journeyData: Option[JourneyData])(arrivingNICheck: Boolean)
                     (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.arrivingNICheck.contains(arrivingNICheck) =>

        cache.storeJourneyData(journeyData.copy(
          arrivingNICheck = Some(arrivingNICheck)
        ))

      case None =>
        cache.storeJourneyData( JourneyData(arrivingNICheck = Some(arrivingNICheck)) )

      case _ =>
        Future.successful( journeyData )
    }
  }
}
