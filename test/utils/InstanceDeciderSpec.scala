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

import models._
import util._

class InstanceDeciderSpec extends InstanceDecider with BaseSpec {

  private def purchasedProductInstance(
    path: String,
    iid: String,
    weightOrVolume: Option[BigDecimal],
    noOfSticks: Option[Int]
  ) = PurchasedProductInstance(
    path = ProductPath(path),
    iid = iid,
    weightOrVolume = weightOrVolume,
    noOfSticks = noOfSticks,
    currency = Some("GBP"),
    cost = Some(100.00)
  )

  private val purchasedProductInstances: List[PurchasedProductInstance] = List(
    purchasedProductInstance("alcohol/beer", "ZkCSUz", Some(10.00), None),
    purchasedProductInstance("alcohol/beer", "OSDZTJ", Some(15.00), None),
    purchasedProductInstance("alcohol/wine", "uRgxRU", Some(20.00), None),
    purchasedProductInstance("alcohol/wine", "dqlkQn", Some(25.00), None),
    purchasedProductInstance("tobacco/cigars", "xBgxTU", Some(0.2), Some(2)),
    purchasedProductInstance("tobacco/cigars", "yFvowp", Some(0.2), Some(3)),
    purchasedProductInstance("tobacco/cigarettes", "fHgRTU", None, Some(2)),
    purchasedProductInstance("tobacco/cigarettes", "RWManU", None, Some(3))
  )

  private def journeyData(workingInstance: Option[PurchasedProductInstance]) = JourneyData(
    prevDeclaration = Some(false),
    euCountryCheck = Some("greatBritain"),
    arrivingNICheck = Some(true),
    bringingOverAllowance = Some(true),
    isUKResident = Some(false),
    privateCraft = Some(false),
    ageOver17 = Some(true),
    purchasedProductInstances = purchasedProductInstances,
    workingInstance = workingInstance
  )

  "InstanceDecider" when {
    ".originalAmountEnteredWeightOrVolume" should {
      "return the correct original amount entered from the purchased product instance" when {
        "a working instance that exists in journey data has same weight or volume" in {
          originalAmountEnteredWeightOrVolume(
            journeyData = journeyData(
              workingInstance = Some(
                purchasedProductInstance(
                  path = "alcohol/beer",
                  iid = "ZkCSUz",
                  weightOrVolume = Some(10.00),
                  noOfSticks = None
                )
              )
            ),
            iid = "ZkCSUz"
          ) shouldBe 10.00
        }

        "a working instance that exists in journey data has different weight or volume" in {
          originalAmountEnteredWeightOrVolume(
            journeyData = journeyData(
              workingInstance = Some(
                purchasedProductInstance(
                  path = "alcohol/wine",
                  iid = "dqlkQn",
                  weightOrVolume = Some(2.00),
                  noOfSticks = None
                )
              )
            ),
            iid = "dqlkQn"
          ) shouldBe 25.00
        }

        "a working instance that exists in journey data does not have weight or volume" in {
          originalAmountEnteredWeightOrVolume(
            journeyData = journeyData(
              workingInstance = Some(
                purchasedProductInstance(
                  path = "alcohol/wine",
                  iid = "uRgxRU",
                  weightOrVolume = None,
                  noOfSticks = None
                )
              )
            ),
            iid = "uRgxRU"
          ) shouldBe 20.00
        }

        "a working instance does not exist in journey data" in {
          originalAmountEnteredWeightOrVolume(
            journeyData = journeyData(workingInstance = None),
            iid = "OSDZTJ"
          ) shouldBe 15.00
        }
      }
    }

    ".originalAmountEnteredNoOfSticks" should {
      "return the correct original amount entered from the purchased product instance" when {
        "a working instance that exists in journey data has same number of sticks" in {
          originalAmountEnteredNoOfSticks(
            journeyData = journeyData(
              workingInstance = Some(
                purchasedProductInstance(
                  path = "tobacco/cigars",
                  iid = "xBgxTU",
                  weightOrVolume = Some(0.2),
                  noOfSticks = Some(2)
                )
              )
            ),
            iid = "xBgxTU"
          ) shouldBe 2
        }

        "a working instance that exists in journey data has different number of sticks" in {
          originalAmountEnteredNoOfSticks(
            journeyData = journeyData(
              workingInstance = Some(
                purchasedProductInstance(
                  path = "tobacco/cigars",
                  iid = "yFvowp",
                  weightOrVolume = Some(0.2),
                  noOfSticks = Some(1)
                )
              )
            ),
            iid = "yFvowp"
          ) shouldBe 3
        }

        "a working instance that exists in journey data does not have number of sticks" in {
          originalAmountEnteredNoOfSticks(
            journeyData = journeyData(
              workingInstance = Some(
                purchasedProductInstance(
                  path = "tobacco/cigarettes",
                  iid = "RWManU",
                  weightOrVolume = None,
                  noOfSticks = None
                )
              )
            ),
            iid = "RWManU"
          ) shouldBe 3
        }

        "a working instance does not exist in journey data" in {
          originalAmountEnteredNoOfSticks(
            journeyData = journeyData(workingInstance = None),
            iid = "fHgRTU"
          ) shouldBe 2
        }
      }
    }
  }
}
