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

package models

import util.BaseSpec

class ProductTreeNodeSpec extends BaseSpec {

  "Calling ProductTreeLeaf.getDescription" should {

    "return the correct display description for tobacco" in {

      val productTreeLeaf          = ProductTreeLeaf("chewing", "Chewing or pipe tobacco", "TOB/A1/OTHER", "tobacco", Nil)
      val purchasedProductInstance = PurchasedProductInstance(
        ProductPath("tobacco/chewing"),
        "iid0",
        Some(1.54332),
        None,
        Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
        None,
        Some("AUD"),
        Some(BigDecimal(10.234))
      )
      productTreeLeaf.isValid(purchasedProductInstance)                            shouldBe true
      productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = false) shouldBe Some(
        ("label.Xg_of_X", List("1543.32", "Chewing or pipe tobacco"))
      )
      productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = true)  shouldBe Some(
        ("label.Xg_of_X", List("1543.32", "Chewing or pipe tobacco"))
      )
    }

    "return the correct display description for alcohol" in {

      val productTreeLeaf          = ProductTreeLeaf("beer", "Beer", "ALC/A2/BEER", "alcohol", Nil)
      val purchasedProductInstance = PurchasedProductInstance(
        ProductPath("alcohol/beer"),
        "iid0",
        Some(1.54332),
        None,
        Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
        None,
        Some("AUD"),
        Some(BigDecimal(10.234))
      )
      productTreeLeaf.isValid(purchasedProductInstance)                            shouldBe true
      productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = false) shouldBe Some(
        ("label.X_litres_X", List("1.54332", "Beer"))
      )
      productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = true)  shouldBe Some(
        ("label.X_litres_X", List("1.54332", "Beer"))
      )
    }

    "return the correct display description for other-goods" in {

      val productTreeLeaf          = ProductTreeLeaf("childrens", "Children’s clothing", "OGD/CLTHS/CHILD", "other-goods", Nil)
      val purchasedProductInstance = PurchasedProductInstance(
        ProductPath("other-goods/childrens"),
        "iid0",
        Some(1.54332),
        None,
        Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
        None,
        Some("AUD"),
        Some(BigDecimal(10.234))
      )
      productTreeLeaf.isValid(purchasedProductInstance)                            shouldBe true
      productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = false) shouldBe Some(
        ("Children’s clothing", List())
      )
      productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = true)  shouldBe Some(
        ("Children’s clothing", List())
      )
    }

    "return the correct display description for cigarettes" which {
      def test(noOfSticks: Int, name: String): Unit =
        s"has a noOfSticks value of $noOfSticks" in {
          val productTreeLeaf: ProductTreeLeaf                   = ProductTreeLeaf(
            token = "cigarettes",
            name = "label.tobacco.cigarettes",
            rateID = "TOB/A1/CIGRT",
            templateId = "cigarettes",
            applicableLimits = List("L-CIGRT")
          )
          val purchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
            path = ProductPath(path = "tobacco/cigarettes"),
            iid = "iid0",
            noOfSticks = Some(noOfSticks),
            country = Some(
              Country(
                code = "EG",
                countryName = "title.egypt",
                alphaTwoCode = "EG",
                isEu = false,
                isCountry = true,
                countrySynonyms = Nil
              )
            ),
            currency = Some("EGP"),
            cost = Some(BigDecimal(10.234))
          )

          productTreeLeaf.isValid(purchasedProductInstance)                            shouldBe true
          productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = false) shouldBe Some(
            ("label.X_X", List(noOfSticks.toString, name))
          )
          productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = true)  shouldBe Some(
            ("label.X_X", List(noOfSticks.toString, name))
          )
        }

      Seq((1, "label.tobacco.cigarettes.single"), (2, "label.tobacco.cigarettes")).foreach(args => test.tupled(args))
    }

    "return the correct display description for cigars" which {
      def test(noOfSticks: Int, name: String): Unit =
        s"has a noOfSticks value of $noOfSticks" in {
          val productTreeLeaf: ProductTreeLeaf                   = ProductTreeLeaf(
            token = "cigars",
            name = "label.tobacco.cigars",
            rateID = "TOB/A1/CIGAR",
            templateId = "cigars",
            applicableLimits = List("L-CIGAR")
          )
          val purchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
            path = ProductPath(path = "tobacco/cigars"),
            iid = "iid0",
            weightOrVolume = Some(1.54332),
            noOfSticks = Some(noOfSticks),
            country = Some(
              Country(
                code = "EG",
                countryName = "title.egypt",
                alphaTwoCode = "EG",
                isEu = false,
                isCountry = true,
                countrySynonyms = Nil
              )
            ),
            currency = Some("EGP"),
            cost = Some(BigDecimal(10.234))
          )

          productTreeLeaf.isValid(purchasedProductInstance)                            shouldBe true
          productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = false) shouldBe Some(
            ("label.X_X", List(noOfSticks.toString, name))
          )
          productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = true)  shouldBe Some(
            ("label.X_X_Xg", List(noOfSticks.toString, name, "1543.32"))
          )
        }

      Seq((1, "label.tobacco.cigars.single"), (2, "label.tobacco.cigars")).foreach(args => test.tupled(args))
    }
  }

  "Calling ProductTreeLeaf.isValid" should {
    "return false for an invalid templateId" in {
      val productTreeLeaf: ProductTreeLeaf                   = ProductTreeLeaf(
        token = "chewing-tobacco",
        name = "label.tobacco.chewing-tobacco",
        rateID = "TOB/A1/OTHER",
        templateId = "hello",
        applicableLimits = List("L-LOOSE")
      )
      val purchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
        path = ProductPath(path = "tobacco/chewing-tobacco"),
        iid = "iid0"
      )

      productTreeLeaf.isValid(purchasedProductInstance) shouldBe false
    }
  }

  "Calling ProductTreeBranch.isLeaf" should {
    "return false" in {
      val productTreeBranch: ProductTreeBranch = ProductTreeBranch(
        token = "cat1",
        name = "Category1",
        children = List(
          ProductTreeLeaf(
            token = "leaf1",
            name = "someName",
            rateID = "someRateID",
            templateId = "someTemplateID",
            applicableLimits = Nil
          )
        )
      )

      productTreeBranch.isLeaf shouldBe false
    }
  }
}
