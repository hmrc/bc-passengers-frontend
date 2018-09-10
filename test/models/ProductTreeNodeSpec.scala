package models

import util.BaseSpec

class ProductTreeNodeSpec extends BaseSpec {

  "Calling ProductTreeLeaf.getDisplayWeight" should {

    "return the correct display weight" in {

      val productTreeLeaf = ProductTreeLeaf("cigars", "Cigars", "TOB/A1/CIGAR", "cigars")
      val purchasedProductInstance = PurchasedProductInstance(0, Some(1.54332), Some(50), Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDisplayWeight(purchasedProductInstance) shouldBe Some("1543.32g")
    }
  }


  "Calling ProductTreeLeaf.getDescription" should {

    "return the correct display description for tobacco" in {

      val productTreeLeaf = ProductTreeLeaf("chewing", "Chewing or pipe tobacco", "TOB/A1/OTHER", "tobacco")
      val purchasedProductInstance = PurchasedProductInstance(0, Some(1.54332), None, Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescription(purchasedProductInstance) shouldBe Some("1543.32g chewing or pipe tobacco")
    }

    "return the correct display description for alcohol" in {

      val productTreeLeaf = ProductTreeLeaf("beer", "Beer", "ALC/A2/BEER", "alcohol")
      val purchasedProductInstance = PurchasedProductInstance(0, Some(1.54332), None, Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescription(purchasedProductInstance) shouldBe Some("1.54332 litres beer")
    }

    "return the correct display description for other-goods" in {

      val productTreeLeaf = ProductTreeLeaf("childrens", "Children’s clothing", "OGD/CLTHS/CHILD", "other-goods")
      val purchasedProductInstance = PurchasedProductInstance(0, Some(1.54332), None, Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescription(purchasedProductInstance) shouldBe Some("Children’s clothing")
    }

    "return the correct display description for cigarettes" in {

      val productTreeLeaf = ProductTreeLeaf("cigarettes", "Cigarettes", "TOB/A1/CIGRT", "cigarettes")
      val purchasedProductInstance = PurchasedProductInstance(0, Some(1.54332), Some(20), Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescription(purchasedProductInstance) shouldBe Some("20 cigarettes")
    }

    "return the correct display description for cigars" in {

      val productTreeLeaf = ProductTreeLeaf("cigars", "Cigars", "TOB/A1/CIGAR", "cigars")
      val purchasedProductInstance = PurchasedProductInstance(0, Some(1.54332), Some(5), Some("AUD"), Some(BigDecimal(10.234)))
      productTreeLeaf.isValid(purchasedProductInstance) shouldBe true
      productTreeLeaf.getDescription(purchasedProductInstance) shouldBe Some("5 cigars")
    }
  }
}