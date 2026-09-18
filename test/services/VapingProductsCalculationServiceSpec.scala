/*
 * Copyright 2026 HM Revenue & Customs
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

import models.*
import util.BaseSpec

class VapingProductsCalculationServiceSpec extends BaseSpec {

  private val service: VapingProductsCalculationService = new VapingProductsCalculationService

  "VapingProductsCalculationService" when {
    "add product helpers" when {
      ".vapingProductsAddHelper" when {
        "there are multiple vaping-products" should {
          "return the sum of volumes of vaping-products plus the volume of the VapingProductsDto" in {
            val journeyData: JourneyData = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("vaping-products/vape"),
                  iid = "iid0",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("vaping-products/vape"),
                  iid = "iid1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("vaping-products/vape"),
                  iid = "iid2",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val vapeDtoVolume: BigDecimal = 0.5

            val actual: BigDecimal   = service.vapeAddHelper(journeyData, vapeDtoVolume, "vape")
            val expected: BigDecimal = 0.8

            actual shouldBe expected
          }
        }

        "there are multiple vaping-products mixed with other products" should {
          "return only the sum of volumes of vaping-products plus the volume of the VapingProductsDto" in {
            val journeyData: JourneyData = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("vaping-products/vape"),
                  iid = "iid0",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("vaping-products/vape"),
                  iid = "iid1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("vaping-products/vape"),
                  iid = "iid2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("vaping-products/vape"),
                  iid = "iid3",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("vaping-products/vape"),
                  iid = "iid4",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val vapeDtoVolume: BigDecimal = 0.5

            val actual: BigDecimal   = service.vapeAddHelper(journeyData, vapeDtoVolume, "vape")
            val expected: BigDecimal = 1.0

            actual shouldBe expected
          }
        }

        "there is a previous declaration" should {
          "return the sum of volumes of vaping-products  plus the volume of the VapingProductsDto plus " +
            "the sum of volumes of previously declared vaping-products " in {
              val previousDeclarationResponse: DeclarationResponse =
                DeclarationResponse(
                  calculation = Calculation("0.00", "0.00", "0.00", "0.00"),
                  liabilityDetails = LiabilityDetails("", "", "", ""),
                  oldPurchaseProductInstances = List(
                    PurchasedProductInstance(
                      ProductPath("vaping-products/vape"),
                      iid = "iid0",
                      weightOrVolume = Some(0.1)
                    )
                  ),
                  amendmentCount = None
                )

              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid2",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid3",
                    weightOrVolume = Some(0.1)
                  )
                ),
                declarationResponse = Some(previousDeclarationResponse)
              )

              val vapeDtoVolume: BigDecimal = 0.5

              val actual: BigDecimal   = service.vapeAddHelper(journeyData, vapeDtoVolume, "beer")
              val expected: BigDecimal = 0.5

              actual shouldBe expected
            }
        }
      }
    }

    "edit product helpers" when {
      ".vapeEditHelper" when {
        "there are multiple vaping products" should {
          "return the sum of volumes of vaping products plus the volume of the VapingProductsDto " +
            "minus the volume of the vaping products in working instance i.e. vaping products being edited" in {
              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid0",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid1",
                    weightOrVolume = Some(0.2)
                  ),
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid2",
                    weightOrVolume = Some(0.2)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid0",
                    weightOrVolume = Some(0.1)
                  )
                )
              )

              val vapeDtoVolume: BigDecimal = 0.5

              val actual: BigDecimal   = service.vapeEditHelper(journeyData, vapeDtoVolume, "vape", "iid0")
              val expected: BigDecimal = 0.9

              actual shouldBe expected
            }
        }

        "there are multiple vaping products mixed with other products" should {
          "return only the sum of volumes of vaping products plus the volume of the VapingProductsDto " +
            "minus the volume of the vaping products in working instance i.e. vaping products being edited" in {
              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid0",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid2",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid3",
                    weightOrVolume = Some(0.1)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("vaping-products/vape"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  )
                )
              )

              val vapeDtoVolume: BigDecimal = 0.3

              val actual: BigDecimal   = service.vapeEditHelper(journeyData, vapeDtoVolume, "vape", "iid1")
              val expected: BigDecimal = 0.6

              actual shouldBe expected
            }
        }
      }
    }
  }
}
