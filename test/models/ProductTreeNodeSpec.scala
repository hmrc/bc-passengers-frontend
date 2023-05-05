/*
 * Copyright 2023 HM Revenue & Customs
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
          val productTreeLeaf          = ProductTreeLeaf("cigarettes", "Cigarettes", "TOB/A1/CIGRT", "cigarettes", Nil)
          val purchasedProductInstance = PurchasedProductInstance(
            ProductPath("tobacco/cigarettes"),
            "iid0",
            Some(1.54332),
            Some(noOfSticks),
            Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          )
          productTreeLeaf.isValid(purchasedProductInstance)                            shouldBe true
          productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = false) shouldBe Some(
            ("label.X_X", List(noOfSticks.toString, name))
          )
          productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = true)  shouldBe Some(
            ("label.X_X", List(noOfSticks.toString, name))
          )
        }

      Seq((1, "Cigarettes.single"), (2, "Cigarettes")).foreach(args => (test _).tupled(args))
    }

    "return the correct display description for cigars" which {
      def test(noOfSticks: Int, name: String): Unit =
        s"has a noOfSticks value of $noOfSticks" in {
          val productTreeLeaf          = ProductTreeLeaf("cigars", "Cigars", "TOB/A1/CIGAR", "cigars", Nil)
          val purchasedProductInstance = PurchasedProductInstance(
            ProductPath("tobacco/cigars"),
            "iid0",
            Some(1.54332),
            Some(noOfSticks),
            Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          )
          productTreeLeaf.isValid(purchasedProductInstance)                            shouldBe true
          productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = false) shouldBe Some(
            ("label.X_X", List(noOfSticks.toString, name))
          )
          productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long = true)  shouldBe Some(
            ("label.X_X_Xg", List(noOfSticks.toString, name, "1543.32"))
          )
        }

      Seq((1, "Cigars.single"), (2, "Cigars")).foreach(args => (test _).tupled(args))
    }
  }
}
