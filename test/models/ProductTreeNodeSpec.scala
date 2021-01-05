/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import play.api.i18n.{Messages, MessagesApi}
import util.BaseSpec

class ProductTreeNodeSpec extends BaseSpec {

  "Calling ProductTreeLeaf.getDescription" should {

    implicit val messages: Messages = injected[MessagesApi].preferred(EnhancedFakeRequest("POST", "/nowhere")(app))

    "return the correct display description for tobacco" in {

      val productTreeLeaf = ProductTreeLeaf("chewing", "Chewing or pipe tobacco", "TOB/A1/OTHER", "tobacco", Nil)
      val purchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/chewing"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = false) shouldBe Some(("label.Xg_of_X", List("1543.32", "chewing or pipe tobacco")))
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = true) shouldBe Some(("label.Xg_of_X", List("1543.32", "chewing or pipe tobacco")))
    }

    "return the correct display description for alcohol" in {

      val productTreeLeaf = ProductTreeLeaf("beer", "Beer", "ALC/A2/BEER", "alcohol", Nil)
      val purchasedProductInstance = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = false) shouldBe Some(("label.X_litres_X", List("1.54332", "beer")))
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = true) shouldBe Some(("label.X_litres_X", List("1.54332", "beer")))
    }

    "return the correct display description for other-goods" in {

      val productTreeLeaf = ProductTreeLeaf("childrens", "Children’s clothing", "OGD/CLTHS/CHILD", "other-goods", Nil)
      val purchasedProductInstance = PurchasedProductInstance(ProductPath("other-goods/childrens"), "iid0", Some(1.54332), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = false) shouldBe Some(("Children’s clothing", List()))
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = true) shouldBe Some(("Children’s clothing", List()))
    }

    "return the correct display description for cigarettes" in {

      val productTreeLeaf = ProductTreeLeaf("cigarettes", "Cigarettes", "TOB/A1/CIGRT", "cigarettes", Nil)
      val purchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0", Some(1.54332), Some(20), Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = false) shouldBe Some(("label.X_X", List("20", "cigarettes")))
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = true) shouldBe Some(("label.X_X", List("20", "cigarettes")))
    }

    "return the correct display description for cigars" in {

      val productTreeLeaf = ProductTreeLeaf("cigars", "Cigars", "TOB/A1/CIGAR", "cigars", Nil)
      val purchasedProductInstance = PurchasedProductInstance(ProductPath("tobacco/cigars"),"iid0", Some(1.54332), Some(5), Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = false) shouldBe Some(("label.X_X", List("5", "cigars")))
      productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long = true) shouldBe Some(("label.X_X_Xg", List("5", "cigars", "1543.32")))
    }
  }
}
