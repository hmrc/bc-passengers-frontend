/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package models

import org.joda.time._
import org.joda.time.format.DateTimeFormat
import play.api.data.Forms.{optional, _}
import play.api.data.validation._
import play.api.data.{Form, Mapping}
import services.CountriesService

import scala.util.Try

case class OtherGoodsSearchDto(
  searchTerm: Option[OtherGoodsSearchItem],
  remove: Option[Int],
  action: Option[String]
)

object OtherGoodsDto {

  def fromPurchasedProductInstance(purchasedProductInstance: PurchasedProductInstance): Option[OtherGoodsDto] = for {
    country <- purchasedProductInstance.country
    currency <- purchasedProductInstance.currency
  } yield OtherGoodsDto("", country.code, currency, purchasedProductInstance.cost.toList)
}

case class OtherGoodsDto(
  action: String,
  country: String,
  currency: String,
  costs: List[BigDecimal] = List()
)

object AlcoholDto {
  def fromPurchasedProductInstance(purchasedProductInstance: PurchasedProductInstance): Option[AlcoholDto] = for {
    country <- purchasedProductInstance.country
    currency <- purchasedProductInstance.currency
    weightOrVolume <- purchasedProductInstance.weightOrVolume
    cost <- purchasedProductInstance.cost
  } yield AlcoholDto(weightOrVolume, country.code, currency, cost)
}

case class AlcoholDto(
  weightOrVolume: BigDecimal,
  country: String,
  currency: String,
  cost: BigDecimal
)

object TobaccoDto {
  def fromPurchasedProductInstance(purchasedProductInstance: PurchasedProductInstance): Option[TobaccoDto] = for {
    country <- purchasedProductInstance.country
    currency <- purchasedProductInstance.currency
    cost <- purchasedProductInstance.cost
    noOfSticks = purchasedProductInstance.noOfSticks
    weightOrVolume = purchasedProductInstance.weightOrVolume
  } yield TobaccoDto(noOfSticks, weightOrVolume, country.code, currency, cost)
}

case class TobaccoDto(
  noOfSticks: Option[Int],
  weightOrVolume: Option[BigDecimal],
  country: String,
  currency: String,
  cost: BigDecimal
)

object EuCountryCheckDto {
  val form: Form[EuCountryCheckDto] = Form(
    mapping(
     "euCountryCheck" -> optional(text).verifying("error.eu_check", x => x.fold(false)(_.nonEmpty)).transform[String](_.get, s => Some(s))
    )(EuCountryCheckDto.apply)(EuCountryCheckDto.unapply)
  )
}

case class EuCountryCheckDto(euCountryCheck: String)

