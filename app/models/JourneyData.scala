/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import org.joda.time.{LocalDate, LocalTime}
import play.api.libs.json.{Json, OFormat}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._


object PurchasedProductInstance {
  implicit val formats: OFormat[PurchasedProductInstance] = Json.format[PurchasedProductInstance]
}
case class PurchasedProductInstance(
  path: ProductPath,
  iid: String,
  weightOrVolume: Option[BigDecimal] = None,
  noOfSticks: Option[Int] = None,
  country: Option[Country] = None,
  currency: Option[String] = None,
  cost: Option[BigDecimal] = None
)

object UserInformation {
  implicit val formats: OFormat[UserInformation] = Json.format[UserInformation]

  def build(dto: EnterYourDetailsDto): UserInformation =
    UserInformation(dto.firstName,
      dto.lastName,
      dto.identification.identificationType.getOrElse(""),
      dto.identification.identificationNumber ,
      dto.emailAddress.email,dto.placeOfArrival.selectPlaceOfArrival.getOrElse(""),
      dto.placeOfArrival.enterPlaceOfArrival.getOrElse(""),
      dto.dateTimeOfArrival.dateOfArrival,
      dto.dateTimeOfArrival.timeOfArrival)
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

object PreviousDeclarationDetails {
  implicit val formats: OFormat[PreviousDeclarationDetails] = Json.format[PreviousDeclarationDetails]

  def build(dto: DeclarationRetrievalDto): PreviousDeclarationDetails =
    PreviousDeclarationDetails(dto.lastName, dto.identificationNumber, dto.referenceNumber)
}
case class PreviousDeclarationDetails(
                            lastName: String,
                            identificationNumber: String,
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
  implicit val formats: OFormat[JourneyData] = Json.format[JourneyData]
}

case class JourneyData(
  prevDeclaration: Option[Boolean] = None,
  euCountryCheck: Option[String] = None,
  arrivingNICheck: Option[Boolean] = None,
  isUKVatPaid: Option[Boolean] = None,
  isUKExcisePaid: Option[Boolean] = None,
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
  defaultCurrency: Option[String] = None,
  previousDeclarationDetails: Option[PreviousDeclarationDetails] = None
) {

  val selectedProducts: List[List[String]] = selectedAliases.map(_.productPath.components)

  def allCurrencyCodes: Set[String] = (for {
    purchasedProductInstances <- purchasedProductInstances
    currencyCode <- purchasedProductInstances.currency
  } yield currencyCode).toSet

  def getPurchasedProductInstance(iid: String): Option[PurchasedProductInstance] =
    purchasedProductInstances.find(_.iid == iid)

  def getOrCreatePurchasedProductInstance(path: ProductPath, iid: String): PurchasedProductInstance
    = purchasedProductInstances.find(_.iid == iid).getOrElse(PurchasedProductInstance(path, iid))

  def updatePurchasedProductInstance(path: ProductPath, iid: String)(block: PurchasedProductInstance => PurchasedProductInstance): JourneyData = {
    val newPdList = block(getOrCreatePurchasedProductInstance(path, iid)) :: purchasedProductInstances.filterNot(_.path == path)
    this.copy(purchasedProductInstances = newPdList)
  }

  def removePurchasedProductInstance(iid: String): JourneyData = {
    this.copy(purchasedProductInstances = purchasedProductInstances.filterNot(_.iid==iid))
  }

  def withUpdatedWorkingInstance(path: ProductPath, iid: String)(block: PurchasedProductInstance => PurchasedProductInstance): JourneyData = {
    val workingInstance = block(this.workingInstance.getOrElse(PurchasedProductInstance(path, iid)))
    this.copy(workingInstance = Some(workingInstance))
  }

  def clearingWorking: JourneyData = this.copy(workingInstance = None)
}
