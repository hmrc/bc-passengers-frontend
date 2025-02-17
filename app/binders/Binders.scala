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

package binders

import models.ProductPath
import play.api.mvc.{JavascriptLiteral, PathBindable}

object Binders {

  implicit val productPathBinder: PathBindable[ProductPath] = new PathBindable[ProductPath] {

    override def bind(key: String, value: String): Either[String, ProductPath] = {
      val re = """^[a-z-/]+$""".r
      value match {
        case re() => Right(ProductPath(value.split("/").toList))
        case _    => Left("Invalid product path component")
      }
    }

    override def unbind(key: String, value: ProductPath): String =
      value.toString
  }

  given productPathJSLBinder: JavascriptLiteral[ProductPath] = new JavascriptLiteral[ProductPath] {
    override def to(value: ProductPath): String = s"""'${value.toString}'"""
  }

}
