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

package binders

import models.ProductPath
import play.api.mvc.PathBindable
import util.BaseSpec

class BindersSpec extends BaseSpec {

  private val binder: PathBindable[ProductPath] = Binders.productPathBinder

  "Binders" when {
    "productPathBinder" should {
      "bind a valid string value" which {
        "matches the regex" in {
          binder.bind("hello", "there") shouldBe Right(ProductPath(List("there")))
        }
      }

      "fail to bind an invalid string value" which {
        "that does not match the regex" in {
          binder.bind("hello", "there?") shouldBe Left("Invalid product path component")
        }
      }

      "unbind ProductPath" in {
        binder.unbind("hello", ProductPath(List("there"))) shouldBe "there"
      }
    }
  }
}
