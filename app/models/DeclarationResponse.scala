/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
  amendmentCount: Option[Int] = Some(0)
)

case class LiabilityDetails(
  totalExciseGBP: String,
  totalCustomsGBP: String,
  totalVATGBP: String,
  grandTotalGBP: String
)
