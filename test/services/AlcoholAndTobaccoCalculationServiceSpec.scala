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

package services

import models._
import util.BaseSpec

class AlcoholAndTobaccoCalculationServiceSpec extends BaseSpec {

  val service = new AlcoholAndTobaccoCalculationService

  "AlcoholAndTobaccoCalculationService" when {

    "Add product helpers" when {

      ".alcoholAddHelper" when {

        "there are multiple beer products" should {

          "return the sum of all beer products, plus the volume from the AlcoholDto" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "item2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "item3",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val alcoholDtoVolume = BigDecimal(0.5)

            val actual   = service.alcoholAddHelper(data, alcoholDtoVolume, "beer")
            val expected = 0.8

            actual shouldBe expected
          }
        }

        "there are multiple beer products mixed with other products" should {

          "return only the sum of all beer products, plus the volume from the AlcoholDto" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "item2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/spirits"),
                  iid = "item3",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "item4",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/cider"),
                  iid = "item5",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val alcoholDtoVolume = BigDecimal(0.5)

            val actual   = service.alcoholAddHelper(data, alcoholDtoVolume, "beer")
            val expected = 0.8

            actual shouldBe expected
          }
        }
      }

      ".looseTobaccoAddHelper" when {

        "there are multiple chewing-tobacco products" should {

          "return the sum of all chewing-tobacco product weights, plus the weight from the TobaccoDto - in grams (5 decimal places)" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item3",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoWeight = Some(BigDecimal(0.5))

            val actual   = service.looseTobaccoAddHelper(data, tobaccoDtoWeight)
            val expected = BigDecimal(0.80000)

            actual shouldBe expected
          }
        }

        "there are multiple rolling-tobacco products" should {

          "return the sum of all chewing-tobacco product weights, plus the weight from the TobaccoDto - in grams (5 decimal places)" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.2)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item2",
                  weightOrVolume = Some(0.2)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item3",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoWeight = Some(BigDecimal(0.5))

            val actual   = service.looseTobaccoAddHelper(data, tobaccoDtoWeight)
            val expected = BigDecimal(1.00000)

            actual shouldBe expected
          }
        }

        "there are mixed loose tobacco products" should {

          "return the sum of all loose tobacco product weights, plus the weight from the TobaccoDto - in grams (5 decimal places)" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item3",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item4",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoWeight = Some(BigDecimal(0.5))
            val actual           = service.looseTobaccoAddHelper(data, tobaccoDtoWeight)
            val expected         = BigDecimal(0.90000)

            actual shouldBe expected
          }
        }
      }
    }

    "Edit product helpers" when {

      ".alcoholEditHelper" when {

        "there are multiple alcohol products of the same type" should {

          "return the sum of all alcohol product volumes of the same type, plus the (AlcoholDtoVolume minus WorkingInstanceVolume(aka product being edited)) for the correct product" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("alcohol/wine"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/wine"),
                  iid = "item2",
                  weightOrVolume = Some(0.2)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/wine"),
                  iid = "item3",
                  weightOrVolume = Some(0.2)
                )
              ),
              workingInstance = Some(
                PurchasedProductInstance(
                  ProductPath("alcohol/wine"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val alcoholDtoVolume = BigDecimal(0.5)

            val actual   = service.alcoholEditHelper(data, alcoholDtoVolume, "wine")
            val expected = BigDecimal(0.9)

            actual shouldBe expected
          }
        }

        "there are mixed alcohol products" should {

          "return the sum of all alcohol product volumes of the same type, plus the (AlcoholDtoVolume minus WorkingInstanceVolume(aka product being edited)) for the correct product" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/wine"),
                  iid = "item2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/non-sparkling-cider"),
                  iid = "item3",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/wine"),
                  iid = "item4",
                  weightOrVolume = Some(0.1)
                )
              ),
              workingInstance = Some(
                PurchasedProductInstance(
                  ProductPath("alcohol/wine"),
                  iid = "item2",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val alcoholDtoVolume = BigDecimal(0.3)

            val actual   = service.alcoholEditHelper(data, alcoholDtoVolume, "wine")
            val expected = BigDecimal(0.4)

            actual shouldBe expected
          }
        }
      }

      ".looseTobaccoEditHelper" when {

        "there are multiple chewing-tobacco products" should {

          "return the sum of all chewing-tobacco product weights, plus the (TobaccoDtoWeight minus WorkingInstanceWeight(aka product being edited)) - in grams (5 decimal places)" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item2",
                  weightOrVolume = Some(0.2)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item3",
                  weightOrVolume = Some(0.2)
                )
              ),
              workingInstance = Some(
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoWeight = Some(BigDecimal(0.5))
            val actual           = service.looseTobaccoEditHelper(data, tobaccoDtoWeight)
            val expected         = BigDecimal(0.90000)

            actual shouldBe expected
          }
        }

        "there are multiple rolling-tobacco products" should {

          "return the sum of all rolling-tobacco product weights, plus the (TobaccoDtoWeight minus WorkingInstanceWeight(aka product being edited)) - in grams (5 decimal places)" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.2)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item2",
                  weightOrVolume = Some(0.2)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item3",
                  weightOrVolume = Some(0.1)
                )
              ),
              workingInstance = Some(
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.2)
                )
              )
            )

            val tobaccoDtoWeight = Some(BigDecimal(0.6))
            val actual   = service.looseTobaccoEditHelper(data, tobaccoDtoWeight)
            val expected = BigDecimal(0.90000)

            actual shouldBe expected
          }
        }

        "there are mixed loose tobacco products" should {

          "return the sum of all loose-tobacco product weights, plus the (TobaccoDtoWeight minus WorkingInstanceWeight(aka product being edited)) - in grams (5 decimal places)" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item3",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item4",
                  weightOrVolume = Some(0.1)
                )
              ),
              workingInstance = Some(
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoWeight = Some(BigDecimal(0.3))

            val actual   = service.looseTobaccoEditHelper(data, tobaccoDtoWeight)
            val expected = BigDecimal(0.60000)

            actual shouldBe expected
          }
        }

        "there are all kinds of mixed tobacco products" should {

          "return only the sum of all loose-tobacco product weights, plus the (TobaccoDtoWeight minus WorkingInstanceWeight(aka product being edited)) - in grams (5 decimal places)" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "item3",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item4",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigars"),
                  iid = "item5",
                  noOfSticks = Some(100),
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item6",
                  noOfSticks = Some(300),
                  weightOrVolume = None
                )
              ),
              workingInstance = Some(
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "item1",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoWeight = Some(BigDecimal(0.3))

            val actual   = service.looseTobaccoEditHelper(data, tobaccoDtoWeight)
            val expected = BigDecimal(0.60000)

            actual shouldBe expected
          }
        }

      }
    }
  }
}
