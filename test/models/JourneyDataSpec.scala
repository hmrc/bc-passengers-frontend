/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import util.BaseSpec

import scala.util.Random

class JourneyDataSpec extends BaseSpec {

  private val countryEgypt = Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)

  "Calling JourneyData.getOrCreatePurchasedProductInstance" should {
    "return the specified product from the journey data if it exists" in {
      val journeyData = JourneyData(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid4", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("alcohol/cider"), "iid3", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("tobacco/cigars"), "iid3", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid3", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("other-goods/childrens"), "iid0", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("other-goods/childrens"), "iid1", Some(1.23456), None, Some(countryEgypt), None, Some("USD"), Some(BigDecimal(10.567)))))

      journeyData.getOrCreatePurchasedProductInstance(ProductPath("other-goods/childrens"), "iid1") shouldEqual PurchasedProductInstance(ProductPath("other-goods/childrens"), "iid1", Some(1.23456), None, Some(countryEgypt), None, Some("USD"), Some(BigDecimal(10.567)))
    }

    "return a new PurchasedProductInstance if the specified one does not exist" in {
      val journeyData = JourneyData(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid4", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("alcohol/cider"), "iid3", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("tobacco/cigars"), "iid3", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid3", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("other-goods/childrens"), "iid0", Some(1.54332), None, Some(countryEgypt), None, Some("AUD"), Some(BigDecimal(10.234))),
        PurchasedProductInstance(ProductPath("other-goods/childrens"), "iid1", Some(1.23456), None, Some(countryEgypt), None, Some("USD"), Some(BigDecimal(10.567)))))

      journeyData.getOrCreatePurchasedProductInstance(ProductPath("alcohol/sparkling"), "iid5") shouldEqual PurchasedProductInstance(ProductPath("alcohol/sparkling"), "iid5", None, None, None, None)
    }
  }
}
