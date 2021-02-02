/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services

import connectors.Cache

import javax.inject.Inject
import models.{JourneyData, PreviousDeclarationDetails}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


class TravelDetailsService @Inject() (
  val cache: Cache,
  implicit val ec: ExecutionContext
) {

  def storeEuCountryCheck(journeyData: Option[JourneyData])(countryChoice: String)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case  Some(jd) if !jd.euCountryCheck.contains(countryChoice) =>

        cache.storeJourneyData(jd.copy(
          euCountryCheck = Some(countryChoice),
          isUKVatPaid = None,
          isUKVatExcisePaid = None,
          isUKResident = None,
          isUccRelief = None,
          isVatResClaimed = None,
          isBringingDutyFree = None,
          bringingOverAllowance = None,
          privateCraft = None,
          ageOver17 = None,
          purchasedProductInstances = Nil,
          defaultCountry = None,
          defaultOriginCountry = None,
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

  def storeUKVatPaid(journeyData: Option[JourneyData])(isUKVatPaid: Boolean)
                     (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.isUKVatPaid.contains(isUKVatPaid) =>

        cache.storeJourneyData(journeyData.copy(
          isUKVatPaid = Some(isUKVatPaid)
        ))

      case None =>
        cache.storeJourneyData( JourneyData(isUKVatPaid = Some(isUKVatPaid)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storeUKExcisePaid(journeyData: Option[JourneyData])(isUKVatExcisePaid: Boolean)
                    (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.isUKVatExcisePaid.contains(isUKVatExcisePaid) =>

        cache.storeJourneyData(journeyData.copy(
          isUKVatExcisePaid = Some(isUKVatExcisePaid)
        ))

      case None =>
        cache.storeJourneyData( JourneyData(isUKVatExcisePaid = Some(isUKVatExcisePaid)) )

      case _ =>
        Future.successful( journeyData )
    }
  }
  def storeUKResident(journeyData: Option[JourneyData])(isUKResident: Boolean)
                     (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.isUKResident.contains(isUKResident) =>

        val resetJourneyData = JourneyData(
          isUKResident = Some(isUKResident),
          euCountryCheck = journeyData.euCountryCheck,
          arrivingNICheck = journeyData.arrivingNICheck
        )

        cache.storeJourneyData(resetJourneyData)

      case None =>
        cache.storeJourneyData( JourneyData(isUKResident = Some(isUKResident)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storeUccRelief(journeyData: Option[JourneyData])(isUccRelief: Boolean)
                     (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.isUccRelief.contains(isUccRelief) =>

        cache.storeJourneyData(journeyData.copy(
          isUccRelief = Some(isUccRelief)
        ))

      case None =>
        cache.storeJourneyData( JourneyData(isUccRelief = Some(isUccRelief)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storePrevDeclaration(journeyData: Option[JourneyData])(prevDeclaration: Boolean)
                    (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.prevDeclaration.contains(prevDeclaration) =>

        cache.storeJourneyData(journeyData.copy(
          prevDeclaration = Some(prevDeclaration),
          previousDeclarationDetails = None
        ))

      case None =>
        cache.storeJourneyData( JourneyData(
          prevDeclaration = Some(prevDeclaration)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storePrevDeclarationDetails(journeyData: Option[JourneyData])(previousDeclarationDetails: PreviousDeclarationDetails)
                          (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {
    journeyData match {
      case Some(journeyData) =>
        cache.storeJourneyData(journeyData.copy(
          previousDeclarationDetails = Some(previousDeclarationDetails)
        ))
      case None =>
        cache.storeJourneyData( JourneyData(previousDeclarationDetails = Some(previousDeclarationDetails)) )

      case _ =>
        Future.successful( journeyData )
    }
  }
}
