/*
 * Copyright 2025 HM Revenue & Customs
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
import models.*
import util.BaseSpec

import scala.util.Random

class NewPurchaseServiceSpec extends BaseSpec {

  trait LocalSetup {
    val noOfSticks: Int               = 100
    val ppi: PurchasedProductInstance = PurchasedProductInstance(
      path = ProductPath("some/item/path"),
      iid = "iid0",
      country = Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
      currency = Some("USD")
    )

    val ppis: List[PurchasedProductInstance] = List(
      PurchasedProductInstance(
        path = ProductPath("some/item/path"),
        iid = "iid0",
        country = Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
        currency = Some("USD"),
        cost = Some(1.69)
      ),
      PurchasedProductInstance(
        path = ProductPath("some/item/path"),
        iid = "iid1",
        country = Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
        currency = Some("USD"),
        cost = Some(2.99)
      )
    )

    lazy val newPurchaseService: NewPurchaseService = app.injector.instanceOf[NewPurchaseService]
  }

  "Calling NewPurchaseService.insertPurchases" should {
    "add purchases when no journey data exists" in new LocalSetup {
      val localContext: LocalContext = LocalContext(
        request = enhancedFakeRequest("GET", "anything"),
        sessionId = "123",
        journeyData = None
      )

      val modifiedJourneyData: JourneyData = newPurchaseService
        .insertPurchases(
          path = ProductPath("some/item/path"),
          weightOrVolume = Some(185.5),
          noOfSticks = Some(noOfSticks),
          countryCode = "FR",
          originCountryCode = None,
          currency = "EUR",
          costs = List(12.50),
          searchTerm = None,
          rand = new Random(1)
        )(localContext)
        ._1

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        PurchasedProductInstance(
          path = ProductPath("some/item/path"),
          iid = "NAvZuG",
          weightOrVolume = Some(185.5),
          noOfSticks = Some(noOfSticks),
          country = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
          originCountry = None,
          currency = Some("EUR"),
          cost = Some(12.50),
          isCustomPaid = Some(false)
        )
      )
    }

    "add purchases to existing journey data" in new LocalSetup {
      val localContext: LocalContext = LocalContext(
        request = enhancedFakeRequest("GET", "anything"),
        sessionId = "123",
        journeyData = Some(JourneyData(purchasedProductInstances = List(ppi)))
      )

      val modifiedJourneyData: JourneyData = newPurchaseService
        .insertPurchases(
          path = ProductPath("some/item/path"),
          weightOrVolume = Some(185.5),
          noOfSticks = Some(noOfSticks),
          countryCode = "FR",
          originCountryCode = None,
          currency = "EUR",
          costs = List(12.50),
          searchTerm = None,
          rand = new Random(1)
        )(localContext)
        ._1

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        ppi,
        PurchasedProductInstance(
          path = ProductPath("some/item/path"),
          iid = "NAvZuG",
          weightOrVolume = Some(185.5),
          noOfSticks = Some(noOfSticks),
          country = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
          originCountry = None,
          currency = Some("EUR"),
          cost = Some(12.50),
          isCustomPaid = Some(false)
        )
      )
    }

    "set default country and currency in journey data" in new LocalSetup {
      val localContext: LocalContext = LocalContext(
        request = enhancedFakeRequest("GET", "anything"),
        sessionId = "123",
        journeyData = Some(JourneyData(purchasedProductInstances = List(ppi)))
      )

      val modifiedJourneyData: JourneyData = newPurchaseService
        .insertPurchases(
          path = ProductPath("some/item/path"),
          weightOrVolume = Some(185.5),
          noOfSticks = Some(noOfSticks),
          countryCode = "FR",
          originCountryCode = None,
          currency = "EUR",
          costs = List(12.50, 13.60, 14.70),
          searchTerm = None,
          rand = new Random(1)
        )(localContext)
        ._1

      modifiedJourneyData.defaultCountry  shouldBe Some("FR")
      modifiedJourneyData.defaultCurrency shouldBe Some("EUR")
    }
  }

  "Calling NewPurchaseService.updatePurchase" should {
    "update a purchase with no origin country code specified in existing journey data" in new LocalSetup {
      val localContext: LocalContext = LocalContext(
        request = enhancedFakeRequest("GET", "anything"),
        sessionId = "123",
        journeyData = Some(JourneyData(purchasedProductInstances = ppis))
      )

      val modifiedJourneyData: JourneyData = newPurchaseService.updatePurchase(
        path = ProductPath("some/item/path"),
        iid = "iid1",
        weightOrVolume = Some(185.5),
        noOfSticks = Some(noOfSticks),
        countryCode = "FR",
        originCountryCode = None,
        currency = "EUR",
        cost = 14.70
      )(localContext)

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        PurchasedProductInstance(
          path = ProductPath("some/item/path"),
          iid = "iid0",
          country = Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          currency = Some("USD"),
          cost = Some(1.69)
        ),
        PurchasedProductInstance(
          path = ProductPath("some/item/path"),
          iid = "iid1",
          weightOrVolume = Some(185.5),
          noOfSticks = Some(noOfSticks),
          country = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
          currency = Some("EUR"),
          cost = Some(14.70),
          isCustomPaid = Some(false)
        )
      )
    }

    "update a purchase with origin country code specified in existing journey data" in new LocalSetup {
      val localContext: LocalContext = LocalContext(
        request = enhancedFakeRequest("GET", "anything"),
        sessionId = "123",
        journeyData = Some(JourneyData(purchasedProductInstances = ppis))
      )

      val modifiedJourneyData: JourneyData = newPurchaseService.updatePurchase(
        path = ProductPath("some/item/path"),
        iid = "iid1",
        weightOrVolume = Some(185.5),
        noOfSticks = Some(noOfSticks),
        countryCode = "FR",
        originCountryCode = Some("BE"),
        currency = "EUR",
        cost = 14.70
      )(localContext)

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        PurchasedProductInstance(
          path = ProductPath("some/item/path"),
          iid = "iid0",
          country = Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
          currency = Some("USD"),
          cost = Some(1.69)
        ),
        PurchasedProductInstance(
          path = ProductPath("some/item/path"),
          iid = "iid1",
          weightOrVolume = Some(185.5),
          noOfSticks = Some(noOfSticks),
          country = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)),
          originCountry = Some(Country("BE", "title.belgium", "BE", isEu = true, isCountry = true, Nil)),
          currency = Some("EUR"),
          cost = Some(14.70),
          isCustomPaid = Some(false)
        )
      )
    }

    "set default country and currency in journey data" in new LocalSetup {
      val localContext: LocalContext = LocalContext(
        request = enhancedFakeRequest("GET", "anything"),
        sessionId = "123",
        journeyData = Some(JourneyData(purchasedProductInstances = ppis))
      )

      val modifiedJourneyData: JourneyData = newPurchaseService.updatePurchase(
        path = ProductPath("some/item/path"),
        iid = "iid1",
        weightOrVolume = Some(185.5),
        noOfSticks = Some(noOfSticks),
        countryCode = "BG",
        originCountryCode = None,
        currency = "AED",
        cost = 14.70
      )(localContext)

      modifiedJourneyData.defaultCountry  shouldBe Some("BG")
      modifiedJourneyData.defaultCurrency shouldBe Some("AED")
    }
  }
}
