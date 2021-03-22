/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import play.api.libs.json.{Json, OFormat}

object DeclarationResponse {
  implicit val formats: OFormat[DeclarationResponse] = Json.format[DeclarationResponse]
}

case class DeclarationResponse(
                               calculation: Calculation,
                               oldPurchaseProductInstances: List[PurchasedProductInstance]
)
