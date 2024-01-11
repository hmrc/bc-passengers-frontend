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

import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.{Form, Mapping}
import util.{formatLocalDate, formatLocalTime, parseLocalDate, parseLocalTime}

import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import scala.util.Try

object OtherGoodsDto {

  def fromPurchasedProductInstance(purchasedProductInstance: PurchasedProductInstance): Option[OtherGoodsDto] = for {
    country  <- purchasedProductInstance.country
    currency <- purchasedProductInstance.currency
    cost     <- purchasedProductInstance.cost
  } yield OtherGoodsDto(
    purchasedProductInstance.searchTerm,
    country.code,
    purchasedProductInstance.originCountry.map(_.code),
    currency,
    cost,
    purchasedProductInstance.isVatPaid,
    purchasedProductInstance.isUccRelief,
    purchasedProductInstance.isCustomPaid,
    purchasedProductInstance.hasEvidence
  )

}

case class OtherGoodsDto(
  searchTerm: Option[OtherGoodsSearchItem],
  country: String,
  originCountry: Option[String],
  currency: String,
  cost: BigDecimal,
  isVatPaid: Option[Boolean],
  isUccRelief: Option[Boolean],
  isCustomPaid: Option[Boolean],
  hasEvidence: Option[Boolean]
)

object AlcoholDto {
  def fromPurchasedProductInstance(purchasedProductInstance: PurchasedProductInstance): Option[AlcoholDto] = for {
    country        <- purchasedProductInstance.country
    currency       <- purchasedProductInstance.currency
    weightOrVolume <- purchasedProductInstance.weightOrVolume
    cost           <- purchasedProductInstance.cost
  } yield AlcoholDto(
    weightOrVolume,
    country.code,
    purchasedProductInstance.originCountry.map(_.code),
    currency,
    cost,
    purchasedProductInstance.isVatPaid,
    purchasedProductInstance.isExcisePaid,
    purchasedProductInstance.isCustomPaid,
    purchasedProductInstance.hasEvidence
  )
}

case class AlcoholDto(
  weightOrVolume: BigDecimal,
  country: String,
  originCountry: Option[String],
  currency: String,
  cost: BigDecimal,
  isVatPaid: Option[Boolean],
  isExcisePaid: Option[Boolean],
  isCustomPaid: Option[Boolean],
  hasEvidence: Option[Boolean]
)

object TobaccoDto {
  def fromPurchasedProductInstance(purchasedProductInstance: PurchasedProductInstance): Option[TobaccoDto] = for {
    country       <- purchasedProductInstance.country
    currency      <- purchasedProductInstance.currency
    cost          <- purchasedProductInstance.cost
    noOfSticks     = purchasedProductInstance.noOfSticks
    weightOrVolume = purchasedProductInstance.weightOrVolume
  } yield TobaccoDto(
    noOfSticks,
    weightOrVolume,
    country.code,
    purchasedProductInstance.originCountry.map(_.code),
    currency,
    cost,
    purchasedProductInstance.isVatPaid,
    purchasedProductInstance.isExcisePaid,
    purchasedProductInstance.isCustomPaid,
    purchasedProductInstance.hasEvidence
  )
}

case class TobaccoDto(
  noOfSticks: Option[Int],
  weightOrVolume: Option[BigDecimal],
  country: String,
  originCountry: Option[String],
  currency: String,
  cost: BigDecimal,
  isVatPaid: Option[Boolean],
  isExcisePaid: Option[Boolean],
  isCustomPaid: Option[Boolean],
  hasEvidence: Option[Boolean]
)

object EuCountryCheckDto {
  val form: Form[EuCountryCheckDto] = Form(
    mapping(
      "euCountryCheck" -> optional(text)
        .verifying("error.eu_check", x => x.fold(false)(_.nonEmpty))
        .transform[String](_.get, s => Some(s))
    )(EuCountryCheckDto.apply)(EuCountryCheckDto.unapply)
  )
}

case class EuCountryCheckDto(euCountryCheck: String)

