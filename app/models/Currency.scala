/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package models

import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}

object Currency {
  implicit val formats = Json.format[Currency]
}
case class Currency(code: String, displayName: String, valueForConversion: Option[String], currencySynonyms: List[String]) {

  def toAutoCompleteJson(implicit messages: Messages): JsObject = Json.obj("code" -> code, "displayName" -> messages(displayName), "synonyms" -> currencySynonyms)

}
