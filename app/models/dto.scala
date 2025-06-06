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

import play.api.data.Forms.*
import play.api.data.validation.*
import play.api.data.{Form, Mapping}
import util.{parseLocalDate, parseLocalTime}

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
    )(EuCountryCheckDto.apply)(o => Some(o.euCountryCheck))
  )
}

case class EuCountryCheckDto(euCountryCheck: String)

object BringingOverAllowanceDto {
  val form: Form[BringingOverAllowanceDto] = Form(
    mapping(
      "bringingOverAllowance" -> optional(boolean)
        .verifying("error.bringing_over_allowance", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(BringingOverAllowanceDto.apply)(o => Some(o.bringingOverAllowance))
  )
}
case class BringingOverAllowanceDto(bringingOverAllowance: Boolean)

object ClaimedVatResDto {
  val form: Form[ClaimedVatResDto] = Form(
    mapping(
      "claimedVatRes" -> optional(boolean)
        .verifying("error.claimed_vat_res", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(ClaimedVatResDto.apply)(o => Some(o.claimedVatRes))
  )
}
case class ClaimedVatResDto(claimedVatRes: Boolean)

object BringingDutyFreeDto {
  val form: Form[BringingDutyFreeDto] = Form(
    mapping(
      "isBringingDutyFree" -> optional(boolean)
        .verifying("error.bringing_duty_free", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(BringingDutyFreeDto.apply)(o => Some(o.isBringingDutyFree))
  )
}
case class BringingDutyFreeDto(isBringingDutyFree: Boolean)

object AgeOver17Dto {
  val form: Form[AgeOver17Dto] = Form(
    mapping(
      "ageOver17" -> optional(boolean).verifying("error.over_17", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(AgeOver17Dto.apply)(o => Some(o.ageOver17))
  )
}
case class AgeOver17Dto(ageOver17: Boolean)

object IrishBorderDto {
  val form: Form[IrishBorderDto] = Form(
    mapping(
      "irishBorder" -> optional(boolean)
        .verifying("error.irish_border", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(IrishBorderDto.apply)(o => Some(o.irishBorder))
  )
}
case class IrishBorderDto(irishBorder: Boolean)

object PrivateCraftDto {

  val form: Form[PrivateCraftDto] = Form(
    mapping(
      "privateCraft" -> optional(boolean)
        .verifying("error.private_craft", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(PrivateCraftDto.apply)(o => Some(o.privateCraft))
  )
}
case class PrivateCraftDto(privateCraft: Boolean)

object ConfirmRemoveDto {
  val form: Form[ConfirmRemoveDto] = Form(
    mapping(
      "confirmRemove" -> optional(boolean)
        .verifying("error.remove_product", _.isDefined)
        .transform[Boolean](_.get, b => Option(b))
    )(ConfirmRemoveDto.apply)(o => Some(o.confirmRemove))
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
    )(SelectProductsDto.apply)(o => Some(o.tokens))
  )
}
case class SelectProductsDto(tokens: List[String])

case class CalculatorResponseDto(items: List[Item], calculation: Calculation, allItemsUseGBP: Boolean)

trait Validators {
  val validInputText         = "^[a-zA-Z- ']+$"
  val identificationPattern  = "^[a-zA-Z0-9- '+]+$"
  val telephoneNumberPattern = """^\+?(?:\s*\d){10,13}$"""
  val emailAddressPattern    = """^(?i)[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,4})$"""

  def nonEmptyMaxLength(maxLength: Int, fieldName: String): Constraint[String] = Constraint("constraint.required") {
    text =>
      if (text.isEmpty) { Invalid(ValidationError(s"error.required.$fieldName")) }
      else if (text.length > maxLength) { Invalid(ValidationError(s"error.max-length.$fieldName")) }
      else { Valid }
  }

  def nonEmptyMaxLengthEmailFormat(maxLength: Int, fieldName: String): Constraint[String] =
    Constraint("constraint.required") { text =>
      if (text.isEmpty) { Invalid(ValidationError(s"error.required.$fieldName")) }
      else if (text.length > maxLength) { Invalid(ValidationError(s"error.max-length.$fieldName")) }
      else if (!text.matches(emailAddressPattern)) { Invalid(ValidationError(s"error.format.$fieldName")) }
      else { Valid }
    }

  def nonEmpty(fieldName: String): Constraint[String] = Constraint("constraint.required") { text =>
    if (text.isEmpty) { Invalid(ValidationError(s"error.required.$fieldName")) }
    else { Valid }
  }

  def validateFieldsRegex(fieldName: String, pattern: String): Constraint[String] =
    Constraint { text =>
      if (text.isEmpty || text.matches(pattern)) Valid else Invalid(ValidationError(s"error.$fieldName"))
    }

  def maxLengthContraint(maxLength: Int, fieldName: String): Constraint[String] =
    Constraint("constraint.maxLength", maxLength) { text =>
      if (text.length <= maxLength) Valid else Invalid(ValidationError(s"error.max-length.$fieldName", maxLength))
    }

  def verifyIdentificationNumberConstraint(
    maxLength: Int,
    idPattern: String,
    telephonePattern: String,
    typeOfId: String
  ): Constraint[String] =
    Constraint { number =>
      (typeOfId, number) match {
        case (x, y) if x.contains("driving") && y.isEmpty                      =>
          nonEmpty("driving_licence").apply(y)
        case (x, y) if x.contains("driving") && (y.length > maxLength)         =>
          maxLengthContraint(maxLength, "driving_licence").apply(y)
        case (x, y) if x.contains("passport") && y.isEmpty                     =>
          nonEmpty("passport_number").apply(y)
        case (x, y) if x.contains("passport") && (y.length > maxLength)        =>
          maxLengthContraint(maxLength, "passport_number").apply(y)
        case (x, y) if x.contains("euId") && y.isEmpty                         =>
          nonEmpty("euId").apply(y)
        case (x, y) if x.contains("euId") && (y.length > maxLength)            =>
          maxLengthContraint(maxLength, "euId").apply(y)
        case (x, y) if x.contains("telephone") && y.isEmpty                    =>
          nonEmpty("telephone").apply(y)
        case (x, y) if !x.contains("telephone") && !y.matches(idPattern)       =>
          Invalid(ValidationError(s"error.identification_number.format"))
        case (x, y) if x.contains("telephone") && !y.matches(telephonePattern) =>
          Invalid(ValidationError(s"error.telephone_number.format"))
        case (_, _)                                                            => Valid
      }
    }
}

object WhatIsYourNameDto extends Validators {

  def form: Form[WhatIsYourNameDto] = Form(
    mapping(
      "firstName" -> text
        .verifying(nonEmptyMaxLength(35, "first_name"))
        .verifying(validateFieldsRegex("first_name.valid", validInputText)),
      "lastName"  -> text
        .verifying(nonEmptyMaxLength(35, "last_name"))
        .verifying(validateFieldsRegex("last_name.valid", validInputText))
    )(WhatIsYourNameDto.apply)(o => Some(Tuple.fromProductTyped(o)))
  )
}

object IdentificationTypeDto extends Validators {

  def form: Form[IdentificationTypeDto] = Form(
    mapping(
      "identificationType" -> optional(text)
        .verifying("error.identification_type", y => y.isDefined)
        .transform(o => o.getOrElse(""), o => Some(o))
    )(IdentificationTypeDto.apply)(o => Some(o.identificationType))
  )
}

object IdentificationNumberDto extends Validators {

  def form(idType: String): Form[IdentificationNumberDto] = Form(
    mapping(
      "identificationNumber" -> text
        .verifying(verifyIdentificationNumberConstraint(40, identificationPattern, telephoneNumberPattern, idType))
    )(IdentificationNumberDto.apply)(o => Some(o.number))
  )
}

object EmailAddressDto extends Validators {

  private def emailAddressMatchingConstraint: Constraint[EmailAddressDto] =
    Constraint { model =>
      (model.email, model.confirmEmail) match {
        case (x, y) if x != y => Invalid("error.required.emailAddress.no_match")
        case _                => Valid
      }
    }

  def form: Form[EmailAddressDto] = Form(
    mapping(
      "email"        -> text.verifying(nonEmptyMaxLengthEmailFormat(132, "email")),
      "confirmEmail" -> text.verifying(nonEmptyMaxLengthEmailFormat(132, "confirm_email"))
    )(EmailAddressDto.apply)(o => Some(Tuple.fromProductTyped(o))).verifying(emailAddressMatchingConstraint)
  )
}

object YourJourneyDetailsDto extends Validators {

  private val mandatoryDate: Mapping[DateOfArrival] =
    tuple("day" -> optional(text).verifying("error.date.day_blank", _.isDefined),
      "month" -> optional(text).verifying("error.date.month_blank", _.isDefined),
      "year" -> optional(text).verifying("error.date.year_blank", _.isDefined)
    )
      .verifying(
        "error.enter_a_date",
        dateParts => {
          val definedParts: Int = dateParts.productIterator.collect { case o @ Some(_) => o }.size
          definedParts > 0
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
      .verifying("error.year_length", dateString => dateString._3.length == 4)
      .verifying(
        "error.enter_a_real_date",
        dateString =>
          dateString._2.nonEmpty && dateString._2.length <= 2 && dateString._1.nonEmpty && dateString._1.length <= 2
      )
      .verifying(
        "error.enter_a_real_date",
        dateInt => Try(LocalDate.of(dateInt._3.toInt, dateInt._2.toInt, dateInt._1.toInt)).isSuccess
      )
       .transform[DateOfArrival](
          dateString => DateOfArrival(dateString._1, dateString._2, dateString._3),
          dateInt => (dateInt._1, dateInt._2, dateInt._3)
        )

  private val mandatoryTime: Mapping[TimeOfArrival] =
    tuple("hour" -> optional(text).verifying("error.time.hour_blank", _.isDefined),
      "minute" -> optional(text).verifying("error.time.minute_blank", _.isDefined)
    )
      .verifying(
        "error.enter_a_time",
         maybeTimeString => maybeTimeString._1.nonEmpty && maybeTimeString._2.nonEmpty
      )
      .transform[(String, String)](
        maybeTimeString => (maybeTimeString._1.get, maybeTimeString._2.get),
        timeString => (Some(timeString._1), Some(timeString._2))
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
      .verifying("error.enter_a_real_time", time => time._1.toInt >= 0 && time._1.toInt <= 23 && time._2.toInt >= 0 && time._2.toInt <= 59)
      .transform[TimeOfArrival](
        timeString => TimeOfArrival(timeString._1, timeString._2),
        time => (time._1.toString, time._2.toString)
      )
      

  private def placeOfArrivalConstraint: Constraint[PlaceOfArrival] = Constraint { model =>
    (model.selectPlaceOfArrival, model.enterPlaceOfArrival) match {
      case (x, y) if x.isEmpty && y.isEmpty => Invalid("error.required.place_of_arrival", "selectPlaceOfArrival")
      case _                                => Valid
    }
  }

  def form(declarationTime: LocalDateTime): Form[YourJourneyDetailsDto] = Form(
    mapping(
      "placeOfArrival"    -> mapping(
        "selectPlaceOfArrival" -> optional(text.verifying(maxLengthContraint(40, "place_of_arrival"))),
        "enterPlaceOfArrival"  -> optional(
          text
            .verifying(maxLengthContraint(40, "place_of_arrival"))
            .verifying(validateFieldsRegex("place_of_arrival.valid", validInputText))
        )
      )(PlaceOfArrival.apply)(o => Some(Tuple.fromProductTyped(o)))
        .verifying()
        .verifying(placeOfArrivalConstraint),
      "dateOfArrival" -> mandatoryDate,
      "timeOfArrival" -> mandatoryTime,
        "dateTimeOfArrival" -> mapping(
          "dateOfArrival" -> mandatoryDate,
          "timeOfArrival" -> mandatoryTime
        )(DateTimeOfArrival.apply)(o => Some(Tuple.fromProductTyped(o)))
        .verifying(
          "error.5_days",
          dto => {
            val dateTime = LocalDateTime
              .of(
                parseLocalDate(s"${dto.dateOfArrival}"),
                parseLocalTime(s"${dto.timeOfArrival}")
              )
              .atZone(ZoneOffset.UTC)
            
            dateTime.isAfter(declarationTime.atZone(ZoneOffset.UTC).minusHours(3L)) && dateTime.isBefore(declarationTime.atZone(ZoneOffset.UTC).plusDays(5L))
          }
    )
    (YourJourneyDetailsDto.apply)(o => Some(Tuple.fromProductTyped(o)))
   )
  )

  def toArrivalForm(dto: YourJourneyDetailsDto): ArrivalForm =
    ArrivalForm(
      selectPlaceOfArrival = dto.placeOfArrival.selectPlaceOfArrival.getOrElse(""),
      enterPlaceOfArrival = dto.placeOfArrival.enterPlaceOfArrival.getOrElse(""),
      dateOfArrival = parseLocalDate(s"${dto.dateOfArrival}"),
      timeOfArrival = parseLocalTime(s"${dto.timeOfArrival}")
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
        .verifying(validateFieldsRegex("last_name.valid", validInputText)),
      "referenceNumber" -> text
        .verifying(validateFieldsRegex("referenceNumber.invalid", chargeReferencePattern))
        .verifying(nonEmpty("referenceNumber"))
    )(DeclarationRetrievalDto.apply)(o => Some(Tuple.fromProductTyped(o)))
  )
}

case class DateTimeOfArrival(dateOfArrival: DateOfArrival, timeOfArrival: TimeOfArrival)
case class DateOfArrival(day: String, month: String, year: String)
case class TimeOfArrival(hour: String, minute: String)
case class PlaceOfArrival(selectPlaceOfArrival: Option[String], enterPlaceOfArrival: Option[String])
case class EmailAddressDto(email: String, confirmEmail: String)
case class WhatIsYourNameDto(firstName: String, lastName: String)
case class IdentificationTypeDto(identificationType: String)
case class IdentificationNumberDto(number: String)
case class YourJourneyDetailsDto(
  placeOfArrival: PlaceOfArrival,
  dateOfArrival: DateOfArrival,
  timeOfArrival: TimeOfArrival,
  dateTimeOfArrival: DateTimeOfArrival
)
case class DeclarationRetrievalDto(lastName: String, referenceNumber: String)
