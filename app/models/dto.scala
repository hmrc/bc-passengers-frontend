package models

import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import services.{CountriesService, CurrencyService}

import scala.math.BigDecimal.RoundingMode
import util._
import uk.gov.hmrc.play.mappers.DateTuple._



object EuCountryCheckDto {
  val form: Form[EuCountryCheckDto] = Form(
    mapping(
     "euCountryCheck" -> optional(text).verifying("error.eu_check", x => x.fold(false)(_.nonEmpty)).transform[String](_.get, s => Some(s))
    )(EuCountryCheckDto.apply)(EuCountryCheckDto.unapply)
  )
}

case class EuCountryCheckDto(euCountryCheck: String)

object SelectedCountryDto {
  def form(countryService: CountriesService, optionalItemsRemaining: Boolean = true): Form[SelectedCountryDto] = Form(
    mapping(
      "country" -> text.verifying("error.country.invalid", countryName => countryService.isValidCountryName(countryName)),
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

object VolumeDto {

  val form: Form[VolumeDto] = Form(
    mapping(
      "volume" -> bigDecimal
    )(VolumeDto.apply)(VolumeDto.unapply)
  )

}
case class VolumeDto(volume: BigDecimal)

object QuantityDto {
  val form: Form[QuantityDto] = Form(
    mapping(
      "quantity" -> number
    )(QuantityDto.apply)(QuantityDto.unapply)
  )
}
case class QuantityDto(quantity: Int)

object NoOfSticksDto {
  val form: Form[NoOfSticksDto] = Form(
    mapping(
      "noOfSticks" -> number
    )(NoOfSticksDto.apply)(NoOfSticksDto.unapply)
  )
}
case class NoOfSticksDto(noOfSticks: Int)

object NoOfSticksAndWeightDto {
  val form: Form[NoOfSticksAndWeightDto] = Form(
    mapping(
      "noOfSticks" -> number,
      "weight" -> bigDecimal.transform[BigDecimal](grams => grams/1000, kilos => BigDecimal(decimalFormat10.format(kilos * 1000)))
    )(NoOfSticksAndWeightDto.apply)(NoOfSticksAndWeightDto.unapply)
  )
}
case class NoOfSticksAndWeightDto(noOfSticks: Int, weight: BigDecimal)

object WeightDto {
  val form: Form[WeightDto] = Form(
    mapping(
      "weight" -> bigDecimal.transform[BigDecimal](grams => grams/1000, kilos => BigDecimal(decimalFormat10.format(kilos * 1000)))
    )(WeightDto.apply)(WeightDto.unapply)
  )
}
case class WeightDto(weight: BigDecimal)

object CurrencyDto {
  def form(currencyService: CurrencyService, optionalItemsRemaining: Boolean = true): Form[CurrencyDto] = Form(
    mapping(
      "currency" -> text.verifying("error.invalid_currency", code => currencyService.isValidCurrencyCode(code)),
      "itemsRemaining" -> optional(number).verifying("error.required", i => optionalItemsRemaining || i.isDefined).transform[Int](_.getOrElse(0), i => Some(i))
    )(CurrencyDto.apply)(CurrencyDto.unapply)
  )
}
case class CurrencyDto(currency: String, itemsRemaining: Int)

object CostDto {
  def form(optionalItemsRemaining: Boolean = true): Form[CostDto] = Form(
    mapping(
      "cost" -> bigDecimal,
      "itemsRemaining" -> optional(number).verifying("error.required", i => optionalItemsRemaining || i.isDefined).transform[Int](_.getOrElse(0), i => Some(i))
    )(CostDto.apply)(CostDto.unapply)
  )
}
case class CostDto(cost: BigDecimal, itemsRemaining: Int)

case class CalculatorResponseDto(bands: Map[String, List[Item]], calculation: Calculation, hasOnlyGBP: Boolean)

object EnterYourDetailsDto {
  val form: Form[EnterYourDetailsDto] = Form(
    mapping(
      "firstName" -> nonEmptyText(1,35),
      "lastName" -> nonEmptyText(1,35),
      "passportNumber" -> nonEmptyText(1,40),
      "placeOfArrival" -> nonEmptyText(1,40),
      "dateOfArrival" -> mandatoryDateTuple("error.enter_a_date")
    )(EnterYourDetailsDto.apply)(EnterYourDetailsDto.unapply)
  )
}
case class EnterYourDetailsDto(firstName: String, lastName: String, passportNumber: String, placeOfArrival: String, dateOfArrival: LocalDate)
