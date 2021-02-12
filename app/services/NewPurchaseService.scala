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
    } yield PurchasedProductInstance(path, iid, weightOrVolume, noOfSticks, country, countryEU, Some(currency), Some(cost), isCustomPaid = Some(journeyData.isUKResident.getOrElse(false)))
    (journeyData.copy(purchasedProductInstances = journeyData.purchasedProductInstances ++ dataToAdd,
      //Added partial working instance to differenciate between new item and existing in ControllerHelpers.revertWorkingInstance
      workingInstance = Some(PurchasedProductInstance(dataToAdd.head.path,dataToAdd.head.iid)),
      defaultCountry = Some(countryCode),
      defaultCurrency = Some(currency)),iid)
  }

  def updatePurchase(path: ProductPath, iid: String, weightOrVolume: Option[BigDecimal], noOfSticks: Option[Int], countryCode: String, originCountryCode: Option[String], currency: String, cost: BigDecimal)(implicit context: LocalContext): JourneyData = {
    val journeyData = context.getJourneyData
    val country = countriesService.getCountryByCode(countryCode)
    val countryEU = countriesService.getCountryByCode(originCountryCode.getOrElse(""))
    journeyData.copy(purchasedProductInstances = journeyData.purchasedProductInstances
      .map(ppi => {
        if (ppi.iid == iid && originCountryCode.isDefined && countriesService.isInEu(originCountryCode.getOrElse(""))) PurchasedProductInstance(path, iid, weightOrVolume, noOfSticks, country, countryEU, Some(currency), Some(cost), ppi.isVatPaid, isCustomPaid = Some(false), ppi.isExcisePaid, ppi.isUccRelief, ppi.hasEvidence)
        else if (ppi.iid == iid) PurchasedProductInstance(path, iid, weightOrVolume, noOfSticks, country, countryEU, Some(currency), Some(cost), ppi.isVatPaid, isCustomPaid = Some(journeyData.isUKResident.getOrElse(false)), ppi.isExcisePaid, ppi.isUccRelief)
        else ppi
      }),
      defaultCountry = Some(countryCode),
      defaultCurrency = Some(currency))
  }
}
