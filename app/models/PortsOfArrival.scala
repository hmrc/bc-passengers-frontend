/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package models

import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}


case class PortsOfArrival(code: String, displayName: String, isGB: Boolean, portSynonyms: List[String]) {

  def toAutoCompleteJson(implicit messages: Messages): JsObject = Json.obj("code" -> code, "displayName" -> messages(displayName), "synonyms" -> portSynonyms)

}
