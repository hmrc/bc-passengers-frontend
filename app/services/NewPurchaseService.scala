/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services

import controllers.LocalContext
import javax.inject.Inject
import models.{JourneyData, ProductPath, PurchasedProductInstance}

import scala.util.Random

class NewPurchaseService @Inject() (
  countriesService: CountriesService
) {

  def insertPurchases(path: ProductPath, weightOrVolume: Option[BigDecimal], noOfSticks: Option[Int], countryCode: String, originCountryCode: Option[String], currency: String, costs: List[BigDecimal], rand: Random = Random)(implicit context: LocalContext): (JourneyData,String) = {

    val journeyData = context.journeyData.getOrElse(JourneyData())
    val iid = rand.alphanumeric.filter(_.isLetter).take(6).mkString
    val dataToAdd = for {
      cost <- costs
      country = countriesService.getCountryByCode(countryCode)
      countryEU = countriesService.getCountryByCode(originCountryCode.getOrElse(""))
    } yield PurchasedProductInstance(path, iid, weightOrVolume, noOfSticks, country, countryEU, Some(currency), Some(cost), isCustomPaid = Some(isCustomsExempt(journeyData.euCountryCheck, originCountryCode, journeyData.isUKResident)))

    (journeyData.copy(purchasedProductInstances = journeyData.purchasedProductInstances ++ dataToAdd,
      defaultCountry = Some(countryCode),
      defaultOriginCountry = Some(originCountryCode.getOrElse("")),
      defaultCurrency = Some(currency)),iid)
  }

  def updatePurchase(path: ProductPath, iid: String, weightOrVolume: Option[BigDecimal], noOfSticks: Option[Int], countryCode: String, originCountryCode: Option[String], currency: String, cost: BigDecimal)(implicit context: LocalContext): JourneyData = {
    val journeyData = context.getJourneyData

    val country = countriesService.getCountryByCode(countryCode)
    val countryEU = countriesService.getCountryByCode(originCountryCode.getOrElse(""))
    journeyData.copy(purchasedProductInstances = journeyData.purchasedProductInstances
      .map(ppi => if (ppi.iid == iid) PurchasedProductInstance(path, iid, weightOrVolume, noOfSticks, country, countryEU, Some(currency), Some(cost), ppi.isVatPaid, Some(isCustomsExempt(journeyData.euCountryCheck, originCountryCode, journeyData.isUKResident)),  ppi.isExcisePaid, ppi.isUccRelief) else ppi),
      defaultCountry = Some(countryCode),
      defaultOriginCountry = originCountryCode,
      defaultCurrency = Some(currency))
  }

  private def isCustomsExempt(euCountryCheck: Option[String], originCountryCode: Option[String], isUKResident: Option[Boolean]): Boolean = {
    if (euCountryCheck.contains("euOnly") && countriesService.isInEu(originCountryCode.getOrElse(""))) true
    else if (euCountryCheck.contains("greatBritain")) isUKResident.getOrElse(false)
    else false
  }
}
