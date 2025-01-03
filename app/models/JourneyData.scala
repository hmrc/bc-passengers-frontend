/*
 * Copyright 2024 HM Revenue & Customs
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

import ai.x.play.json.Encoders.encoder
import ai.x.play.json.Jsonx
import play.api.libs.json.{Json, OFormat}
import util.{parseLocalDate, parseLocalTime}

import java.time.{LocalDate, LocalTime}
object PurchasedProductInstance {
  implicit val formats: OFormat[PurchasedProductInstance] = Json.format[PurchasedProductInstance]
}
case class PurchasedProductInstance(
  path: ProductPath,
  iid: String,
  weightOrVolume: Option[BigDecimal] = None,
  noOfSticks: Option[Int] = None,
  country: Option[Country] = None,
  originCountry: Option[Country] = None,
  currency: Option[String] = None,
  cost: Option[BigDecimal] = None,
  searchTerm: Option[OtherGoodsSearchItem] = None,
  isVatPaid: Option[Boolean] = None,
  isCustomPaid: Option[Boolean] = None,
  isExcisePaid: Option[Boolean] = None,
  isUccRelief: Option[Boolean] = None,
  hasEvidence: Option[Boolean] = None,
  isEditable: Option[Boolean] = Some(true)
)

object UserInformation {
  implicit val formats: OFormat[UserInformation] = Json.format[UserInformation]

  def build(dto: EnterYourDetailsDto): UserInformation =
    UserInformation(
      dto.firstName,
      dto.lastName,
      dto.identification.identificationType.getOrElse(""),
      dto.identification.identificationNumber,
      dto.emailAddress.email,
      dto.placeOfArrival.selectPlaceOfArrival.getOrElse(""),
      dto.placeOfArrival.enterPlaceOfArrival.getOrElse(""),
      parseLocalDate(dto.dateTimeOfArrival.dateOfArrival),
      parseLocalTime(dto.dateTimeOfArrival.timeOfArrival)
    )
}
case class UserInformation(
  firstName: String,
  lastName: String,
  identificationType: String,
  identificationNumber: String,
  emailAddress: String,
  selectPlaceOfArrival: String,
  enterPlaceOfArrival: String,
  dateOfArrival: LocalDate,
  timeOfArrival: LocalTime
)

object PreviousDeclarationRequest {
  implicit val formats: OFormat[PreviousDeclarationRequest] = Json.format[PreviousDeclarationRequest]

  def build(dto: DeclarationRetrievalDto): PreviousDeclarationRequest =
    PreviousDeclarationRequest(dto.lastName, dto.referenceNumber.toUpperCase)
}
case class PreviousDeclarationRequest(
  lastName: String,
  referenceNumber: String
)

object ProductAlias {
  implicit val formats: OFormat[ProductAlias] = Json.format[ProductAlias]
}

case class ProductAlias(term: String, productPath: ProductPath)

object PaymentNotification {
  implicit val formats: OFormat[PaymentNotification] = Json.format[PaymentNotification]
}

case class PaymentNotification(status: String, reference: String)

object JourneyData {
  implicit val format: OFormat[JourneyData] = Json.format[JourneyData]
}

case class JourneyData(
  prevDeclaration: Option[Boolean] = None,
  euCountryCheck: Option[String] = None,
  arrivingNICheck: Option[Boolean] = None,
  isUKVatPaid: Option[Boolean] = None,
  isUKVatExcisePaid: Option[Boolean] = None,
  isUKResident: Option[Boolean] = None,
  isUccRelief: Option[Boolean] = None,
  isVatResClaimed: Option[Boolean] = None,
  isBringingDutyFree: Option[Boolean] = None,
  bringingOverAllowance: Option[Boolean] = None,
  privateCraft: Option[Boolean] = None,
  ageOver17: Option[Boolean] = None,
  irishBorder: Option[Boolean] = None,
  selectedAliases: List[ProductAlias] = Nil,
  purchasedProductInstances: List[PurchasedProductInstance] = Nil,
  workingInstance: Option[PurchasedProductInstance] = None,
  userInformation: Option[UserInformation] = None,
  calculatorResponse: Option[CalculatorResponse] = None,
  chargeReference: Option[String] = None,
  defaultCountry: Option[String] = None,
  defaultOriginCountry: Option[String] = None,
  defaultCurrency: Option[String] = None,
  previousDeclarationRequest: Option[PreviousDeclarationRequest] = None,
  declarationResponse: Option[DeclarationResponse] = None,
  deltaCalculation: Option[Calculation] = None,
  amendmentCount: Option[Int] = None,
  pendingPayment: Option[Boolean] = None,
  amendState: Option[String] = None
) {

  val selectedProducts: List[List[String]] = selectedAliases.map(_.productPath.components)

  def allCurrencyCodes(purchasedProductInstances: List[PurchasedProductInstance]): Set[String] = (for {
    purchasedProductInstances <- purchasedProductInstances
    currencyCode              <- purchasedProductInstances.currency
  } yield currencyCode).toSet

  def getPurchasedProductInstance(iid: String): Option[PurchasedProductInstance] =
    purchasedProductInstances.find(_.iid == iid)

  def getOrCreatePurchasedProductInstance(path: ProductPath, iid: String): PurchasedProductInstance =
    purchasedProductInstances.find(_.iid == iid).getOrElse(PurchasedProductInstance(path, iid))

  def revertPurchasedProductInstance(): JourneyData = {
    val workingInstance = this.workingInstance
    if (workingInstance.isDefined) {
      val ppis = this.purchasedProductInstances.map { ppi =>
        if (ppi.iid == workingInstance.get.iid) {
          workingInstance.get
        } else {
          ppi
        }
      }
      this.copy(purchasedProductInstances = ppis)
    } else {
      this
    }
  }

  def removePurchasedProductInstance(iid: String): JourneyData =
    this.copy(purchasedProductInstances = purchasedProductInstances.filterNot(_.iid == iid))

  def clearingWorking: JourneyData = this.copy(workingInstance = None)
}
