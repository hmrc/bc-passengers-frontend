package models

import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.Json


object PurchasedProductInstance {
  implicit val formats = Json.format[PurchasedProductInstance]
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
  implicit val formats = Json.format[UserInformation]

  def build(dto: EnterYourDetailsDto): UserInformation =
    UserInformation(dto.firstName, dto.lastName, dto.passportNumber, dto.placeOfArrival, dto.dateOfArrival)
}
case class UserInformation(
  firstName: String,
  lastName: String,
  passportNumber: String,
  placeOfArrival: String,
  dateOfArrival: LocalDate
)


object JourneyData {
  implicit val formats = Json.format[JourneyData]
}
case class JourneyData(
  euCountryCheck: Option[String] = None,
  privateCraft: Option[Boolean] = None,
  ageOver17: Option[Boolean] = None,
  selectedProducts: List[List[String]] = Nil,
  purchasedProductInstances: List[PurchasedProductInstance] = Nil,
  workingInstance: Option[PurchasedProductInstance] = None,
  userInformation: Option[UserInformation] = None,
  calculatorResponse: Option[CalculatorResponse] = None
) {

  def allCurrencyCodes: Set[String] = (for {
    purchasedProductInstances <- purchasedProductInstances
    currencyCode <- purchasedProductInstances.currency
  } yield currencyCode).toSet

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
    val purchasedProductInstance = block(this.workingInstance.getOrElse(PurchasedProductInstance(path, iid)))
    this.copy(workingInstance = Some(purchasedProductInstance))
  }

  def clearingWorking: JourneyData = this.copy(workingInstance = None)
}
