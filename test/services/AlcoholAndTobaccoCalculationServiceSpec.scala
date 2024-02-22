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

import models.{LiabilityDetails, _}
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

        "there is a previous declaration" should {

          "return the sum of (all beer products + the volume from the AlcoholDto + previously declared alcohol products" in {

            val previousDeclarationResponse =
              DeclarationResponse(
                Calculation("0.00", "0.00", "0.00", "0.00"),
                LiabilityDetails("", "", "", ""),
                List(
                  PurchasedProductInstance(
                    ProductPath("alcohol/beer"),
                    iid = "item1",
                    weightOrVolume = Some(0.1)
                  )
                ),
                amendmentCount = None
              )

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
              ),
              declarationResponse = Some(previousDeclarationResponse)
            )

            val alcoholDtoVolume = BigDecimal(0.5)

            val actual   = service.alcoholAddHelper(data, alcoholDtoVolume, "beer")
            val expected = 0.9

            actual shouldBe expected
          }
        }
      }

      ".noOfSticksTobaccoAddHelper" when {

        "there are multiple cigarette tobacco products" should {

          "return the sum of all cigarette tobacco product units, plus the units from the TobaccoDto" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item1",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item2",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item3",
                  noOfSticks = Some(10)
                )
              )
            )

            val tobaccoDtoWeight = Some(50)

            val actual   = service.noOfSticksTobaccoAddHelper(data, tobaccoDtoWeight, "tobacco/cigarettes")
            val expected = 80

            actual shouldBe expected
          }
        }

        "there are multiple cigarillo products" should {

          "return the sum of all cigarillo tobacco product units, plus the units from the TobaccoDto" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillos"),
                  iid = "item1",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillos"),
                  iid = "item2",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillos"),
                  iid = "item3",
                  noOfSticks = Some(10)
                )
              )
            )

            val tobaccoDtoWeight = Some(50)

            val actual   = service.noOfSticksTobaccoAddHelper(data, tobaccoDtoWeight, "tobacco/cigarillos")
            val expected = 80

            actual shouldBe expected
          }
        }

        "there is a previous declaration" should {

          "return the sum of all cigarettes tobacco product units, plus the units from the TobaccoDto" in {

            val previousDeclarationResponse =
              DeclarationResponse(
                Calculation("0.00", "0.00", "0.00", "0.00"),
                LiabilityDetails("", "", "", ""),
                List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "item0",
                    noOfSticks = Some(10)
                  )
                ),
                amendmentCount = None
              )

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item1",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item2",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item3",
                  noOfSticks = Some(10)
                )
              ),
              declarationResponse = Some(previousDeclarationResponse)
            )

            val tobaccoDtoWeight = Some(50)

            val actual   = service.noOfSticksTobaccoAddHelper(data, tobaccoDtoWeight, "tobacco/cigarettes")
            val expected = 90

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

        "there is a previous declaration" should {

          "return the sum of (all chewing-tobacco products + the volume from the TobaccoDto + previously declared chewing-tobacco products)" in {

            val previousDeclarationResponse =
              DeclarationResponse(
                Calculation("0.00", "0.00", "0.00", "0.00"),
                LiabilityDetails("", "", "", ""),
                List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "item0",
                    weightOrVolume = Some(0.1)
                  )
                ),
                amendmentCount = None
              )

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
              ),
              declarationResponse = Some(previousDeclarationResponse)
            )

            val tobaccoDtoWeight = BigDecimal(0.5)

            val actual   = service.looseTobaccoAddHelper(data, Some(tobaccoDtoWeight))
            val expected = 0.9

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

            val actual   = service.alcoholEditHelper(data, alcoholDtoVolume, "wine", "item1")
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

            val actual   = service.alcoholEditHelper(data, alcoholDtoVolume, "wine", "item2")
            val expected = BigDecimal(0.4)

            actual shouldBe expected
          }
        }
      }

      ".noOfSticksTobaccoEditHelper" when {

        "there are multiple cigarettes products" should {

          "return the sum of all cigarettes product units, plus the (TobaccoDtoNoOfSticks minus WorkingInstanceNoOfSticks(aka product being edited))" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item1",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item2",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item3",
                  noOfSticks = Some(10)
                )
              ),
              workingInstance = Some(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item1",
                  noOfSticks = Some(10)
                )
              )
            )

            val tobaccoDtoWeight = Some(50)
            val actual           = service.noOfSticksTobaccoEditHelper(data, tobaccoDtoWeight, "tobacco/cigarettes", "item1")
            val expected         = 70

            actual shouldBe expected
          }
        }

        "there are multiple cigarillo products" should {

          "return the sum of all cigarillo product units, plus the (TobaccoDtoNoOfSticks minus WorkingInstanceNoOfSticks(aka product being edited))" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillo"),
                  iid = "item1",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillo"),
                  iid = "item2",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillo"),
                  iid = "item3",
                  noOfSticks = Some(10)
                )
              ),
              workingInstance = Some(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillo"),
                  iid = "item1",
                  noOfSticks = Some(10)
                )
              )
            )

            val tobaccoDtoWeight = Some(50)
            val actual           = service.noOfSticksTobaccoEditHelper(data, tobaccoDtoWeight, "tobacco/cigarillo", "item1")
            val expected         = 70

            actual shouldBe expected
          }
        }

        "for cigarillo products and there are other kinds of mixed tobacco products" should {

          "return only the sum of all cigarillo product units, plus the (TobaccoDtoNoOfSticks minus WorkingInstanceNoOfSticks(aka product being edited))" in {

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillo"),
                  iid = "item1",
                  noOfSticks = Some(10),
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillo"),
                  iid = "item2",
                  noOfSticks = Some(10),
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillo"),
                  iid = "item3",
                  noOfSticks = Some(10),
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillo"),
                  iid = "item4",
                  noOfSticks = Some(10),
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
                  ProductPath("tobacco/cigarillo"),
                  iid = "item1",
                  weightOrVolume = Some(10)
                )
              )
            )

            val tobaccoDtoWeight = Some(50)

            val actual   = service.noOfSticksTobaccoEditHelper(data, tobaccoDtoWeight, "tobacco/cigarillo", "item1")
            val expected = 90

            actual shouldBe expected
          }
        }

        "there is a previous declaration" should {

          "return the sum of (all cigarettes tobacco product units + the units from the edited TobaccoDto/workingInstance + previously declared chewing-tobacco products)" in {

            val previousDeclarationResponse =
              DeclarationResponse(
                Calculation("0.00", "0.00", "0.00", "0.00"),
                LiabilityDetails("", "", "", ""),
                List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "item0",
                    noOfSticks = Some(10)
                  )
                ),
                amendmentCount = None
              )

            val data = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item1",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item2",
                  noOfSticks = Some(10)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item3",
                  noOfSticks = Some(10)
                )
              ),
              workingInstance = Some(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "item1",
                  noOfSticks = Some(10)
                )
              ),
              declarationResponse = Some(previousDeclarationResponse)
            )

            val tobaccoDtoWeight = Some(50)
            val actual           = service.noOfSticksTobaccoEditHelper(data, tobaccoDtoWeight, "tobacco/cigarettes", "item1")
            val expected         = 80

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
            val actual           = service.looseTobaccoEditHelper(data, tobaccoDtoWeight, "item1")
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
            val actual           = service.looseTobaccoEditHelper(data, tobaccoDtoWeight, "item1")
            val expected         = BigDecimal(0.90000)

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

            val actual   = service.looseTobaccoEditHelper(data, tobaccoDtoWeight, "item1")
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

            val actual   = service.looseTobaccoEditHelper(data, tobaccoDtoWeight, "item1")
            val expected = BigDecimal(0.60000)

            actual shouldBe expected
          }
        }

        "there is a previous declaration" should {

          "return the sum of (all chewing-tobacco products + the volume from the edited TobaccoDto/workingInstance + previously declared chewing-tobacco products)" in {

            val previousDeclarationResponse =
              DeclarationResponse(
                Calculation("0.00", "0.00", "0.00", "0.00"),
                LiabilityDetails("", "", "", ""),
                List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "item0",
                    weightOrVolume = Some(0.1)
                  )
                ),
                amendmentCount = None
              )

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
              ),
              declarationResponse = Some(previousDeclarationResponse)
            )

            val tobaccoDtoWeight = Some(BigDecimal(0.5))
            val actual           = service.looseTobaccoEditHelper(data, tobaccoDtoWeight, "item1")
            val expected         = BigDecimal(1.00000)

            actual shouldBe expected
          }
        }

      }
    }
  }
}
