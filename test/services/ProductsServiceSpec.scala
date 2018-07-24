package services

import org.scalatest.Matchers
import services.ProductsService.{Branch, Leaf}
import uk.gov.hmrc.play.test.UnitSpec


class ProductsServiceSpec extends UnitSpec with Matchers {

  val tree =
    Branch("root", "NestedTree",
      List(
        Branch("cat1",
          "Category1",
          List(
            Leaf("leaf1", "someName","someRateID","someTemplateID")
          )
        ),
        Branch("cat2",
          "Category2",
          List(
            Leaf("leaf2", "someName","someRateID","someTemplateID"),
            Branch("subcat1",
              "SubCategory1",
              List(Leaf("leaf3", "someName","someRateID","someTemplateID"),
                Leaf("leaf4", "someOtherName","someOtherRateID","someOtherTemplateID"))
            ),
            Branch("subcat2",
              "SubCategory2",
              List(Leaf("leaf5", "someName","someRateID","someTemplateID"))
            )
          )
        )
      )
    )

  "Invoking getNode" should  {

    "Return a node from the product tree with a valid path" in {

      tree.getDescendant(List("cat1", "leaf1")) shouldBe Some(Leaf("leaf1","someName", "someRateID", "someTemplateID"))

      tree.getDescendant(List("cat1", "leaf1", "Not Used")) shouldBe Some(Leaf("leaf1", "someName", "someRateID", "someTemplateID"))

      tree.getDescendant(List("cat1")) shouldBe Some(Branch("cat1", "Category1", List(Leaf("leaf1", "someName", "someRateID", "someTemplateID"))))

      tree.getDescendant(List("cat2", "subcat2", "leaf5")) shouldBe Some(Leaf("leaf5", "someName", "someRateID", "someTemplateID"))
    }

    "Return None if the head of the path is invalid" in {

      tree.getDescendant(List("Not", "A", "Valid", "Path")) shouldBe None
    }
  }
}
