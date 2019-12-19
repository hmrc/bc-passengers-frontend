package models

import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}

case class OtherGoodsSearchItem(name: String, path: ProductPath){
  def toAutoCompleteJson(implicit messages: Messages): JsObject =  Json.obj("code" -> name, "displayName" -> messages(name), "synonyms" -> List[String]())
}