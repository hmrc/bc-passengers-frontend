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

package models

import play.api.libs.json.*

case class ProductPath(components: List[String]) {
  override def toString: String                       = components.mkString("/")
  def addingComponent(component: String): ProductPath = ProductPath(components :+ component)
  def toMessageKey: String                            = components.mkString(".")
}

object ProductPath {
  def apply(path: String): ProductPath = ProductPath(path.split("/").toList)

  given formats: Format[ProductPath] = new Format[ProductPath] {
    override def writes(o: ProductPath): JsValue             = JsString(o.toString)
    override def reads(json: JsValue): JsResult[ProductPath] = json match {
      case JsString(value) => JsSuccess[ProductPath](ProductPath(value))
      case _               => JsError("Invalid ProductPath json")
    }
  }
}
