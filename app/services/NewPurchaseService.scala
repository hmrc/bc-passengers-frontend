/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services

import connectors.Cache
import controllers.LocalContext
import javax.inject.Inject
import models.{JourneyData, ProductPath, PurchasedProductInstance}

import scala.util.Random

class NewPurchaseService @Inject() (
  cache: Cache,
  countriesService: CountriesService
) {

  def insertPurchases(path: ProductPath, weightOrVolume: Option[BigDecimal], noOfSticks: Option[Int], countryCode: String, currency: String, costs: List[BigDecimal], rand: Random = Random)(implicit context: LocalContext): (JourneyData,String) = {

    val journeyData = context.journeyData.getOrElse(JourneyData())
    val iid = rand.alphanumeric.filter(_.isLetter).take(6).mkString
    val dataToAdd = for {
      cost <- costs
      country = countriesService.getCountryByCode(countryCode)
    } yield PurchasedProductInstance(path, iid, weightOrVolume, noOfSticks, country, Some(currency), Some(cost))

    (journeyData.copy(purchasedProductInstances = journeyData.purchasedProductInstances ++ dataToAdd,
      defaultCountry = Some(countryCode),
      defaultCurrency = Some(currency)),iid)
  }

  def updatePurchase(path: ProductPath, iid: String, weightOrVolume: Option[BigDecimal], noOfSticks: Option[Int], countryCode: String, currency: String, cost: BigDecimal)(implicit context: LocalContext): JourneyData = {
    val journeyData = context.getJourneyData
    val country = countriesService.getCountryByCode(countryCode)
    journeyData.copy(purchasedProductInstances = journeyData.purchasedProductInstances
      .map(ppi => if (ppi.iid == iid) PurchasedProductInstance(path, iid, weightOrVolume, noOfSticks, country, Some(currency), Some(cost), ppi.isVatPaid) else ppi),
      defaultCountry = Some(countryCode),
      defaultCurrency = Some(currency))
  }
}