object BringingOverAllowanceDto {
  val form: Form[BringingOverAllowanceDto] = Form(
    mapping(
      "bringingOverAllowance" -> optional(boolean).verifying("error.bringing_over_allowance", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(BringingOverAllowanceDto.apply)(BringingOverAllowanceDto.unapply)
  )
}
case class BringingOverAllowanceDto(bringingOverAllowance: Boolean)

object ClaimedVatResDto {
  val form: Form[ClaimedVatResDto] = Form(
    mapping(
      "claimedVatRes" -> optional(boolean).verifying("error.claimed_vat_res", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(ClaimedVatResDto.apply)(ClaimedVatResDto.unapply)
  )
}
case class ClaimedVatResDto(claimedVatRes: Boolean)

object BringingDutyFreeDto {
  val form: Form[BringingDutyFreeDto] = Form(
    mapping(
      "isBringingDutyFree" -> optional(boolean).verifying("error.bringing_duty_free", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(BringingDutyFreeDto.apply)(BringingDutyFreeDto.unapply)
  )
}
case class BringingDutyFreeDto(isBringingDutyFree: Boolean)

object SelectedCountryDto {
  def form(countryService: CountriesService, optionalItemsRemaining: Boolean = true): Form[SelectedCountryDto] = Form(
    mapping(
      "country" -> text.verifying("error.country.invalid", code => countryService.isValidCountryCode(code)),
      "itemsRemaining" -> optional(number).verifying("error.required", i => optionalItemsRemaining || i.isDefined).transform[Int](_.getOrElse(0), i => Some(i))
    )(SelectedCountryDto.apply)(SelectedCountryDto.unapply)
  )
}
case class SelectedCountryDto(country: String, itemsRemaining: Int)

object AgeOver17Dto {
  val form: Form[AgeOver17Dto] = Form(
    mapping(
      "ageOver17" -> optional(boolean).verifying("error.over_17", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(AgeOver17Dto.apply)(AgeOver17Dto.unapply)
  )
}
case class AgeOver17Dto(ageOver17: Boolean)

object IrishBorderDto {
  val form: Form[IrishBorderDto] = Form(
    mapping(
      "irishBorder" -> optional(boolean).verifying("error.irish_border", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(IrishBorderDto.apply)(IrishBorderDto.unapply)
  )
}
case class IrishBorderDto(irishBorder: Boolean)


object PrivateCraftDto {

  val form: Form[PrivateCraftDto] = Form(
    mapping(
      "privateCraft" -> optional(boolean).verifying("error.private_craft", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(PrivateCraftDto.apply)(PrivateCraftDto.unapply)
  )
}
case class PrivateCraftDto(privateCraft: Boolean)

object ConfirmRemoveDto {
  val form: Form[ConfirmRemoveDto] = Form(
    mapping(
      "confirmRemove" -> optional(boolean).verifying("error.remove_product", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(ConfirmRemoveDto.apply)(ConfirmRemoveDto.unapply)
  )
}
case class ConfirmRemoveDto(confirmRemove: Boolean)

object SelectProductsDto {

  def nonEmptyList[T]: Constraint[List[T]] = Constraint[List[T]]("constraint.required") { list =>
    if (list.nonEmpty) Valid else Invalid(ValidationError("error.required"))
  }

  val form: Form[SelectProductsDto] = Form(
    mapping(
      "tokens" -> list(nonEmptyText).verifying(nonEmptyList)
    )(SelectProductsDto.apply)(SelectProductsDto.unapply)
  )
}
case class SelectProductsDto(tokens: List[String])

case class CalculatorResponseDto(items: List[Item], calculation: Calculation, allItemsUseGBP: Boolean)

object EnterYourDetailsDto {
  val telephoneNumberPattern = """^\+?(?:\s*\d){10,13}$"""
  val emailAddressPattern = """^(?i)[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,4})$"""

  private def nonEmptyMaxLength(maxLength: Int, fieldName: String): Constraint[String] = Constraint("constraint.required") {
    text =>
      if (text.isEmpty) Invalid(ValidationError(s"error.required.$fieldName"))
      else if (text.length > maxLength) Invalid(ValidationError(s"error.max-length.$fieldName"))
      else Valid
  }

  def maxLength(length: Int , fieldName: String): Constraint[String] = Constraint("constraint.maxLength", length) {
    text =>
      if (text.size <= length) Valid else Invalid(ValidationError(s"error.max-length.$fieldName", length))
  }

  private def isEmailAddress(pattern: String, fieldName: String): Constraint[String] = {
    Constraint {
      case str if str.matches(pattern) =>
        Valid
      case str if str.isEmpty =>
        Invalid(ValidationError(s"error.required.$fieldName"))
      case _ =>
        Invalid(s"error.format.$fieldName", pattern)
    }
  }

  private def mandatoryDate(error: String) = tuple("day" -> optional(text), "month" -> optional(text), "year" -> optional(text))
    .verifying("error.enter_a_date", dateParts => {
      val definedParts: Int = dateParts.productIterator.collect { case o@Some(_) => o }.size
      definedParts > 0
    })
    .verifying("error.include_day_month_and_year", dateParts => {
      val definedParts: Int = dateParts.productIterator.collect { case o@Some(_) => o }.size
      definedParts < 1 || definedParts > 2
    })
    .transform[(String, String, String)](
      maybeDateString => (maybeDateString._1.get, maybeDateString._2.get, maybeDateString._3.get),
      dateString => (Some(dateString._1), Some(dateString._2), Some(dateString._3))
    )
    .verifying("error.only_whole_numbers", dateString => dateString._1.forall(_.isDigit) && dateString._2.forall(_.isDigit) && dateString._3.forall(_.isDigit))
    .transform[(String, String, String)](identity, identity)
    .verifying("error.year_length", dateString => dateString._3.size == 4)
    .transform[(Int, Int, Int)](dateString => (dateString._1.toInt, dateString._2.toInt, dateString._3.toInt), dateInt => (dateInt._1.toString, dateInt._2.toString, dateInt._3.toString))
    .verifying("error.enter_a_real_date", dateInt => Try(new LocalDate(dateInt._3.toInt, dateInt._2.toInt, dateInt._1.toInt)).isSuccess)
    .transform[LocalDate](
      dateInt => LocalDate.parse(s"${dateInt._3.toString}-${dateInt._2.toString}-${dateInt._1.toString}"),
      localDate => (localDate.getDayOfMonth, localDate.getMonthOfYear, localDate.getDayOfYear)
    )


  private def mandatoryTime(error: String): Mapping[LocalTime] = tuple("hour" -> optional(text), "minute" -> optional(text), "halfday" -> optional(text))
    .verifying(error, maybeTimeString => maybeTimeString._1.nonEmpty && maybeTimeString._2.nonEmpty && maybeTimeString._3.nonEmpty)
    .transform[(String, String, String)](
      maybeTimeString => (maybeTimeString._1.get, maybeTimeString._2.get, maybeTimeString._3.get),
      timeString => (Some(timeString._1), Some(timeString._2), Some(timeString._3))
    )
    .verifying("error.enter_a_real_time", timeString => timeString._1.forall(_.isDigit) && timeString._2.forall(_.isDigit))
    .transform[(Int, Int, String)](
      timeString => (timeString._1.toInt, timeString._2.toInt, timeString._3),
      time => (time._1.toString, time._2.toString, time._3)
    )
    .verifying("error.enter_a_real_time", time => time._1>= 1 && time._1 <= 12 && time._2 >= 0 && time._2<= 59)
    .verifying("error.enter_a_real_time", time => time._3.toLowerCase == "am" || time._3.toLowerCase == "pm")
    .transform[LocalTime](
      time => LocalTime.parse(s"${time._1}:${time._2} ${time._3}", DateTimeFormat.forPattern("hh:mm aa")),
      localTime => (localTime.getHourOfDay, localTime.getMinuteOfHour, if (localTime.getHourOfDay > 12) "pm" else "am")
    )

  def placeOfArrivalConstraint(message: String): Constraint[PlaceOfArrival] = Constraint {
    model => (model.selectPlaceOfArrival, model.enterPlaceOfArrival) match {
      case (x, y) if(x.isEmpty && y.isEmpty) => Invalid(message, "selectPlaceOfArrival")
      case _ => Valid
    }
  }

  private def verifyIdentificationNumberConstraint(pattern: String): Constraint[Identification] = {
    Constraint {
      model =>
        (model.identificationType, model.identificationNumber) match {
          case (x, y) if (!x.contains("telephone") && !y.isEmpty) => Valid
          case (x, y) if (x.contains("telephone") && y.matches(pattern)  ) => Valid
          case _ => Invalid(ValidationError(s"error.telephone_number.format"))
        }
    }
  }


  def form(declarationTime: DateTime): Form[EnterYourDetailsDto] = Form(
    mapping(
      "firstName" -> text.verifying(nonEmptyMaxLength(35, "first_name")),
      "lastName" -> text.verifying(nonEmptyMaxLength(35, "last_name")),
      "identification" -> mapping(
        "identificationType" -> optional(text).verifying("error.identification_type", y => y.nonEmpty && Try(y).toOption.isDefined),
        "identificationNumber" -> text.verifying(nonEmptyMaxLength(40, "identification_number"))
      )(Identification.apply)(Identification.unapply)
        .verifying(verifyIdentificationNumberConstraint(telephoneNumberPattern)),
      "emailAddress" -> text.verifying(isEmailAddress(emailAddressPattern, "emailAddress")),
      "placeOfArrival" -> mapping(
        "selectPlaceOfArrival" -> optional(text.verifying(maxLength(40, "place_of_arrival"))),
        "enterPlaceOfArrival" -> optional(text.verifying(maxLength(40, "place_of_arrival")))
      )(PlaceOfArrival.apply)(PlaceOfArrival.unapply).verifying()
        .verifying(placeOfArrivalConstraint("error.required.place_of_arrival")),
      "dateTimeOfArrival" -> mapping(
        "dateOfArrival" -> mandatoryDate("error.enter_a_date"),
        "timeOfArrival" -> mandatoryTime("error.enter_a_time")
      )(DateTimeOfArrival.apply)(DateTimeOfArrival.unapply)
        .verifying("error.72_hours", dto => dto.dateOfArrival.toDateTime(dto.timeOfArrival).isBefore(declarationTime.plus(Days.THREE)))
    )(EnterYourDetailsDto.apply)(EnterYourDetailsDto.unapply)
  )
}

case class DateTimeOfArrival(dateOfArrival: LocalDate, timeOfArrival: LocalTime)
case class PlaceOfArrival(selectPlaceOfArrival: Option[String], enterPlaceOfArrival: Option[String])
case class Identification(identificationType: Option[String], identificationNumber: String)
case class EnterYourDetailsDto(firstName: String, lastName: String, identification: Identification , emailAddress: String, placeOfArrival: PlaceOfArrival,  dateTimeOfArrival: DateTimeOfArrival)
