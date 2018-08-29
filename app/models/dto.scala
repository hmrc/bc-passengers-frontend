package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import services.CurrencyService


object SelectedCountryDto {
  val form: Form[SelectedCountryDto] = Form(
    mapping(
      "country" -> text
    )(SelectedCountryDto.apply)(SelectedCountryDto.unapply)
  )
}
case class SelectedCountryDto(country: String)

object AgeOver17Dto {
  val form: Form[AgeOver17Dto] = Form(
    mapping(
      "ageOver17" -> optional(boolean).verifying("over_17.error", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(AgeOver17Dto.apply)(AgeOver17Dto.unapply)
  )
}
case class AgeOver17Dto(ageOver17: Boolean)


object PrivateCraftDto {

  val form: Form[PrivateCraftDto] = Form(
    mapping(
      "privateCraft" -> optional(boolean).verifying("private_craft.error", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(PrivateCraftDto.apply)(PrivateCraftDto.unapply)
  )
}
case class PrivateCraftDto(privateCraft: Boolean)


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
      "weight" -> bigDecimal
    )(NoOfSticksAndWeightDto.apply)(NoOfSticksAndWeightDto.unapply)
  )
}
case class NoOfSticksAndWeightDto(noOfSticks: Int, weight: BigDecimal)

object WeightDto {
  val form: Form[WeightDto] = Form(
    mapping(
      "weight" -> bigDecimal
    )(WeightDto.apply)(WeightDto.unapply)
  )
}
case class WeightDto(weight: BigDecimal)

object CurrencyDto {
  def form(currencyService: CurrencyService): Form[CurrencyDto] = Form(
    mapping(
      "currency" -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code))
    )(CurrencyDto.apply)(CurrencyDto.unapply)
  )
}
case class CurrencyDto(currency: String)

object CostDto {
  val form: Form[CostDto] = Form(
    mapping(
      "cost" -> bigDecimal
    )(CostDto.apply)(CostDto.unapply)
  )
}
case class CostDto(cost: BigDecimal)

