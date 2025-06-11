/*
 * Copyright 2025 HM Revenue & Customs
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
import util.{parseLocalDate, parseLocalTime}

import java.time.{LocalDate, LocalTime}

object PurchasedProductInstance {
  given formats: OFormat[PurchasedProductInstance] = Json.format[PurchasedProductInstance]
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

object UserInformation {
  implicit val formats: OFormat[UserInformation] = Json.format[UserInformation]

  def build(pre: PreUserInformation): UserInformation =
    UserInformation(
      pre.nameForm.firstName,
      pre.nameForm.lastName,
      pre.identification.map(_.identificationType).getOrElse(""),
      pre.identification.flatMap(_.identificationNumber).getOrElse(""),
      pre.emailAddress.getOrElse(""),
      pre.arrivalForm.map(_.selectPlaceOfArrival).getOrElse(""),
      pre.arrivalForm.map(_.enterPlaceOfArrival).getOrElse(""),
      pre.arrivalForm.map(arrival => parseLocalDate(arrival.dateOfArrival.toString)).getOrElse(LocalDate.now()),
      pre.arrivalForm.map(arrival => parseLocalTime(arrival.timeOfArrival.toString)).getOrElse(LocalTime.now())
    )

  def getPreUser(userInfo: UserInformation): PreUserInformation =
    PreUserInformation(
      nameForm = WhatIsYourNameForm(
        firstName = userInfo.firstName,
        lastName = userInfo.lastName
      ),
      identification = Some(
        IdentificationForm(
          identificationType = userInfo.identificationType,
          identificationNumber = Option(userInfo.identificationNumber)
        )
      ),
      emailAddress = Some(userInfo.emailAddress),
      arrivalForm = Some(
        ArrivalForm(
          selectPlaceOfArrival = userInfo.selectPlaceOfArrival,
          enterPlaceOfArrival = userInfo.enterPlaceOfArrival,
          dateOfArrival = userInfo.dateOfArrival,
          timeOfArrival = userInfo.timeOfArrival
        )
      )
    )
}

case class ArrivalForm(
  selectPlaceOfArrival: String,
  enterPlaceOfArrival: String,
  dateOfArrival: LocalDate,
  timeOfArrival: LocalTime
)

object ArrivalForm {
  implicit val formats: OFormat[ArrivalForm] = Json.format[ArrivalForm]
}

case class PreUserInformation(
  nameForm: WhatIsYourNameForm,
  identification: Option[IdentificationForm] = None,
  emailAddress: Option[String] = None,
  arrivalForm: Option[ArrivalForm] = None
) {

  def buildUserInfo: Option[UserInformation] =
    identification.flatMap(id =>
      emailAddress.flatMap(email =>
        arrivalForm.map(arrival =>
          UserInformation(
            firstName = nameForm.firstName,
            lastName = nameForm.lastName,
            identificationType = id.identificationType.toString,
            identificationNumber = id.identificationNumber.getOrElse(""),
            emailAddress = email,
            selectPlaceOfArrival = arrival.selectPlaceOfArrival,
            enterPlaceOfArrival = arrival.enterPlaceOfArrival,
            dateOfArrival = arrival.dateOfArrival,
            timeOfArrival = arrival.timeOfArrival
          )
        )
      )
    )
}

object PreUserInformation {
  implicit val formats: OFormat[PreUserInformation] = Json.format[PreUserInformation]

  def fromWhatIsYourNameDto(whatIsYourNameDto: WhatIsYourNameDto): PreUserInformation =
    PreUserInformation(
      nameForm = WhatIsYourNameForm(
        whatIsYourNameDto.firstName,
        whatIsYourNameDto.lastName
      )
    )

  def getBasicUserInfo(maybePreUserInfo: Option[PreUserInformation]): UserInformation =
    UserInformation(
      firstName = maybePreUserInfo.map(_.nameForm.firstName).getOrElse(""),
      lastName = maybePreUserInfo.map(_.nameForm.lastName).getOrElse(""),
      identificationType = maybePreUserInfo.flatMap(_.identification.map(_.identificationType)).getOrElse(""),
      identificationNumber = maybePreUserInfo.flatMap(_.identification.flatMap(_.identificationNumber)).getOrElse(""),
      emailAddress = maybePreUserInfo.flatMap(_.emailAddress).getOrElse(""),
      selectPlaceOfArrival = maybePreUserInfo.flatMap(_.arrivalForm.map(_.selectPlaceOfArrival)).getOrElse(""),
      enterPlaceOfArrival = maybePreUserInfo.flatMap(_.arrivalForm.map(_.enterPlaceOfArrival)).getOrElse(""),
      dateOfArrival = maybePreUserInfo.flatMap(_.arrivalForm.map(_.dateOfArrival)).getOrElse(LocalDate.now()),
      timeOfArrival = maybePreUserInfo.flatMap(_.arrivalForm.map(_.timeOfArrival)).getOrElse(LocalTime.now())
    )
}

case class WhatIsYourNameForm(firstName: String, lastName: String)

object WhatIsYourNameForm {
  implicit val formats: OFormat[WhatIsYourNameForm] = Json.format[WhatIsYourNameForm]
}

case class IdentificationForm(identificationType: String, identificationNumber: Option[String] = None)

object IdentificationForm {
  implicit val formats: OFormat[IdentificationForm] = Json.format[IdentificationForm]
}

object PreviousDeclarationRequest {
  given formats: OFormat[PreviousDeclarationRequest] = Json.format[PreviousDeclarationRequest]

  def build(dto: DeclarationRetrievalDto): PreviousDeclarationRequest =
    PreviousDeclarationRequest(dto.lastName, dto.referenceNumber.toUpperCase.filterNot(_.isWhitespace))
}

case class PreviousDeclarationRequest(
  lastName: String,
  referenceNumber: String
)

object ProductAlias {
  given formats: OFormat[ProductAlias] = Json.format[ProductAlias]
}

case class ProductAlias(term: String, productPath: ProductPath)

object PaymentNotification {
  given formats: OFormat[PaymentNotification] = Json.format[PaymentNotification]
}

case class PaymentNotification(status: String, reference: String)

object JourneyData {
  given format: OFormat[JourneyData] = Json.format[JourneyData]
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
  preUserInformation: Option[PreUserInformation] = None,
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

  def buildUserInformation: Option[UserInformation] = preUserInformation.flatMap(_.buildUserInfo)
}

object JourneyDataDownstream {
  given formats: OFormat[JourneyDataDownstream] = Json.format[JourneyDataDownstream]

  def apply(journeyData: JourneyData): JourneyDataDownstream =
    JourneyDataDownstream(
      prevDeclaration = journeyData.prevDeclaration,
      euCountryCheck = journeyData.euCountryCheck,
      arrivingNICheck = journeyData.arrivingNICheck,
      isUKVatPaid = journeyData.isUKVatPaid,
      isUKVatExcisePaid = journeyData.isUKVatExcisePaid,
      isUKResident = journeyData.isUKResident,
      isUccRelief = journeyData.isUccRelief,
      isVatResClaimed = journeyData.isVatResClaimed,
      isBringingDutyFree = journeyData.isBringingDutyFree,
      bringingOverAllowance = journeyData.bringingOverAllowance,
      privateCraft = journeyData.privateCraft,
      ageOver17 = journeyData.ageOver17,
      irishBorder = journeyData.irishBorder,
      selectedAliases = journeyData.selectedAliases,
      purchasedProductInstances = journeyData.purchasedProductInstances,
      workingInstance = journeyData.workingInstance,
      userInformation = journeyData.buildUserInformation,
      calculatorResponse = journeyData.calculatorResponse,
      chargeReference = journeyData.chargeReference,
      defaultCountry = journeyData.defaultCountry,
      defaultOriginCountry = journeyData.defaultOriginCountry,
      defaultCurrency = journeyData.defaultCurrency,
      previousDeclarationRequest = journeyData.previousDeclarationRequest,
      declarationResponse = journeyData.declarationResponse,
      deltaCalculation = journeyData.deltaCalculation,
      amendmentCount = journeyData.amendmentCount,
      pendingPayment = journeyData.pendingPayment,
      amendState = journeyData.amendState
    )

}

case class JourneyDataDownstream(
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
)
