package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}


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
      "ageOver17" -> optional(boolean).verifying("error.required", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(AgeOver17Dto.apply)(AgeOver17Dto.unapply)
  )
}
case class AgeOver17Dto(ageOver17: Boolean)

object PrivateCraftDto {
  val form: Form[PrivateCraftDto] = Form(
    mapping(
      "privateCraft" -> optional(boolean).verifying("error.required", _.isDefined).transform[Boolean](_.get, b => Option(b))
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