object BringingOverAllowanceDto {
  val form: Form[BringingOverAllowanceDto] = Form(
    mapping(
      "bringingOverAllowance" -> optional(boolean)
        .verifying("error.bringing_over_allowance", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(BringingOverAllowanceDto.apply)(BringingOverAllowanceDto.unapply)
  )
}
case class BringingOverAllowanceDto(bringingOverAllowance: Boolean)

object ClaimedVatResDto {
  val form: Form[ClaimedVatResDto] = Form(
    mapping(
      "claimedVatRes" -> optional(boolean)
        .verifying("error.claimed_vat_res", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(ClaimedVatResDto.apply)(ClaimedVatResDto.unapply)
  )
}
case class ClaimedVatResDto(claimedVatRes: Boolean)

object BringingDutyFreeDto {
  val form: Form[BringingDutyFreeDto] = Form(
    mapping(
      "isBringingDutyFree" -> optional(boolean)
        .verifying("error.bringing_duty_free", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(BringingDutyFreeDto.apply)(BringingDutyFreeDto.unapply)
  )
}
case class BringingDutyFreeDto(isBringingDutyFree: Boolean)

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
      "irishBorder" -> optional(boolean)
        .verifying("error.irish_border", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(IrishBorderDto.apply)(IrishBorderDto.unapply)
  )
}
case class IrishBorderDto(irishBorder: Boolean)

object PrivateCraftDto {

  val form: Form[PrivateCraftDto] = Form(
    mapping(
      "privateCraft" -> optional(boolean)
        .verifying("error.private_craft", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(PrivateCraftDto.apply)(PrivateCraftDto.unapply)
  )
}
case class PrivateCraftDto(privateCraft: Boolean)

object ConfirmRemoveDto {
  val form: Form[ConfirmRemoveDto] = Form(
    mapping(
      "confirmRemove" -> optional(boolean)
        .verifying("error.remove_product", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(ConfirmRemoveDto.apply)(ConfirmRemoveDto.unapply)
  )
}
case class ConfirmRemoveDto(confirmRemove: Boolean)

object SelectProductsDto {

  private val getError: String = "error.required"

  val form: Form[SelectProductsDto] = Form(
    mapping(
      "tokens" -> text
        .verifying(getError, _.nonEmpty)
        .transform[List[String]](item => List(item), _.head)
    )(SelectProductsDto.apply)(SelectProductsDto.unapply)
  )
}
case class SelectProductsDto(tokens: List[String])

case class CalculatorResponseDto(items: List[Item], calculation: Calculation, allItemsUseGBP: Boolean)

trait Validators {
  val validInputText                                                           = "^[a-zA-Z- ']+$"
  def nonEmptyMaxLength(maxLength: Int, fieldName: String): Constraint[String] = Constraint("constraint.required") {
    text =>
      if (text.isEmpty) { Invalid(ValidationError(s"error.required.$fieldName")) }
      else if (text.length > maxLength) { Invalid(ValidationError(s"error.max-length.$fieldName")) }
      else { Valid }
  }

  def nonEmpty(fieldName: String): Constraint[String] = Constraint("constraint.required") { text =>
    if (text.isEmpty) { Invalid(ValidationError(s"error.required.$fieldName")) }
    else { Valid }
  }

  def validateFieldsRegex(errorKey: String, pattern: String): Constraint[String] =
    Constraint { text =>
      if (text.isEmpty || text.matches(pattern)) Valid else Invalid(errorKey)
    }
}

object EnterYourDetailsDto extends Validators {
  private val identificationPattern  = "^[a-zA-Z0-9- '+]+$"
  private val telephoneNumberPattern = """^\+?(?:\s*\d){10,13}$"""
  private val emailAddressPattern    = """^(?i)[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,4})$"""

  private def maxLength(length: Int, fieldName: String): Constraint[String] =
    Constraint("constraint.maxLength", length) { text =>
      if (text.length <= length) Valid else Invalid(ValidationError(s"error.max-length.$fieldName", length))
    }

  private val mandatoryDate: Mapping[String] =
    tuple("day" -> optional(text), "month" -> optional(text), "year" -> optional(text))
      .verifying(
        "error.enter_a_date",
        dateParts => {
          val definedParts: Int = dateParts.productIterator.collect { case o @ Some(_) => o }.size
          definedParts > 0
        }
      )
      .verifying(
        "error.include_day_month_and_year",
        dateParts => {
          val definedParts: Int = dateParts.productIterator.collect { case o @ Some(_) => o }.size
          definedParts < 1 || definedParts > 2
        }
      )
      .transform[(String, String, String)](
        maybeDateString => (maybeDateString._1.get, maybeDateString._2.get, maybeDateString._3.get),
        dateString => (Some(dateString._1), Some(dateString._2), Some(dateString._3))
      )
      .verifying(
        "error.enter_a_real_date",
        dateString =>
          dateString._1.forall(_.isDigit) && dateString._2.forall(_.isDigit) && dateString._3.forall(_.isDigit)
      )
      .transform[(String, String, String)](identity, identity)
      .verifying("error.year_length", dateString => dateString._3.length == 4)
      .verifying(
        "error.enter_a_real_date",
        dateString =>
          dateString._2.nonEmpty && dateString._2.length <= 2 && dateString._1.nonEmpty && dateString._1.length <= 2
      )
      .transform[(Int, Int, Int)](
        dateString => (dateString._1.toInt, dateString._2.toInt, dateString._3.toInt),
        dateInt => (dateInt._1.toString, dateInt._2.toString, dateInt._3.toString)
      )
      .verifying(
        "error.enter_a_real_date",
        dateInt => Try(LocalDate.of(dateInt._3, dateInt._2, dateInt._1)).isSuccess
      )
      .transform[String](
        dateInt => s"${dateInt._3.toString}-${dateInt._2.toString}-${dateInt._1.toString}",
        dateString =>
          dateString.split("-") match {
            case Array(dd, mm, yyyy) => (dd.toInt, mm.toInt, yyyy.toInt)
          }
      )

  private val mandatoryTime: Mapping[String] =
    tuple("hour" -> optional(text), "minute" -> optional(text), "halfday" -> optional(text))
      .verifying(
        "error.enter_a_time",
        maybeTimeString => maybeTimeString._1.nonEmpty && maybeTimeString._2.nonEmpty && maybeTimeString._3.nonEmpty
      )
      .transform[(String, String, String)](
        maybeTimeString => (maybeTimeString._1.get, maybeTimeString._2.get, maybeTimeString._3.get),
        timeString => (Some(timeString._1), Some(timeString._2), Some(timeString._3))
      )
      .verifying(
        "error.enter_a_real_time",
        timeString => timeString._1.forall(_.isDigit) && timeString._2.forall(_.isDigit)
      )
      .verifying(
        "error.enter_a_real_time",
        timeString =>
          timeString._1.nonEmpty && timeString._1.length <= 2 && timeString._2.nonEmpty && timeString._2.length <= 2
      )
      .transform[(Int, Int, String)](
        timeString => (timeString._1.toInt, timeString._2.toInt, timeString._3),
        time => (time._1.toString, time._2.toString, time._3)
      )
      .verifying("error.enter_a_real_time", time => time._1 >= 1 && time._1 <= 12 && time._2 >= 0 && time._2 <= 59)
      .verifying("error.enter_a_real_time", time => time._3.toLowerCase == "am" || time._3.toLowerCase == "pm")
      .transform[String](
        time => s"${time._1}:${time._2} ${time._3}",
        localTime =>
          localTime.split(":") match {
            case Array(hours, minutesAndAmPm) =>
              val x = minutesAndAmPm.split(" ") match {
                case Array(minutes, ampm) =>
                  (minutes, ampm)
              }
              (hours.toInt, x._1.toInt, x._2)
          }
      )

  private def placeOfArrivalConstraint: Constraint[PlaceOfArrival] = Constraint { model =>
    (model.selectPlaceOfArrival, model.enterPlaceOfArrival) match {
      case (x, y) if x.isEmpty && y.isEmpty => Invalid("error.required.place_of_arrival", "selectPlaceOfArrival")
      case _                                => Valid
    }
  }

  private def verifyIdentificationNumberConstraint(
    idPattern: String,
    telephonePattern: String
  ): Constraint[Identification] =
    Constraint { model =>
      (model.identificationType, model.identificationNumber) match {
        case (x, y) if !x.contains("telephone") && y.matches(idPattern)        => Valid
        case (x, y) if !x.contains("telephone") && !y.matches(idPattern)       =>
          Invalid(ValidationError(s"error.identification_number.format"))
        case (x, y) if x.contains("telephone") && y.matches(telephonePattern)  => Valid
        case (x, y) if x.contains("telephone") && !y.matches(telephonePattern) =>
          Invalid(ValidationError(s"error.telephone_number.format"))
      }
    }

  private def emailAddressMatchConstraint(): Constraint[EmailAddress] =
    Constraint { model =>
      (model.email, model.confirmEmail) match {
        case (x, y) if x.isEmpty && y.isEmpty                                                 => Valid
        case (x, y) if x.nonEmpty && y.isEmpty                                                => Invalid(ValidationError(s"error.required.emailAddress.confirmEmail"))
        case (x, y) if x.isEmpty && y.nonEmpty                                                => Invalid(ValidationError(s"error.required.emailAddress.email"))
        case (x, y) if (!x.matches(emailAddressPattern)) || (!y.matches(emailAddressPattern)) =>
          Invalid(s"error.format.emailAddress", emailAddressPattern)
        case (x, y) if x != y                                                                 =>
          Invalid(ValidationError(s"error.required.emailAddress.no_match"))
        case _                                                                                => Valid
      }
    }

  def form(declarationTime: LocalDateTime): Form[EnterYourDetailsDto] = Form(
    mapping(
      "firstName"         -> text
        .verifying(nonEmptyMaxLength(35, "first_name"))
        .verifying(validateFieldsRegex("error.first_name.valid", validInputText)),
      "lastName"          -> text
        .verifying(nonEmptyMaxLength(35, "last_name"))
        .verifying(validateFieldsRegex("error.last_name.valid", validInputText)),
      "identification"    -> mapping(
        "identificationType"   -> optional(text)
          .verifying("error.identification_type", y => y.nonEmpty && Try(y).toOption.isDefined),
        "identificationNumber" -> text.verifying(nonEmptyMaxLength(40, "identification_number"))
      )(Identification.apply)(Identification.unapply)
        .verifying(verifyIdentificationNumberConstraint(identificationPattern, telephoneNumberPattern)),
      "emailAddress"      -> mapping(
        "email"        -> text.verifying(maxLength(132, "email")),
        "confirmEmail" -> text.verifying(maxLength(132, "email"))
      )(EmailAddress.apply)(EmailAddress.unapply)
        .verifying(emailAddressMatchConstraint()),
      "placeOfArrival"    -> mapping(
        "selectPlaceOfArrival" -> optional(text.verifying(maxLength(40, "place_of_arrival"))),
        "enterPlaceOfArrival"  -> optional(
          text
            .verifying(maxLength(40, "place_of_arrival"))
            .verifying(validateFieldsRegex("error.place_of_arrival.valid", validInputText))
        )
      )(PlaceOfArrival.apply)(PlaceOfArrival.unapply)
        .verifying()
        .verifying(placeOfArrivalConstraint),
      "dateTimeOfArrival" -> mapping(
        "dateOfArrival" -> mandatoryDate,
        "timeOfArrival" -> mandatoryTime
      )(DateTimeOfArrival.apply)(DateTimeOfArrival.unapply)
        .verifying(
          "error.5_days",
          dto =>
            LocalDateTime
              .of(
                parseLocalDate(dto.dateOfArrival),
                parseLocalTime(dto.timeOfArrival)
              )
              .atZone(ZoneOffset.UTC)
              .isAfter(declarationTime.atZone(ZoneOffset.UTC).minusHours(3L))
        )
        .verifying(
          "error.5_days",
          dto =>
            LocalDateTime
              .of(
                parseLocalDate(dto.dateOfArrival),
                parseLocalTime(dto.timeOfArrival)
              )
              .atZone(ZoneOffset.UTC)
              .isBefore(declarationTime.atZone(ZoneOffset.UTC).plusDays(5L))
        )
    )(EnterYourDetailsDto.apply)(EnterYourDetailsDto.unapply)
  )

  def fromUserInformation(userInformation: UserInformation): EnterYourDetailsDto =
    EnterYourDetailsDto(
      userInformation.firstName,
      userInformation.lastName,
      Identification(Some(userInformation.identificationType), userInformation.identificationNumber),
      EmailAddress(userInformation.emailAddress, userInformation.emailAddress),
      PlaceOfArrival(Some(userInformation.selectPlaceOfArrival), Some(userInformation.enterPlaceOfArrival)),
      DateTimeOfArrival(
        formatLocalDate(userInformation.dateOfArrival, "dd-MM-yyyy"),
        formatLocalTime(userInformation.timeOfArrival).toLowerCase
      )
    )

}

object DeclarationRetrievalDto extends Validators {

  def fromPreviousDeclarationDetails(previousDeclarationDetails: PreviousDeclarationRequest): DeclarationRetrievalDto =
    DeclarationRetrievalDto(previousDeclarationDetails.lastName, previousDeclarationDetails.referenceNumber)
  private val chargeReferencePattern                                                                                  = """^[Xx]([A-Za-z])[Pp][Rr](\d{10})$"""
  def form(): Form[DeclarationRetrievalDto]                                                                           = Form(
    mapping(
      "lastName"        -> text
        .verifying(nonEmptyMaxLength(35, "last_name"))
        .verifying(validateFieldsRegex("error.last_name.valid", validInputText)),
      "referenceNumber" -> text
        .verifying(validateFieldsRegex("error.referenceNumber.invalid", chargeReferencePattern))
        .verifying(nonEmpty("referenceNumber"))
    )(DeclarationRetrievalDto.apply)(DeclarationRetrievalDto.unapply)
  )
}

case class DateTimeOfArrival(dateOfArrival: String, timeOfArrival: String)
case class PlaceOfArrival(selectPlaceOfArrival: Option[String], enterPlaceOfArrival: Option[String])
case class Identification(identificationType: Option[String], identificationNumber: String)
case class EmailAddress(email: String, confirmEmail: String)
case class EnterYourDetailsDto(
  firstName: String,
  lastName: String,
  identification: Identification,
  emailAddress: EmailAddress,
  placeOfArrival: PlaceOfArrival,
  dateTimeOfArrival: DateTimeOfArrival
)
case class DeclarationRetrievalDto(lastName: String, referenceNumber: String)
