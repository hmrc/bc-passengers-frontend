package models

case class Country(countryName: String, alphaTwoCode: String, isEu: Boolean, currencyCode: Option[String])
