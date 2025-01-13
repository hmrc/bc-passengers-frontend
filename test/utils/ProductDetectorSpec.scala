/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import models.*
import util.*

class ProductDetectorSpec extends ProductDetector with BaseSpec {

  private def purchasedProductInstance(path: String, iid: String) = PurchasedProductInstance(
    path = ProductPath(path),
    iid = iid,
    weightOrVolume = Some(10.00),
    country = Some(
      Country(
        code = "US",
        countryName = "title.united_states_of_america",
        alphaTwoCode = "US",
        isEu = false,
        isCountry = true,
        countrySynonyms = List("USA", "US", "American")
      )
    ),
    currency = Some("GBP"),
    cost = Some(100.00),
    isVatPaid = Some(false),
    isCustomPaid = Some(false),
    isExcisePaid = Some(false)
  )

  private val purchasedProductInstances: List[PurchasedProductInstance] = List(
    purchasedProductInstance("alcohol/beer", "ZkCSUz"),
    purchasedProductInstance("alcohol/wine", "uRgxRU"),
    purchasedProductInstance("tobacco/chewing-tobacco", "xBgxTU")
  )

  private val oldPurchaseProductInstances: List[PurchasedProductInstance] = List(
    purchasedProductInstance("alcohol/cider/sparkling-cider-up", "ZjCSUz"),
    purchasedProductInstance("alcohol/other", "uFgxRU"),
    purchasedProductInstance("tobacco/rolling-tobacco", "xHgxTU")
  )

  private val declarationResponse: DeclarationResponse = DeclarationResponse(
    calculation = Calculation(
      excise = "306.40",
      customs = "8.80",
      vat = "28.80",
      allTax = "344.00"
    ),
    liabilityDetails = LiabilityDetails(
      totalExciseGBP = "306.40",
      totalCustomsGBP = "8.80",
      totalVATGBP = "28.80",
      grandTotalGBP = "344.00"
    ),
    oldPurchaseProductInstances = oldPurchaseProductInstances
  )

  private def journeyData(declarationResponse: Option[DeclarationResponse]) = JourneyData(
    prevDeclaration = Some(false),
    euCountryCheck = Some("greatBritain"),
    arrivingNICheck = Some(true),
    bringingOverAllowance = Some(true),
    isUKResident = Some(false),
    privateCraft = Some(false),
    ageOver17 = Some(true),
    purchasedProductInstances = purchasedProductInstances,
    declarationResponse = declarationResponse
  )

  "ProductDetector" when {
    ".checkAlcoholProductExists" should {
      "return true when an alcohol product exists" in {
        checkAlcoholProductExists(
          productToken = "beer",
          wineOrSparklingExists = checkProductExists(journeyData(None), "wine"),
          ciderOrOtherAlcoholExists =
            checkProductExists(journeyData(None), "cider") || checkProductExists(journeyData(None), "other"),
          beerOrSpiritExists = checkProductExists(journeyData(None), "beer")
        ) shouldBe true

        checkAlcoholProductExists(
          productToken = "wine",
          wineOrSparklingExists = checkProductExists(journeyData(None), "wine"),
          ciderOrOtherAlcoholExists =
            checkProductExists(journeyData(None), "cider") || checkProductExists(journeyData(None), "other"),
          beerOrSpiritExists = checkProductExists(journeyData(None), "beer")
        ) shouldBe true
      }

      "return false when an alcohol product does not exist" in {
        checkAlcoholProductExists(
          productToken = "other",
          wineOrSparklingExists = checkProductExists(journeyData(None), "wine"),
          ciderOrOtherAlcoholExists =
            checkProductExists(journeyData(None), "cider") || checkProductExists(journeyData(None), "other"),
          beerOrSpiritExists = checkProductExists(journeyData(None), "beer")
        ) shouldBe false

        checkAlcoholProductExists(
          productToken = "spirits",
          wineOrSparklingExists = checkProductExists(journeyData(None), "wine"),
          ciderOrOtherAlcoholExists =
            checkProductExists(journeyData(None), "cider") || checkProductExists(journeyData(None), "other"),
          beerOrSpiritExists = checkProductExists(journeyData(None), "spirits")
        ) shouldBe false
      }
    }

    ".checkProductExists" when {
      "declaration response does not exist in journey data" should {
        "return true when a product exists" in {
          checkProductExists(journeyData(None), "alcohol/beer")            shouldBe true
          checkProductExists(journeyData(None), "alcohol/wine")            shouldBe true
          checkProductExists(journeyData(None), "tobacco/chewing-tobacco") shouldBe true
        }

        "return false when a product does not exist" in {
          checkProductExists(journeyData(None), "alcohol/other")          shouldBe false
          checkProductExists(journeyData(None), "alcohol/sparkling-wine") shouldBe false
          checkProductExists(journeyData(None), "tobacco/cigarettes")     shouldBe false
        }
      }

      "declaration response exists in journey data" should {
        "return true when a product exists" in {
          checkProductExists(journeyData(Some(declarationResponse)), "alcohol/beer")                     shouldBe true
          checkProductExists(journeyData(Some(declarationResponse)), "alcohol/wine")                     shouldBe true
          checkProductExists(journeyData(Some(declarationResponse)), "alcohol/cider/sparkling-cider-up") shouldBe true
          checkProductExists(journeyData(Some(declarationResponse)), "alcohol/other")                    shouldBe true
          checkProductExists(journeyData(Some(declarationResponse)), "tobacco/chewing-tobacco")          shouldBe true
          checkProductExists(journeyData(Some(declarationResponse)), "tobacco/rolling-tobacco")          shouldBe true
        }

        "return false when a product does not exist" in {
          checkProductExists(journeyData(Some(declarationResponse)), "alcohol/spirits")        shouldBe false
          checkProductExists(journeyData(Some(declarationResponse)), "tobacco/cigars")         shouldBe false
          checkProductExists(journeyData(Some(declarationResponse)), "tobacco/heated-tobacco") shouldBe false
        }
      }
    }
  }
}
