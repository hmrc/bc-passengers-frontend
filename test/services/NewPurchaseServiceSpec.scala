/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import controllers.LocalContext
import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
import util.BaseSpec

import scala.util.Random

class NewPurchaseServiceSpec extends BaseSpec {


  trait LocalSetup {

    lazy val s = app.injector.instanceOf[NewPurchaseService]
  }

  "Calling NewPurchaseService.insertPurchases" should {

    "add purchases to existing journey data" in new LocalSetup {

      val ppi = PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("USD"))

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = List(ppi))))

      val modifiedJourneyData = s.insertPurchases(ProductPath("some/item/path"), Some(185.5), Some(100), "FR", None, "EUR", List(12.50), None, new Random(1))(localContext)._1

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        ppi,
        PurchasedProductInstance(ProductPath("some/item/path"), "NAvZuG", Some(185.5), Some(100), Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), None, Some("EUR"), Some(12.50), None, None, Some(false), None, None)
      )
    }

    "set default country and currency in journey data" in new LocalSetup{

      val ppi = PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("USD"))

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = List(ppi))))

      val modifiedJourneyData = s.insertPurchases(ProductPath("some/item/path"), Some(185.5), Some(100), "FR", None, "EUR", List(12.50, 13.60, 14.70), None, new Random(1))(localContext)._1

      modifiedJourneyData.defaultCountry shouldBe Some("FR")
      modifiedJourneyData.defaultCurrency shouldBe Some("EUR")
    }
  }

  "Calling NewPurchaseService.updatePurchase" should {

    "update a purchase in existing journey data" in new LocalSetup {

      val ppis = List(
        PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("USD"), Some(1.69)),
        PurchasedProductInstance(ProductPath("some/item/path"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("USD"), Some(2.99))
      )

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = ppis)))

      val modifiedJourneyData = s.updatePurchase(ProductPath("some/item/path"), "iid1", Some(185.5), Some(100), "FR", None, "EUR", 14.70)(localContext)

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("USD"), Some(1.69), None, None, None, None),
        PurchasedProductInstance(ProductPath("some/item/path"), "iid1", Some(185.5), Some(100), Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)), None, Some("EUR"), Some(14.70), None, None, Some(false), None, None)
      )

    }

    "set default country and currency in journey data" in new LocalSetup{

      val ppis = List(
        PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("USD"), Some(1.69)),
        PurchasedProductInstance(ProductPath("some/item/path"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)), None, Some("USD"), Some(2.99))
      )

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = ppis)))

      val modifiedJourneyData = s.updatePurchase(ProductPath("some/item/path"), "iid1", Some(185.5), Some(100), "BG", None, "AED", 14.70)(localContext)

      modifiedJourneyData.defaultCountry shouldBe Some("BG")
      modifiedJourneyData.defaultCurrency shouldBe Some("AED")
    }
  }

}
