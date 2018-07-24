package models

import play.api.libs.json.Json


object JourneyData {
  implicit val formats = Json.format[JourneyData]
}

case class JourneyData(country: Option[String] = None, ageOver17: Option[Boolean] = None, privateCraft: Option[Boolean] = None, selectedProducts: Option[List[List[String]]] = None)
