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

package controllers

import connectors.Cache
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.BaseSpec

import scala.concurrent.Future

class LimitExceedControllerSpec extends BaseSpec {

  private val mockCache: Cache = mock(classOf[Cache])

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(mockCache))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCache)
  }

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

  private def declarationResponse(
    oldPurchaseProductInstances: List[PurchasedProductInstance]
  ): Option[DeclarationResponse] =
    Some(
      DeclarationResponse(
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
    )

  private def journeyData(
    purchasedProductInstances: List[PurchasedProductInstance] = Nil,
    workingInstance: Option[PurchasedProductInstance] = None,
    declarationResponse: Option[DeclarationResponse] = None
  ) =
    JourneyData(
      prevDeclaration = Some(false),
      euCountryCheck = Some("greatBritain"),
      arrivingNICheck = Some(true),
      bringingOverAllowance = Some(true),
      ageOver17 = Some(true),
      privateCraft = Some(false),
      purchasedProductInstances = purchasedProductInstances,
      workingInstance = workingInstance,
      declarationResponse = declarationResponse
    )

  "LimitExceedController" when {
    ".onPageLoadAddJourneyAlcoholVolume" when {
      "making a first declaration" should {
        Seq(("beer", "120"), ("spirits", "15")).foreach { case (productToken, userInput) =>
          s"load limit exceed page and display the content specifically for $productToken" in {
            when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData())))

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/$productToken/upper-limits/volume"
              ).withSession(s"user-amount-input-$productToken" -> userInput)
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc
              .getElementsByTag("h1")
              .text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text() shouldBe s"You have entered a total of $userInput litres of $productToken."
            content     should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }

        val wineSparklingWineGroupMessage: String = "wine (this includes sparkling wine)."
        val ciderOtherAlcoholGroupMessage: String =
          "all other alcoholic drinks (including cider, port, sherry and alcohol up to 22%)."

        Seq(
          ("wine", "sparking wine", "alcohol/sparkling-wine", "100", s"110 litres of $wineSparklingWineGroupMessage"),
          ("sparkling-wine", "wine", "alcohol/wine", "100", s"110 litres of $wineSparklingWineGroupMessage"),
          (
            "other",
            "cider",
            "alcohol/cider/non-sparkling-cider",
            "30.01",
            s"40.01 litres of $ciderOtherAlcoholGroupMessage"
          )
        ).foreach { case (productToken, previouslyAddedAlcohol, path, userInput, totalWithGroupMessage) =>
          s"load limit exceed page and display the group content for $productToken when $previouslyAddedAlcohol has been added" in {
            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    purchasedProductInstances = List(
                      purchasedProductInstance(
                        path = path,
                        iid = "iid0",
                        weightOrVolume = Some(10.0),
                        noOfSticks = None
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/$productToken/upper-limits/volume"
              ).withSession(s"user-amount-input-$productToken" -> userInput)
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc
              .getElementsByTag("h1")
              .text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text() shouldBe s"You have entered a total of $totalWithGroupMessage"
            content     should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }

        "not load limit exceed page if the path is incorrect" in {
          when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData())))

          val result: Future[Result] = route(
            app,
            FakeRequest(
              "GET",
              "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/zzz/yyy/upper-limits/volume"
            )
          ).get

          status(result) shouldBe NOT_FOUND
        }
      }

      "making an amendment to a previous declaration" should {
        Seq(("beer", "20.001", "110.001"), ("spirits", "3.001", "10.001")).foreach {
          case (productToken, userInput, totalVolume) =>
            s"load limit exceed page and display the content specifically for $productToken" in {
              when(mockCache.fetch(any())).thenReturn(
                Future.successful(
                  Some(
                    journeyData(
                      declarationResponse = declarationResponse(
                        oldPurchaseProductInstances = List(
                          purchasedProductInstance(
                            path = "alcohol/beer",
                            iid = "uFgxRU",
                            weightOrVolume = Some(90.0),
                            noOfSticks = None
                          ),
                          purchasedProductInstance(
                            path = "alcohol/spirits",
                            iid = "ZjCSUz",
                            weightOrVolume = Some(7.0),
                            noOfSticks = None
                          )
                        )
                      )
                    )
                  )
                )
              )

              val result: Future[Result] = route(
                app,
                FakeRequest(
                  "GET",
                  s"/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/$productToken/upper-limits/volume"
                ).withSession(s"user-amount-input-$productToken" -> userInput)
              ).get

              status(result) shouldBe OK

              val content: String = contentAsString(result)
              val doc: Document   = Jsoup.parse(content)

              doc
                .getElementsByTag("h1")
                .text() shouldBe "There is a problem"
              doc
                .getElementById("entered-amount")
                .text() shouldBe s"You have entered a total of $totalVolume litres of $productToken."
              content     should include(
                "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                  "They will calculate and take payment of the taxes and duties due."
              )
            }
        }

        val wineSparklingWineGroupMessage: String = "wine (this includes sparkling wine)."
        val ciderOtherAlcoholGroupMessage: String =
          "all other alcoholic drinks (including cider, port, sherry and alcohol up to 22%)."

        Seq(
          (
            "wine",
            "sparking wine",
            "alcohol/sparkling-wine",
            "75.001",
            s"90.001 litres of $wineSparklingWineGroupMessage"
          ),
          ("sparkling-wine", "wine", "alcohol/wine", "75.001", s"90.001 litres of $wineSparklingWineGroupMessage"),
          (
            "other",
            "cider",
            "alcohol/cider/non-sparkling-cider",
            "5.001",
            s"20.001 litres of $ciderOtherAlcoholGroupMessage"
          )
        ).foreach { case (productToken, previouslyAddedAlcohol, path, userInput, totalWithGroupMessage) =>
          s"load limit exceed page and display the group content for $productToken when $previouslyAddedAlcohol has been added" in {
            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    purchasedProductInstances = List(
                      purchasedProductInstance(
                        path = path,
                        iid = "iid1",
                        weightOrVolume = Some(10.0),
                        noOfSticks = None
                      )
                    ),
                    declarationResponse = declarationResponse(
                      oldPurchaseProductInstances = List(
                        purchasedProductInstance(
                          path = path,
                          iid = "iid0",
                          weightOrVolume = Some(5.0),
                          noOfSticks = None
                        )
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/$productToken/upper-limits/volume"
              ).withSession(s"user-amount-input-$productToken" -> userInput)
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc
              .getElementsByTag("h1")
              .text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text() shouldBe s"You have entered a total of $totalWithGroupMessage"
            content     should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }
      }
    }

    ".onPageLoadAddJourneyTobaccoWeight" when {
      "making a first declaration" should {
        Seq(
          ("rolling-tobacco", "rolling tobacco"),
          ("chewing-tobacco", "pipe or chewing tobacco")
        ).foreach { case (productToken, name) =>
          s"load limit exceed page and display the content specifically for $name" in {
            when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData())))

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/$productToken/upper-limits/weight"
              ).withSession(s"user-amount-input-$productToken" -> "1.5")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc.getElementsByTag("h1").text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text()                         shouldBe s"You have entered a total of 1500g of $name."
            content                             should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }

        "load limit exceed page and display the group content for rolling tobacco when chewing tobacco has been added" in {
          when(mockCache.fetch(any())).thenReturn(
            Future.successful(
              Some(
                journeyData(
                  purchasedProductInstances = List(
                    purchasedProductInstance(
                      path = "tobacco/chewing-tobacco",
                      iid = "iid0",
                      weightOrVolume = Some(1),
                      noOfSticks = None
                    )
                  )
                )
              )
            )
          )

          val result: Future[Result] = route(
            app,
            FakeRequest(
              "GET",
              "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/rolling-tobacco/upper-limits/weight"
            ).withSession("user-amount-input-rolling-tobacco" -> "0.11")
          ).get

          status(result) shouldBe OK

          val content: String = contentAsString(result)
          val doc: Document   = Jsoup.parse(content)

          doc
            .getElementsByTag("h1")
            .text() shouldBe "There is a problem"
          doc
            .getElementById("entered-amount")
            .text() shouldBe "You have entered a total of 1110g of loose tobacco."
          content     should include(
            "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
              "They will calculate and take payment of the taxes and duties due."
          )
        }

        "not load limit exceed page if the path is incorrect" in {
          when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData())))

          val result: Future[Result] = route(
            app,
            FakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/goods/zzz/yyy/upper-limits/weight")
          ).get

          status(result) shouldBe NOT_FOUND
        }
      }

      "making an amendment to a previous declaration" should {
        Seq(
          ("rolling-tobacco", "rolling tobacco"),
          ("chewing-tobacco", "pipe or chewing tobacco")
        ).foreach { case (productToken, name) =>
          s"load limit exceed page and display the content specifically for $name" in {
            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    declarationResponse = declarationResponse(
                      oldPurchaseProductInstances = List(
                        purchasedProductInstance(
                          path = s"tobacco/$productToken",
                          iid = "uFgxRU",
                          weightOrVolume = Some(0.5),
                          noOfSticks = None
                        )
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/$productToken/upper-limits/weight"
              ).withSession(s"user-amount-input-$productToken" -> "0.50001")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc.getElementsByTag("h1").text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text()                         shouldBe s"You have entered a total of 1000.01g of $name."
            content                             should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }

        "load limit exceed page and display the group content for rolling tobacco when chewing tobacco has been added" in {
          when(mockCache.fetch(any())).thenReturn(
            Future.successful(
              Some(
                journeyData(
                  purchasedProductInstances = List(
                    purchasedProductInstance(
                      path = "tobacco/chewing-tobacco",
                      iid = "iid1",
                      weightOrVolume = Some(0.4),
                      noOfSticks = None
                    )
                  ),
                  declarationResponse = declarationResponse(
                    oldPurchaseProductInstances = List(
                      purchasedProductInstance(
                        path = "tobacco/chewing-tobacco",
                        iid = "iid0",
                        weightOrVolume = Some(0.5),
                        noOfSticks = None
                      )
                    )
                  )
                )
              )
            )
          )

          val result: Future[Result] = route(
            app,
            FakeRequest(
              "GET",
              "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/rolling-tobacco/upper-limits/weight"
            ).withSession("user-amount-input-rolling-tobacco" -> "0.10001")
          ).get

          status(result) shouldBe OK

          val content: String = contentAsString(result)
          val doc: Document   = Jsoup.parse(content)

          doc
            .getElementsByTag("h1")
            .text() shouldBe "There is a problem"
          doc
            .getElementById("entered-amount")
            .text() shouldBe "You have entered a total of 1000.01g of loose tobacco."
          content     should include(
            "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
              "They will calculate and take payment of the taxes and duties due."
          )
        }
      }
    }

    ".onPageLoadAddJourneyNoOfSticks" when {
      "making a first declaration" should {
        Seq(
          ("cigars", "cigars"),
          ("cigarillos", "cigarillos"),
          ("cigarettes", "cigarettes"),
          ("heated-tobacco", "tobacco sticks")
        ).foreach { case (productToken, name) =>
          s"load limit exceed page and display the content specifically for $name" in {
            when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData())))

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/$productToken/upper-limits/units-of-product"
              ).withSession(s"user-amount-input-$productToken" -> "900")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc.getElementsByTag("h1").text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text()                         shouldBe s"You have entered a total of 900 $name."
            content                             should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }

        "not load limit exceed page if the path is incorrect" in {
          when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData())))

          val result: Future[Result] = route(
            app,
            FakeRequest(
              "GET",
              "/check-tax-on-goods-you-bring-into-the-uk/goods/zzz/yyyy/upper-limits/units-of-product"
            )
          ).get

          status(result) shouldBe NOT_FOUND
        }
      }

      "making an amendment to a previous declaration" should {
        Seq(
          ("cigars", "cigars", 200, 201),
          ("cigarillos", "cigarillos", 400, 401),
          ("cigarettes", "cigarettes", 800, 801),
          ("heated-tobacco", "tobacco sticks", 800, 801)
        ).foreach { case (productToken, name, previouslyEnteredSticks, totalSticks) =>
          s"load limit exceed page and display the content specifically for $name" in {
            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    declarationResponse = declarationResponse(
                      oldPurchaseProductInstances = List(
                        purchasedProductInstance(
                          path = s"tobacco/$productToken",
                          iid = "uFgxRU",
                          weightOrVolume = None,
                          noOfSticks = Some(previouslyEnteredSticks)
                        )
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/$productToken/upper-limits/units-of-product"
              ).withSession(s"user-amount-input-$productToken" -> "1")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc.getElementsByTag("h1").text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text()                         shouldBe s"You have entered a total of $totalSticks $name."
            content                             should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }
      }
    }

    ".onPageLoadEditAlcoholVolume" when {
      "making a first declaration" should {
        Seq("non-sparkling-cider", "sparkling-cider", "sparkling-cider-up").foreach { productToken =>
          s"load limit exceed page and display the content specifically for cider ($productToken)" in {
            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    purchasedProductInstances = List(
                      purchasedProductInstance(
                        path = s"alcohol/$productToken",
                        iid = "iid0",
                        weightOrVolume = Some(20.0),
                        noOfSticks = None
                      )
                    ),
                    workingInstance = Some(
                      purchasedProductInstance(
                        path = s"alcohol/$productToken",
                        iid = "iid0",
                        weightOrVolume = Some(20.0),
                        noOfSticks = None
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/cider/$productToken/upper-limits/iid0/edit/volume"
              ).withSession(s"user-amount-input-$productToken" -> "50.50")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc
              .getElementsByTag("h1")
              .text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text() shouldBe "You changed 20 litres of cider to 50.5 litres of cider."
            doc
              .getElementById("new-total-amount")
              .text() shouldBe "This means your total is now 50.5 litres of cider."
            content     should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }

        "load limit exceed page and display the group content for wine when sparkling wine has been added" in {
          when(mockCache.fetch(any())).thenReturn(
            Future.successful(
              Some(
                journeyData(
                  purchasedProductInstances = List(
                    purchasedProductInstance(
                      path = "alcohol/sparkling-wine",
                      iid = "iid0",
                      weightOrVolume = Some(30.0),
                      noOfSticks = None
                    ),
                    purchasedProductInstance(
                      path = "alcohol/wine",
                      iid = "iid1",
                      weightOrVolume = Some(40.0),
                      noOfSticks = None
                    )
                  ),
                  workingInstance = Some(
                    purchasedProductInstance(
                      path = "alcohol/wine",
                      iid = "iid1",
                      weightOrVolume = Some(40.0),
                      noOfSticks = None
                    )
                  )
                )
              )
            )
          )

          val result: Future[Result] = route(
            app,
            FakeRequest(
              "GET",
              "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/wine/upper-limits/iid1/edit/volume"
            ).withSession("user-amount-input-wine" -> "80.0")
          ).get

          status(result) shouldBe OK

          val content: String = contentAsString(result)
          val doc: Document   = Jsoup.parse(content)

          doc
            .getElementsByTag("h1")
            .text() shouldBe "There is a problem"
          doc
            .getElementById("entered-amount")
            .text() shouldBe "You changed 40 litres of wine to 80 litres of wine."
          doc
            .getElementById("new-total-amount")
            .text() shouldBe "This means your total is now 110 litres of wine (this includes sparkling wine)."
          content     should include(
            "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
              "They will calculate and take payment of the taxes and duties due."
          )
        }
      }

      "making an amendment to a previous declaration" should {
        Seq("non-sparkling-cider", "sparkling-cider", "sparkling-cider-up").foreach { productToken =>
          s"load limit exceed page and display the content specifically for cider ($productToken)" in {
            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    purchasedProductInstances = List(
                      purchasedProductInstance(
                        path = s"alcohol/$productToken",
                        iid = "iid1",
                        weightOrVolume = Some(10.0),
                        noOfSticks = None
                      )
                    ),
                    workingInstance = Some(
                      purchasedProductInstance(
                        path = s"alcohol/$productToken",
                        iid = "iid1",
                        weightOrVolume = Some(10.0),
                        noOfSticks = None
                      )
                    ),
                    declarationResponse = declarationResponse(
                      oldPurchaseProductInstances = List(
                        purchasedProductInstance(
                          path = s"alcohol/$productToken",
                          iid = "iid0",
                          weightOrVolume = Some(10.0),
                          noOfSticks = None
                        )
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/cider/$productToken/upper-limits/iid1/edit/volume"
              ).withSession(s"user-amount-input-$productToken" -> "10.001")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc
              .getElementsByTag("h1")
              .text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text() shouldBe "You changed 10 litres of cider to 10.001 litres of cider."
            doc
              .getElementById("new-total-amount")
              .text() shouldBe "This means your total is now 20.001 litres of cider."
            content     should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }

        "load limit exceed page and display the group content for wine when sparkling wine has been added" in {
          when(mockCache.fetch(any())).thenReturn(
            Future.successful(
              Some(
                journeyData(
                  purchasedProductInstances = List(
                    purchasedProductInstance(
                      path = "alcohol/sparkling-wine",
                      iid = "iid1",
                      weightOrVolume = Some(20.0),
                      noOfSticks = None
                    ),
                    purchasedProductInstance(
                      path = "alcohol/wine",
                      iid = "iid2",
                      weightOrVolume = Some(40.0),
                      noOfSticks = None
                    )
                  ),
                  workingInstance = Some(
                    purchasedProductInstance(
                      path = "alcohol/wine",
                      iid = "iid2",
                      weightOrVolume = Some(40.0),
                      noOfSticks = None
                    )
                  ),
                  declarationResponse = declarationResponse(
                    oldPurchaseProductInstances = List(
                      purchasedProductInstance(
                        path = s"alcohol/sparkling-wine",
                        iid = "iid0",
                        weightOrVolume = Some(20.0),
                        noOfSticks = None
                      )
                    )
                  )
                )
              )
            )
          )

          val result: Future[Result] = route(
            app,
            FakeRequest(
              "GET",
              "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/wine/upper-limits/iid2/edit/volume"
            ).withSession("user-amount-input-wine" -> "50.001")
          ).get

          status(result) shouldBe OK

          val content: String = contentAsString(result)
          val doc: Document   = Jsoup.parse(content)

          doc
            .getElementsByTag("h1")
            .text() shouldBe "There is a problem"
          doc
            .getElementById("entered-amount")
            .text() shouldBe "You changed 40 litres of wine to 50.001 litres of wine."
          doc
            .getElementById("new-total-amount")
            .text() shouldBe "This means your total is now 90.001 litres of wine (this includes sparkling wine)."
          content     should include(
            "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
              "They will calculate and take payment of the taxes and duties due."
          )
        }
      }
    }

    ".onPageLoadEditTobaccoWeight" when {
      "making a first declaration" should {
        Seq(
          ("rolling-tobacco", "rolling tobacco"),
          ("chewing-tobacco", "pipe or chewing tobacco")
        ).foreach { case (productToken, name) =>
          s"load limit exceed page and display the content specifically for $name" in {
            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    purchasedProductInstances = List(
                      purchasedProductInstance(
                        path = s"tobacco/$productToken",
                        iid = "iid0",
                        weightOrVolume = Some(0.9),
                        noOfSticks = None
                      )
                    ),
                    workingInstance = Some(
                      purchasedProductInstance(
                        path = s"tobacco/$productToken",
                        iid = "iid0",
                        weightOrVolume = Some(0.9),
                        noOfSticks = None
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/$productToken/upper-limits/iid0/edit/weight"
              ).withSession(s"user-amount-input-$productToken" -> "1.100")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc.getElementsByTag("h1").text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text()                         shouldBe s"You changed 900g of $name to 1100g of $name."
            doc
              .getElementById("new-total-amount")
              .text()                         shouldBe s"This means your total is now 1100g of $name."
            content                             should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }

        "load limit exceed page and display the group content for chewing tobacco when rolling tobacco has been added" in {
          when(mockCache.fetch(any())).thenReturn(
            Future.successful(
              Some(
                journeyData(
                  purchasedProductInstances = List(
                    purchasedProductInstance(
                      path = "tobacco/rolling-tobacco",
                      iid = "iid0",
                      weightOrVolume = Some(0.4),
                      noOfSticks = None
                    ),
                    purchasedProductInstance(
                      path = "tobacco/chewing-tobacco",
                      iid = "iid1",
                      weightOrVolume = Some(0.4),
                      noOfSticks = None
                    )
                  ),
                  workingInstance = Some(
                    purchasedProductInstance(
                      path = "tobacco/chewing-tobacco",
                      iid = "iid1",
                      weightOrVolume = Some(0.4),
                      noOfSticks = None
                    )
                  )
                )
              )
            )
          )

          val result: Future[Result] = route(
            app,
            FakeRequest(
              "GET",
              "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/chewing-tobacco/upper-limits/iid1/edit/weight"
            ).withSession("user-amount-input-chewing-tobacco" -> "1.20")
          ).get

          status(result) shouldBe OK

          val content: String = contentAsString(result)
          val doc: Document   = Jsoup.parse(content)

          doc.getElementsByTag("h1").text() shouldBe "There is a problem"
          doc
            .getElementById("entered-amount")
            .text()                         shouldBe "You changed 400g of pipe or chewing tobacco to 1200g of pipe or chewing tobacco."
          doc
            .getElementById("new-total-amount")
            .text()                         shouldBe "This means your total is now 1600g of loose tobacco."
          content                             should include(
            "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
              "They will calculate and take payment of the taxes and duties due."
          )
        }
      }

      "making an amendment to a previous declaration" should {
        Seq(
          ("rolling-tobacco", "rolling tobacco"),
          ("chewing-tobacco", "pipe or chewing tobacco")
        ).foreach { case (productToken, name) =>
          s"load limit exceed page and display the content specifically for $name" in {
            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    purchasedProductInstances = List(
                      purchasedProductInstance(
                        path = s"tobacco/$productToken",
                        iid = "iid1",
                        weightOrVolume = Some(0.9),
                        noOfSticks = None
                      )
                    ),
                    workingInstance = Some(
                      purchasedProductInstance(
                        path = s"tobacco/$productToken",
                        iid = "iid1",
                        weightOrVolume = Some(0.9),
                        noOfSticks = None
                      )
                    ),
                    declarationResponse = declarationResponse(
                      oldPurchaseProductInstances = List(
                        purchasedProductInstance(
                          path = s"tobacco/$productToken",
                          iid = "iid0",
                          weightOrVolume = Some(0.1),
                          noOfSticks = None
                        )
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/$productToken/upper-limits/iid1/edit/weight"
              ).withSession(s"user-amount-input-$productToken" -> "0.90001")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc.getElementsByTag("h1").text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text()                         shouldBe s"You changed 900g of $name to 900.01g of $name."
            doc
              .getElementById("new-total-amount")
              .text()                         shouldBe s"This means your total is now 1000.01g of $name."
            content                             should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }

        "load limit exceed page and display the group content for chewing tobacco when rolling tobacco has been added" in {
          when(mockCache.fetch(any())).thenReturn(
            Future.successful(
              Some(
                journeyData(
                  purchasedProductInstances = List(
                    purchasedProductInstance(
                      path = "tobacco/rolling-tobacco",
                      iid = "iid1",
                      weightOrVolume = Some(0.4),
                      noOfSticks = None
                    ),
                    purchasedProductInstance(
                      path = "tobacco/chewing-tobacco",
                      iid = "iid2",
                      weightOrVolume = Some(0.4),
                      noOfSticks = None
                    )
                  ),
                  workingInstance = Some(
                    purchasedProductInstance(
                      path = "tobacco/chewing-tobacco",
                      iid = "iid2",
                      weightOrVolume = Some(0.4),
                      noOfSticks = None
                    )
                  ),
                  declarationResponse = declarationResponse(
                    oldPurchaseProductInstances = List(
                      purchasedProductInstance(
                        path = s"tobacco/rolling-tobacco",
                        iid = "iid0",
                        weightOrVolume = Some(0.2),
                        noOfSticks = None
                      )
                    )
                  )
                )
              )
            )
          )

          val result: Future[Result] = route(
            app,
            FakeRequest(
              "GET",
              "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/chewing-tobacco/upper-limits/iid2/edit/weight"
            ).withSession("user-amount-input-chewing-tobacco" -> "0.40001")
          ).get

          status(result) shouldBe OK

          val content: String = contentAsString(result)
          val doc: Document   = Jsoup.parse(content)

          doc.getElementsByTag("h1").text() shouldBe "There is a problem"
          doc
            .getElementById("entered-amount")
            .text()                         shouldBe "You changed 400g of pipe or chewing tobacco to 400.01g of pipe or chewing tobacco."
          doc
            .getElementById("new-total-amount")
            .text()                         shouldBe "This means your total is now 1000.01g of loose tobacco."
          content                             should include(
            "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
              "They will calculate and take payment of the taxes and duties due."
          )
        }
      }
    }

    ".onPageLoadEditNoOfSticks" when {
      "making a first declaration" should {
        Seq(
          ("cigars", "cigars"),
          ("cigarillos", "cigarillos"),
          ("cigarettes", "cigarettes"),
          ("heated-tobacco", "tobacco sticks")
        ).foreach { case (productToken, name) =>
          s"load limit exceed page and display the content specifically for $name" in {
            val noOfSticks: Int = 150

            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    purchasedProductInstances = List(
                      purchasedProductInstance(
                        path = s"tobacco/$productToken",
                        iid = "iid0",
                        weightOrVolume = None,
                        noOfSticks = Some(noOfSticks)
                      )
                    ),
                    workingInstance = Some(
                      purchasedProductInstance(
                        path = s"tobacco/$productToken",
                        iid = "iid0",
                        weightOrVolume = None,
                        noOfSticks = Some(noOfSticks)
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/$productToken/upper-limits/iid0/edit/units-of-product"
              ).withSession(s"user-amount-input-$productToken" -> "801")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc.getElementsByTag("h1").text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text()                         shouldBe s"You changed 150 $name to 801 $name."
            doc
              .getElementById("new-total-amount")
              .text()                         shouldBe s"This means your total is now 801 $name."
            content                             should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }
      }

      "making an amendment to a previous declaration" should {
        Seq(
          ("cigars", "cigars"),
          ("cigarillos", "cigarillos"),
          ("cigarettes", "cigarettes"),
          ("heated-tobacco", "tobacco sticks")
        ).foreach { case (productToken, name) =>
          s"load limit exceed page and display the content specifically for $name" in {
            val noOfSticks: Int = 75

            when(mockCache.fetch(any())).thenReturn(
              Future.successful(
                Some(
                  journeyData(
                    purchasedProductInstances = List(
                      purchasedProductInstance(
                        path = s"tobacco/$productToken",
                        iid = "iid1",
                        weightOrVolume = None,
                        noOfSticks = Some(noOfSticks)
                      )
                    ),
                    workingInstance = Some(
                      purchasedProductInstance(
                        path = s"tobacco/$productToken",
                        iid = "iid1",
                        weightOrVolume = None,
                        noOfSticks = Some(noOfSticks)
                      )
                    ),
                    declarationResponse = declarationResponse(
                      oldPurchaseProductInstances = List(
                        purchasedProductInstance(
                          path = s"tobacco/$productToken",
                          iid = "iid0",
                          weightOrVolume = None,
                          noOfSticks = Some(noOfSticks)
                        )
                      )
                    )
                  )
                )
              )
            )

            val result: Future[Result] = route(
              app,
              FakeRequest(
                "GET",
                s"/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/$productToken/upper-limits/iid1/edit/units-of-product"
              ).withSession(s"user-amount-input-$productToken" -> "726")
            ).get

            status(result) shouldBe OK

            val content: String = contentAsString(result)
            val doc: Document   = Jsoup.parse(content)

            doc.getElementsByTag("h1").text() shouldBe "There is a problem"
            doc
              .getElementById("entered-amount")
              .text()                         shouldBe s"You changed 75 $name to 726 $name."
            doc
              .getElementById("new-total-amount")
              .text()                         shouldBe s"This means your total is now 801 $name."
            content                             should include(
              "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                "They will calculate and take payment of the taxes and duties due."
            )
          }
        }
      }
    }
  }
}
