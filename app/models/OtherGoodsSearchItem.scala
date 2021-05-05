/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
object OtherGoodsSearchItem {
  implicit val formats = Json.format[OtherGoodsSearchItem]
}
case class OtherGoodsSearchItem(name: String, path: ProductPath){
  def toAutoCompleteJson(implicit messages: Messages): JsObject =  Json.obj("code" -> name, "displayName" -> messages(name), "synonyms" -> List[String]())
}
