/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}

object Country {
  implicit val formats = Json.format[Country]
}
case class Country(code: String, countryName: String, alphaTwoCode: String, isEu: Boolean, isCountry: Boolean, countrySynonyms: List[String]) {

  def toAutoCompleteJson(implicit messages: Messages): JsObject = Json.obj("code" -> code, "displayName" -> messages(countryName), "synonyms" -> countrySynonyms)
}
