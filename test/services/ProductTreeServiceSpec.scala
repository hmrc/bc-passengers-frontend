package services

import models.{ProductPath, ProductTreeBranch, ProductTreeLeaf}
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec


class ProductTreeServiceSpec extends UnitSpec with Matchers {

  val tree =
    ProductTreeBranch("root", "NestedTree",
      List(
        ProductTreeBranch("cat1",
          "Category1",
          List(
            ProductTreeLeaf("leaf1", "someName","someRateID","someTemplateID")
          )
        ),
        ProductTreeBranch("cat2",
          "Category2",
          List(
            ProductTreeLeaf("leaf2", "someName","someRateID","someTemplateID"),
            ProductTreeBranch("subcat1",
              "SubCategory1",
              List(ProductTreeLeaf("leaf3", "someName","someRateID","someTemplateID"),
                ProductTreeLeaf("leaf4", "someOtherName","someOtherRateID","someOtherTemplateID"))
            ),
            ProductTreeBranch("subcat2",
              "SubCategory2",
              List(ProductTreeLeaf("leaf5", "someName","someRateID","someTemplateID"))
            )
          )
        )
      )
    )

  "Invoking getNode" should  {

    "Return a node from the product tree with a valid path" in {

      tree.getDescendant(ProductPath(List("cat1", "leaf1"))) shouldBe Some(ProductTreeLeaf("leaf1","someName", "someRateID", "someTemplateID"))

      tree.getDescendant(ProductPath(List("cat1", "leaf1", "Not Used"))) shouldBe Some(ProductTreeLeaf("leaf1", "someName", "someRateID", "someTemplateID"))

      tree.getDescendant(ProductPath(List("cat1"))) shouldBe Some(ProductTreeBranch("cat1", "Category1", List(ProductTreeLeaf("leaf1", "someName", "someRateID", "someTemplateID"))))

      tree.getDescendant(ProductPath(List("cat2", "subcat2", "leaf5"))) shouldBe Some(ProductTreeLeaf("leaf5", "someName", "someRateID", "someTemplateID"))
    }

    "Return None if the head of the path is invalid" in {

      tree.getDescendant(ProductPath(List("Not", "A", "Valid", "Path"))) shouldBe None
    }
  }
}
