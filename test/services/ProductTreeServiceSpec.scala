/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package services

import models.{OtherGoodsSearchItem, ProductPath, ProductTreeBranch, ProductTreeLeaf, ProductTreeNode}
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec
import util.BaseSpec


class ProductTreeServiceSpec extends BaseSpec {

  val tree =
    ProductTreeBranch("root", "NestedTree",
      List(
        ProductTreeBranch("cat1",
          "Category1",
          List(
            ProductTreeLeaf("leaf1", "someName","someRateID","someTemplateID", Nil)
          )
        ),
        ProductTreeBranch("cat2",
          "Category2",
          List(
            ProductTreeLeaf("leaf2", "someName","someRateID","someTemplateID", Nil),
            ProductTreeBranch("subcat1",
              "SubCategory1",
              List(ProductTreeLeaf("leaf3", "someName","someRateID","someTemplateID", Nil),
                ProductTreeLeaf("leaf4", "someOtherName","someOtherRateID","someOtherTemplateID", Nil))
            ),
            ProductTreeBranch("subcat2",
              "SubCategory2",
              List(ProductTreeLeaf("leaf5", "someName","someRateID","someTemplateID", Nil))
            )
          )
        )
      )
    )

  "Invoking getNode" should  {

    "Return a node from the product tree with a valid path" in {

      tree.getDescendant(ProductPath(List("cat1", "leaf1"))) shouldBe Some(ProductTreeLeaf("leaf1","someName", "someRateID", "someTemplateID", Nil))

      tree.getDescendant(ProductPath(List("cat1", "leaf1", "Not Used"))) shouldBe Some(ProductTreeLeaf("leaf1", "someName", "someRateID", "someTemplateID", Nil))

      tree.getDescendant(ProductPath(List("cat1"))) shouldBe Some(ProductTreeBranch("cat1", "Category1", List(ProductTreeLeaf("leaf1", "someName", "someRateID", "someTemplateID", Nil))))

      tree.getDescendant(ProductPath(List("cat2", "subcat2", "leaf5"))) shouldBe Some(ProductTreeLeaf("leaf5", "someName", "someRateID", "someTemplateID", Nil))
    }

    "Return None if the head of the path is invalid" in {

      tree.getDescendant(ProductPath(List("Not", "A", "Valid", "Path"))) shouldBe None
    }
  }

  "Calling getOtherGoodsSearchItems" should {
    "Return a list of other-goods search items which are all present in the productTree" in {

      val productTreeService = injected[ProductTreeService]

      val items: List[OtherGoodsSearchItem] = productTreeService.otherGoodsSearchItems

      for(item <- items) {
        val descendant: Option[ProductTreeNode] = productTreeService.productTree.getDescendant(item.path)
        assert(descendant.isDefined, item.path + " was not defined in the product tree")
      }
    }

  }

}
