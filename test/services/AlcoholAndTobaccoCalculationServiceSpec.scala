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

  private val service: AlcoholAndTobaccoCalculationService = new AlcoholAndTobaccoCalculationService

  "AlcoholAndTobaccoCalculationService" when {
    "add product helpers" when {
      ".alcoholAddHelper" when {
        "there are multiple beer products" should {
          "return the sum of volumes of beer products plus the volume of the AlcoholDto" in {
            val journeyData: JourneyData = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "iid0",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "iid1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "iid2",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val alcoholDtoVolume: BigDecimal = 0.5

            val actual: BigDecimal   = service.alcoholAddHelper(journeyData, alcoholDtoVolume, "beer")
            val expected: BigDecimal = 0.8

            actual shouldBe expected
          }
        }

        "there are multiple beer products mixed with other products" should {
          "return only the sum of volumes of beer products plus the volume of the AlcoholDto" in {
            val journeyData: JourneyData = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "iid0",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "iid1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/spirits"),
                  iid = "iid2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/beer"),
                  iid = "iid3",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("alcohol/cider"),
                  iid = "iid4",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val alcoholDtoVolume: BigDecimal = 0.5

            val actual: BigDecimal   = service.alcoholAddHelper(journeyData, alcoholDtoVolume, "beer")
            val expected: BigDecimal = 0.8

            actual shouldBe expected
          }
        }

        "there is a previous declaration" should {
          "return the sum of volumes of beer products plus the volume of the AlcoholDto plus " +
            "the sum of volumes of previously declared beer products" in {
              val previousDeclarationResponse: DeclarationResponse =
                DeclarationResponse(
                  calculation = Calculation("0.00", "0.00", "0.00", "0.00"),
                  liabilityDetails = LiabilityDetails("", "", "", ""),
                  oldPurchaseProductInstances = List(
                    PurchasedProductInstance(
                      ProductPath("alcohol/beer"),
                      iid = "iid0",
                      weightOrVolume = Some(0.1)
                    )
                  ),
                  amendmentCount = None
                )

              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("alcohol/beer"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("alcohol/beer"),
                    iid = "iid2",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("alcohol/beer"),
                    iid = "iid3",
                    weightOrVolume = Some(0.1)
                  )
                ),
                declarationResponse = Some(previousDeclarationResponse)
              )

              val alcoholDtoVolume: BigDecimal = 0.5

              val actual: BigDecimal   = service.alcoholAddHelper(journeyData, alcoholDtoVolume, "beer")
              val expected: BigDecimal = 0.9

              actual shouldBe expected
            }
        }
      }

      ".noOfSticksTobaccoAddHelper" when {
        "there are multiple cigarette products" should {
          "return the sum of number of sticks of cigarette products plus the number of sticks of the TobaccoDto" in {
            val (journeyDataNoOfSticks, tobaccoNoOfSticks): (Int, Int) = (10, 50)

            val journeyData: JourneyData = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "iid0",
                  noOfSticks = Some(journeyDataNoOfSticks)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "iid1",
                  noOfSticks = Some(journeyDataNoOfSticks)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarettes"),
                  iid = "iid2",
                  noOfSticks = Some(journeyDataNoOfSticks)
                )
              )
            )

            val tobaccoDtoNoOfSticks: Option[Int] = Some(tobaccoNoOfSticks)

            val actual: Int   =
              service.noOfSticksTobaccoAddHelper(journeyData, tobaccoDtoNoOfSticks, "cigarettes")
            val expected: Int = 80

            actual shouldBe expected
          }
        }

        "there are multiple cigarillo products" should {
          "return the sum of number of sticks of cigarillo products plus the number of sticks of the TobaccoDto" in {
            val (journeyDataNoOfSticks, tobaccoNoOfSticks): (Int, Int) = (10, 50)

            val journeyData: JourneyData = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillos"),
                  iid = "iid0",
                  noOfSticks = Some(journeyDataNoOfSticks),
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillos"),
                  iid = "iid1",
                  noOfSticks = Some(journeyDataNoOfSticks),
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/cigarillos"),
                  iid = "iid2",
                  noOfSticks = Some(journeyDataNoOfSticks),
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoNoOfSticks: Option[Int] = Some(tobaccoNoOfSticks)

            val actual: Int   =
              service.noOfSticksTobaccoAddHelper(journeyData, tobaccoDtoNoOfSticks, "cigarillos")
            val expected: Int = 80

            actual shouldBe expected
          }
        }

        "there is a previous declaration" should {
          "return the sum of number of sticks of cigarette products plus the number of sticks of the TobaccoDto plus " +
            "the sum of number of sticks of previously declared cigarette products" in {
              val (commonNoOfSticks, tobaccoNoOfSticks): (Int, Int) = (10, 50)

              val previousDeclarationResponse: DeclarationResponse =
                DeclarationResponse(
                  calculation = Calculation("0.00", "0.00", "0.00", "0.00"),
                  liabilityDetails = LiabilityDetails("", "", "", ""),
                  oldPurchaseProductInstances = List(
                    PurchasedProductInstance(
                      ProductPath("tobacco/cigarettes"),
                      iid = "iid0",
                      noOfSticks = Some(commonNoOfSticks)
                    )
                  ),
                  amendmentCount = None
                )

              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid1",
                    noOfSticks = Some(commonNoOfSticks)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid2",
                    noOfSticks = Some(commonNoOfSticks)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid3",
                    noOfSticks = Some(commonNoOfSticks)
                  )
                ),
                declarationResponse = Some(previousDeclarationResponse)
              )

              val tobaccoDtoNoOfSticks: Option[Int] = Some(tobaccoNoOfSticks)

              val actual: Int   =
                service.noOfSticksTobaccoAddHelper(journeyData, tobaccoDtoNoOfSticks, "cigarettes")
              val expected: Int = 90

              actual shouldBe expected
            }
        }
      }

      ".looseTobaccoAddHelper" when {
        "there are multiple pipe or chewing tobacco products" should {
          "return the sum of weights of pipe or chewing tobacco products plus the weight of the TobaccoDto" in {
            val journeyData: JourneyData = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "iid0",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "iid1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "iid2",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoWeight: Option[BigDecimal] = Some(0.5)

            val actual: BigDecimal   = service.looseTobaccoAddHelper(journeyData, tobaccoDtoWeight)
            val expected: BigDecimal = 0.80000

            actual shouldBe expected
          }
        }

        "there are multiple rolling tobacco products" should {
          "return the sum of weights of rolling tobacco products plus the weight of the TobaccoDto" in {
            val journeyData: JourneyData = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "iid0",
                  weightOrVolume = Some(0.2)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "iid1",
                  weightOrVolume = Some(0.2)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "iid2",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoWeight: Option[BigDecimal] = Some(0.5)

            val actual: BigDecimal   = service.looseTobaccoAddHelper(journeyData, tobaccoDtoWeight)
            val expected: BigDecimal = 1.00000

            actual shouldBe expected
          }
        }

        "there are mixed loose tobacco products" should {
          "return the sum of weights of loose tobacco products plus the weight of the TobaccoDto" in {
            val journeyData: JourneyData = JourneyData(
              purchasedProductInstances = List(
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "iid0",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "iid1",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/chewing-tobacco"),
                  iid = "iid2",
                  weightOrVolume = Some(0.1)
                ),
                PurchasedProductInstance(
                  ProductPath("tobacco/rolling-tobacco"),
                  iid = "iid3",
                  weightOrVolume = Some(0.1)
                )
              )
            )

            val tobaccoDtoWeight: Option[BigDecimal] = Some(0.5)

            val actual: BigDecimal   = service.looseTobaccoAddHelper(journeyData, tobaccoDtoWeight)
            val expected: BigDecimal = 0.90000

            actual shouldBe expected
          }
        }

        "there is a previous declaration" should {
          "return the sum of weights of pipe or chewing tobacco products plus the weight of the TobaccoDto plus " +
            "the sum of weights of previously declared pipe or chewing tobacco products" in {
              val previousDeclarationResponse: DeclarationResponse =
                DeclarationResponse(
                  calculation = Calculation("0.00", "0.00", "0.00", "0.00"),
                  liabilityDetails = LiabilityDetails("", "", "", ""),
                  oldPurchaseProductInstances = List(
                    PurchasedProductInstance(
                      ProductPath("tobacco/chewing-tobacco"),
                      iid = "iid0",
                      weightOrVolume = Some(0.1)
                    )
                  ),
                  amendmentCount = None
                )

              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "iid2",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "iid3",
                    weightOrVolume = Some(0.1)
                  )
                ),
                declarationResponse = Some(previousDeclarationResponse)
              )

              val tobaccoDtoWeight: Option[BigDecimal] = Some(0.5)

              val actual   = service.looseTobaccoAddHelper(journeyData, tobaccoDtoWeight)
              val expected = 0.9

              actual shouldBe expected
            }
        }
      }
    }

    "edit product helpers" when {
      ".alcoholEditHelper" when {
        "there are multiple wine products" should {
          "return the sum of volumes of wine products plus the volume of the AlcoholDto " +
            "minus the volume of the wine product in working instance i.e. wine product being edited" in {
              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("alcohol/wine"),
                    iid = "iid0",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("alcohol/wine"),
                    iid = "iid1",
                    weightOrVolume = Some(0.2)
                  ),
                  PurchasedProductInstance(
                    ProductPath("alcohol/wine"),
                    iid = "iid2",
                    weightOrVolume = Some(0.2)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("alcohol/wine"),
                    iid = "iid0",
                    weightOrVolume = Some(0.1)
                  )
                )
              )

              val alcoholDtoVolume: BigDecimal = 0.5

              val actual: BigDecimal   = service.alcoholEditHelper(journeyData, alcoholDtoVolume, "wine", "iid0")
              val expected: BigDecimal = 0.9

              actual shouldBe expected
            }
        }

        "there are multiple wine products mixed with other products" should {
          "return only the sum of volumes of wine products plus the volume of the AlcoholDto " +
            "minus the volume of the wine product in working instance i.e. wine product being edited" in {
              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("alcohol/beer"),
                    iid = "iid0",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("alcohol/wine"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("alcohol/cider/non-sparkling-cider"),
                    iid = "iid2",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("alcohol/wine"),
                    iid = "iid3",
                    weightOrVolume = Some(0.1)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("alcohol/wine"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  )
                )
              )

              val alcoholDtoVolume: BigDecimal = 0.3

              val actual: BigDecimal   = service.alcoholEditHelper(journeyData, alcoholDtoVolume, "wine", "iid1")
              val expected: BigDecimal = 0.4

              actual shouldBe expected
            }
        }
      }

      ".noOfSticksTobaccoEditHelper" when {
        "there are multiple cigarette products" should {
          "return the sum of number of sticks of cigarette products plus the number of sticks of the TobaccoDto " +
            "minus the number of sticks of cigarette product in working instance i.e. cigarette product being edited" in {
              val (journeyDataNoOfSticks, tobaccoNoOfSticks): (Int, Int) = (10, 50)

              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid0",
                    noOfSticks = Some(journeyDataNoOfSticks)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid1",
                    noOfSticks = Some(journeyDataNoOfSticks)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid2",
                    noOfSticks = Some(journeyDataNoOfSticks)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid0",
                    noOfSticks = Some(journeyDataNoOfSticks)
                  )
                )
              )

              val tobaccoDtoNoOfSticks: Option[Int] = Some(tobaccoNoOfSticks)

              val actual: Int   =
                service.noOfSticksTobaccoEditHelper(journeyData, tobaccoDtoNoOfSticks, "cigarettes", "iid0")
              val expected: Int = 70

              actual shouldBe expected
            }
        }

        "there are multiple cigarillo products" should {
          "return the sum of number of sticks of cigarillo products plus the number of sticks of the TobaccoDto " +
            "minus the number of sticks of cigarillo product in working instance i.e. cigarillo product being edited" in {
              val (journeyDataNoOfSticks, tobaccoNoOfSticks): (Int, Int) = (10, 50)

              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarillos"),
                    iid = "iid0",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarillos"),
                    iid = "iid1",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarillos"),
                    iid = "iid2",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarillos"),
                    iid = "iid0",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  )
                )
              )

              val tobaccoDtoNoOfSticks: Option[Int] = Some(tobaccoNoOfSticks)

              val actual: Int   =
                service.noOfSticksTobaccoEditHelper(journeyData, tobaccoDtoNoOfSticks, "cigarillos", "iid0")
              val expected: Int = 70

              actual shouldBe expected
            }
        }

        "there are multiple cigarillo products mixed with other products" should {
          "return only the sum of number of sticks of cigarillo products plus the number of sticks of the TobaccoDto " +
            "minus the number of sticks of cigarillo product in working instance i.e. cigarillo product being edited" in {
              val (journeyDataNoOfSticks, tobaccoNoOfSticks): (Int, Int) = (10, 50)

              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarillos"),
                    iid = "iid0",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarillos"),
                    iid = "iid1",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarillos"),
                    iid = "iid2",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarillos"),
                    iid = "iid3",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigars"),
                    iid = "iid4",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid5",
                    noOfSticks = Some(journeyDataNoOfSticks)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarillos"),
                    iid = "iid0",
                    noOfSticks = Some(journeyDataNoOfSticks),
                    weightOrVolume = Some(0.1)
                  )
                )
              )

              val tobaccoDtoNoOfSticks: Option[Int] = Some(tobaccoNoOfSticks)

              val actual: Int   =
                service.noOfSticksTobaccoEditHelper(journeyData, tobaccoDtoNoOfSticks, "cigarillos", "iid0")
              val expected: Int = 80

              actual shouldBe expected
            }
        }

        "there is a previous declaration" should {
          "return the sum of number of sticks of cigarette products plus the number of sticks of the TobaccoDto plus " +
            "the sum of number of sticks of previously declared cigarette products " +
            "minus the number of cigarette sticks in working instance i.e. cigarette product being edited" in {
              val (commonNoOfSticks, tobaccoNoOfSticks): (Int, Int) = (10, 50)

              val previousDeclarationResponse: DeclarationResponse =
                DeclarationResponse(
                  calculation = Calculation("0.00", "0.00", "0.00", "0.00"),
                  liabilityDetails = LiabilityDetails("", "", "", ""),
                  oldPurchaseProductInstances = List(
                    PurchasedProductInstance(
                      ProductPath("tobacco/cigarettes"),
                      iid = "iid0",
                      noOfSticks = Some(commonNoOfSticks)
                    )
                  ),
                  amendmentCount = None
                )

              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid1",
                    noOfSticks = Some(commonNoOfSticks)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid2",
                    noOfSticks = Some(commonNoOfSticks)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid3",
                    noOfSticks = Some(commonNoOfSticks)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid1",
                    noOfSticks = Some(commonNoOfSticks)
                  )
                ),
                declarationResponse = Some(previousDeclarationResponse)
              )

              val tobaccoDtoWeight: Option[Int] = Some(tobaccoNoOfSticks)

              val actual: Int   =
                service.noOfSticksTobaccoEditHelper(journeyData, tobaccoDtoWeight, "cigarettes", "iid1")
              val expected: Int = 80

              actual shouldBe expected
            }
        }
      }

      ".looseTobaccoEditHelper" when {
        "there are multiple pipe or chewing tobacco products" should {
          "return the sum of weights of pipe or chewing tobacco products plus the weight of the TobaccoDto " +
            "minus the weight of pipe or chewing tobacco in working instance i.e. " +
            "pipe or chewing tobacco product being edited" in {
              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "iid0",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "iid1",
                    weightOrVolume = Some(0.2)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "iid2",
                    weightOrVolume = Some(0.2)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "iid0",
                    weightOrVolume = Some(0.1)
                  )
                )
              )

              val tobaccoDtoWeight: Option[BigDecimal] = Some(0.5)

              val actual: BigDecimal   = service.looseTobaccoEditHelper(journeyData, tobaccoDtoWeight, "iid0")
              val expected: BigDecimal = 0.90000

              actual shouldBe expected
            }
        }

        "there are multiple rolling tobacco products" should {
          "return the sum of weights of rolling tobacco products plus the weight of the TobaccoDto " +
            "minus the weight of rolling tobacco in working instance i.e. rolling tobacco product being edited" in {
              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid0",
                    weightOrVolume = Some(0.2)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid1",
                    weightOrVolume = Some(0.2)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid2",
                    weightOrVolume = Some(0.1)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid0",
                    weightOrVolume = Some(0.2)
                  )
                )
              )

              val tobaccoDtoWeight: Option[BigDecimal] = Some(0.6)

              val actual: BigDecimal   = service.looseTobaccoEditHelper(journeyData, tobaccoDtoWeight, "iid0")
              val expected: BigDecimal = 0.90000

              actual shouldBe expected
            }
        }

        "there are multiple loose tobacco products with all kinds of mixed tobacco products" should {
          "return the sum of weights of loose tobacco products plus the weight of the TobaccoDto " +
            "minus the weight of rolling tobacco in working instance i.e. rolling tobacco product being edited" in {
              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "iid0",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/chewing-tobacco"),
                    iid = "iid2",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid3",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigars"),
                    iid = "iid4",
                    noOfSticks = Some(1),
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid5",
                    noOfSticks = Some(1)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  )
                )
              )

              val tobaccoDtoWeight: Option[BigDecimal] = Some(0.3)

              val actual: BigDecimal   = service.looseTobaccoEditHelper(journeyData, tobaccoDtoWeight, "iid1")
              val expected: BigDecimal = 0.60000

              actual shouldBe expected
            }
        }

        "there is a previous declaration" should {
          "return the sum of weights of rolling tobacco products plus the weight of the TobaccoDto plus " +
            "the sum of weights of previously declared rolling tobacco products " +
            "minus the weight of rolling tobacco product in working instance i.e. " +
            "rolling tobacco product being edited" in {
              val previousDeclarationResponse: DeclarationResponse =
                DeclarationResponse(
                  calculation = Calculation("0.00", "0.00", "0.00", "0.00"),
                  liabilityDetails = LiabilityDetails("", "", "", ""),
                  oldPurchaseProductInstances = List(
                    PurchasedProductInstance(
                      ProductPath("tobacco/rolling-tobacco"),
                      iid = "iid0",
                      weightOrVolume = Some(0.1)
                    )
                  ),
                  amendmentCount = None
                )

              val journeyData: JourneyData = JourneyData(
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid2",
                    weightOrVolume = Some(0.2)
                  ),
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid3",
                    weightOrVolume = Some(0.2)
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/rolling-tobacco"),
                    iid = "iid1",
                    weightOrVolume = Some(0.1)
                  )
                ),
                declarationResponse = Some(previousDeclarationResponse)
              )

              val tobaccoDtoWeight: Option[BigDecimal] = Some(0.5)

              val actual               = service.looseTobaccoEditHelper(journeyData, tobaccoDtoWeight, "iid1")
              val expected: BigDecimal = 1.00000

              actual shouldBe expected
            }
        }
      }
    }
  }
}
