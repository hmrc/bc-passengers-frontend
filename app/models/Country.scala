package models

import play.api.libs.json.Json

object Country {
  implicit val formats = Json.format[Country]
}
case class Country(countryName: String, alphaTwoCode: String, isEu: Boolean, currencyCode: Option[String])