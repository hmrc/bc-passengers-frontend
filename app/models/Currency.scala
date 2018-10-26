package models

import play.api.libs.json.Json

object Currency {
  implicit val formats = Json.format[Currency]
}
case class Currency(code: String, displayName: String, value: Option[String])
