/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}

case class ProductListEntry(productPath: ProductPath, productTreeLeaf: ProductTreeLeaf){

  def toAutoCompleteJson(implicit messages: Messages): JsObject = Json.obj("code" -> productPath, "displayName" -> messages(productTreeLeaf.name))
}
