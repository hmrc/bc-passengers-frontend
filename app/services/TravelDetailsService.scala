package services

import connectors.Cache
import javax.inject.Inject
import models.JourneyData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}


class TravelDetailsService @Inject() (
  val cache: Cache,
  implicit val ec: ExecutionContext
) {

  def storeEuCountryCheck(countryChoice: String)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>

        val updatedJourneyData = journeyData match {
          case _ if journeyData.euCountryCheck.contains(countryChoice) =>
            journeyData.copy(euCountryCheck = Some(countryChoice))
          case _ =>
            journeyData.copy(
              euCountryCheck = Some(countryChoice),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = None,
              privateCraft = None,
              ageOver17 = None,
              selectedProducts = Nil,
              purchasedProductInstances = Nil
            )
        }

        cache.store(updatedJourneyData)
      case None =>
        cache.store( JourneyData(euCountryCheck = Some(countryChoice)) )
    }
  }

  def storeVatResCheck(isVatResClaimed: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>

        val updatedJourneyData = journeyData match {
          case _ if journeyData.isVatResClaimed.contains(isVatResClaimed) =>
            journeyData.copy(isVatResClaimed = Some(isVatResClaimed))
          case _ =>
            journeyData.copy(
              isVatResClaimed = Some(isVatResClaimed),
              isBringingDutyFree = None,
              bringingOverAllowance = None,
              privateCraft = None,
              ageOver17 = None,
              selectedProducts = Nil
            )
        }

        cache.store(updatedJourneyData)
      case None =>
        cache.store(JourneyData(isVatResClaimed = Some(isVatResClaimed)))
    }
  }

  def storeBringingDutyFree(isBringingDutyFree: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>

        val updatedJourneyData = journeyData match {
          case _ if journeyData.isBringingDutyFree.contains(isBringingDutyFree) =>
            journeyData.copy(isBringingDutyFree = Some(isBringingDutyFree))
          case _ =>
            journeyData.copy(
              isBringingDutyFree = Some(isBringingDutyFree),
              bringingOverAllowance = None,
              privateCraft = None,
              ageOver17 = None,
              selectedProducts = Nil
            )
        }

        cache.store(updatedJourneyData)
      case None =>
        cache.store(JourneyData(isBringingDutyFree = Some(isBringingDutyFree)))
    }
  }

  def storeBringingOverAllowance(bringingOverAllowance: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>

        val updatedJourneyData = journeyData match {
          case _ if journeyData.bringingOverAllowance.contains(bringingOverAllowance) =>
            journeyData.copy(bringingOverAllowance = Some(bringingOverAllowance))
          case _ =>
            journeyData.copy(
              bringingOverAllowance = Some(bringingOverAllowance),
              privateCraft = None,
              ageOver17 = None,
              selectedProducts = Nil
            )
        }

        cache.store(updatedJourneyData)
      case None =>
        cache.store( JourneyData(bringingOverAllowance = Some(bringingOverAllowance)) )
    }
  }

  def storePrivateCraft(privateCraft: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>

        val updatedJourneyData = journeyData match {
          case _ if journeyData.privateCraft.contains(privateCraft) =>
            journeyData.copy(privateCraft = Some(privateCraft))
          case _ =>
            journeyData.copy(
              privateCraft = Some(privateCraft),
              ageOver17 = None,
              selectedProducts = Nil
            )
        }

        cache.store(updatedJourneyData)
      case None =>
        cache.store( JourneyData(privateCraft = Some(privateCraft)) )
    }
  }

  def storeAgeOver17(ageOver17: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>

        val updatedJourneyData = journeyData match {
          case _ if journeyData.ageOver17.contains(ageOver17) =>
            journeyData.copy(ageOver17 = Some(ageOver17))
          case _ =>
            journeyData.copy(
              ageOver17 = Some(ageOver17),
              selectedProducts = Nil
            )
        }

        cache.store(updatedJourneyData)
      case None =>
        cache.store( JourneyData(ageOver17 = Some(ageOver17)) )
    }
  }

  def storeIrishBorder (irishBorder: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>

        val updatedJourneyData = journeyData match {
          case _ if journeyData.irishBorder.contains(irishBorder) =>
            journeyData.copy(irishBorder = Some(irishBorder))
          case _ =>
            journeyData.copy(
              irishBorder = Some(irishBorder)
            )
        }

        cache.store(updatedJourneyData)
      case None =>
        cache.store( JourneyData(irishBorder = Some(irishBorder)) )
    }
  }
}
