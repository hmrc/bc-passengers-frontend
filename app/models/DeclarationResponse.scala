/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import play.api.libs.json.{Json, OFormat}

object DeclarationResponse {
  implicit val formats: OFormat[DeclarationResponse] = Json.format[DeclarationResponse]
}

object LiabilityDetails {
  implicit val formats: OFormat[LiabilityDetails] = Json.format[LiabilityDetails]
}

case class DeclarationResponse(
                                calculation: Calculation,
                                liabilityDetails: LiabilityDetails,
                                oldPurchaseProductInstances: List[PurchasedProductInstance],
                                amendCount: Int = 0
                              )

case class LiabilityDetails(
                             totalExciseGBP: String,
                             totalCustomsGBP: String,
                             totalVATGBP: String,
                             grandTotalGBP: String
                           )