package models

import play.api.libs.json.{JsObject, Json}

object Country {
  implicit val formats = Json.format[Country]
}
case class Country(countryName: String, alphaTwoCode: String, isEu: Boolean, currencyCode: Option[String], countrySynonyms: List[String]) {

  def toAutoCompleteJson: JsObject = Json.obj("countryName" -> countryName, "countrySynonyms" -> countrySynonyms)
}