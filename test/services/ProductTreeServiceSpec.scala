/*
 * Copyright 2025 HM Revenue & Customs
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

import models.*
import util.BaseSpec

class ProductTreeServiceSpec extends BaseSpec {

  val tree: ProductTreeBranch =
    ProductTreeBranch(
      "root",
      "NestedTree",
      List(
        ProductTreeBranch(
          "cat1",
          "Category1",
          List(
            ProductTreeLeaf("leaf1", "someName", "someRateID", "someTemplateID", Nil)
          )
        ),
        ProductTreeBranch(
          "cat2",
          "Category2",
          List(
            ProductTreeLeaf("leaf2", "someName", "someRateID", "someTemplateID", Nil),
            ProductTreeBranch(
              "subcat1",
              "SubCategory1",
              List(
                ProductTreeLeaf("leaf3", "someName", "someRateID", "someTemplateID", Nil),
                ProductTreeLeaf("leaf4", "someOtherName", "someOtherRateID", "someOtherTemplateID", Nil)
              )
            ),
            ProductTreeBranch(
              "subcat2",
              "SubCategory2",
              List(ProductTreeLeaf("leaf5", "someName", "someRateID", "someTemplateID", Nil))
            )
          )
        )
      )
    )

  "Invoking getNode" should {

    "Return a node from the product tree with a valid path" in {

      tree.getDescendant(ProductPath(List("cat1", "leaf1"))) shouldBe Some(
        ProductTreeLeaf("leaf1", "someName", "someRateID", "someTemplateID", Nil)
      )

      tree.getDescendant(ProductPath(List("cat1", "leaf1", "Not Used"))) shouldBe Some(
        ProductTreeLeaf("leaf1", "someName", "someRateID", "someTemplateID", Nil)
      )

      tree.getDescendant(ProductPath(List("cat1"))) shouldBe Some(
        ProductTreeBranch(
          "cat1",
          "Category1",
          List(ProductTreeLeaf("leaf1", "someName", "someRateID", "someTemplateID", Nil))
        )
      )

      tree.getDescendant(ProductPath(List("cat2", "subcat2", "leaf5"))) shouldBe Some(
        ProductTreeLeaf("leaf5", "someName", "someRateID", "someTemplateID", Nil)
      )
    }

    "Return None if the head of the path is invalid" in {

      tree.getDescendant(ProductPath(List("Not", "A", "Valid", "Path"))) shouldBe None
    }
  }

  "Calling getOtherGoodsSearchItems" should {
    "Return a list of other-goods search items which are all present in the productTree" in {

      val productTreeService = injected[ProductTreeService]

      val items: List[OtherGoodsSearchItem] = productTreeService.otherGoodsSearchItems

      for (item <- items) {
        val descendant: Option[ProductTreeNode] = productTreeService.productTree.getDescendant(item.path)
        assert(descendant.isDefined, item.path.toString + " was not defined in the product tree")
      }
    }

  }

}
