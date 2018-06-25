package services

import org.scalatest.Matchers
import services.ProductsService.{Branch, Leaf}
import uk.gov.hmrc.play.test.UnitSpec


class ProductsServiceSpec extends UnitSpec with Matchers {

  val tree =
    Branch("NestedTree",
      List(
        Branch(
          "Category1",
          List(
            Leaf("someName","someRateID","someTemplateID")
          )
        ),
        Branch(
          "Category2",
          List(
            Leaf("someName","someRateID","someTemplateID"),
            Branch(
              "SubCategory1",
              List(Leaf("someName","someRateID","someTemplateID"),
                Leaf("someOtherName","someOtherRateID","someOtherTemplateID"))
            ),
            Branch(
              "SubCategory2",
              List(Leaf("someName","someRateID","someTemplateID"))
            )
          )
        )
      )
    )

  "Invoking getNode" should  {

    "Return a node from the product tree with a valid path" in {

      tree.getDescendant(List("Category1", "someName")) shouldBe Some(Leaf("someName", "someRateID", "someTemplateID"))

      tree.getDescendant(List("Category1", "someName", "Not Used")) shouldBe Some(Leaf("someName", "someRateID", "someTemplateID"))

      tree.getDescendant(List("Category1")) shouldBe Some(Branch("Category1", List(Leaf("someName", "someRateID", "someTemplateID"))))

      tree.getDescendant(List("Category2", "SubCategory1", "someName")) shouldBe Some(Leaf("someName", "someRateID", "someTemplateID"))
    }

    "Return None if the head of the path is invalid" in {

      tree.getDescendant(List("Not", "A", "Valid", "Path")) shouldBe None
    }
  }
}
