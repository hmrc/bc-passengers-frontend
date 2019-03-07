package models

import org.joda.time._
import org.joda.time.format.DateTimeFormat
import play.api.data.{Form, Mapping}
import play.api.data.Forms.{optional, _}
import play.api.data.validation._
import play.api.libs.json.Json
import services.{CalculatorService, CountriesService, CurrencyService}
import uk.gov.hmrc.http.HeaderCarrier
import util._

import scala.util.{Random, Try}

object WorkingPurchaseDataDto {

  def fromPurchasedProductInstance(purchasedProductInstance: PurchasedProductInstance) = for {
    country <- purchasedProductInstance.country
    currency <- purchasedProductInstance.currency
  } yield WorkingPurchaseDataDto("", country.countryName, currency, purchasedProductInstance.cost.toList)
}

case class WorkingPurchaseDataDto(
  action: String,
  country: String,
  currency: String,
  costs: List[BigDecimal] = List()
)

object EuCountryCheckDto {
  val form: Form[EuCountryCheckDto] = Form(
    mapping(
     "euCountryCheck" -> optional(text).verifying("error.eu_check", x => x.fold(false)(_.nonEmpty)).transform[String](_.get, s => Some(s))
    )(EuCountryCheckDto.apply)(EuCountryCheckDto.unapply)
  )
}

case class EuCountryCheckDto(euCountryCheck: String)


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
      "bringingDutyFree" -> optional(boolean).verifying("error.bringing_duty_free", _.isDefined).transform[Boolean](_.get, b => Option(b))
    )(BringingDutyFreeDto.apply)(BringingDutyFreeDto.unapply)
  )
}
case class BringingDutyFreeDto(bringingDutyFree: Boolean)

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

  def form(limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Form[VolumeDto] = Form(
    mapping(
      "volume" -> text
        .transform[String](s => if(s.headOption.contains('.')) "0"+s else s, s => s)
        .transform[String](s => if(s.lastOption.contains('.')) s+"0" else s, s => s)
        .verifying(bigDecimalCheckConstraint("volume", 3))
        .transform[BigDecimal](volume => BigDecimal(volume), bd => bd.toString())
        .verifying(calculatorLimitConstraintBigDecimal(limits, applicableLimits))
    )(VolumeDto.apply)(VolumeDto.unapply)
  )

}
case class VolumeDto(volume: BigDecimal)

object QuantityDto {

  val form: Form[QuantityDto] = Form(
    mapping(
      "quantity" -> text.verifying(quantityConstraint("quantity")).transform[Int](qty => qty.toInt, int => int.toString))(QuantityDto.apply)(QuantityDto.unapply)
  )
}
case class QuantityDto(quantity: Int)

object NoOfSticksDto {

  def form(limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Form[NoOfSticksDto] = Form(
    mapping(
      "noOfSticks" -> text
        .verifying(noOfSticksConstraint("noofsticks")).transform[Int](noOfSticks => noOfSticks.toInt, int => int.toString)
        .verifying(calculatorLimitConstraintInt(limits, applicableLimits))
    )(NoOfSticksDto.apply)(NoOfSticksDto.unapply)
  )
}
case class NoOfSticksDto(noOfSticks: Int)

object NoOfSticksAndWeightDto {

  def form(limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Form[NoOfSticksAndWeightDto] = Form(
    mapping(
      "noOfSticks" -> text
        .verifying(noOfSticksConstraint("noofsticks")).transform[Int](noOfSticks => noOfSticks.toInt, int => int.toString)
        .verifying(calculatorLimitConstraintInt(limits, applicableLimits)),
      "weight" -> text
        .transform[String](s => if(s.headOption.contains('.')) "0"+s else s, s => s)
        .transform[String](s => if(s.lastOption.contains('.')) s+"0" else s, s => s)
        .verifying(bigDecimalCheckConstraint("weight", 2))
        .transform[BigDecimal](grams => BigDecimal(decimalFormat5.format(grams.toDouble/1000)), kilos => BigDecimal(decimalFormat5.format(kilos * 1000)).toString())
    )(NoOfSticksAndWeightDto.apply)(NoOfSticksAndWeightDto.unapply)
  )
}
case class NoOfSticksAndWeightDto(noOfSticks: Int, weight: BigDecimal)

object WeightDto {

  def form(limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Form[WeightDto] = Form(
    mapping(
      "weight" -> text
        .transform[String](s => if(s.headOption.contains('.')) "0"+s else s, s => s)
        .transform[String](s => if(s.lastOption.contains('.')) s+"0" else s, s => s)
        .verifying(bigDecimalCheckConstraint("weight", 2))
        .transform[BigDecimal](grams => BigDecimal(decimalFormat5.format(grams.toDouble/1000)), kilos => BigDecimal(decimalFormat5.format(kilos * 1000)).toString())
        .verifying(calculatorLimitConstraintBigDecimal(limits, applicableLimits))
    )(WeightDto.apply)(WeightDto.unapply)
  )

}
case class WeightDto(weight: BigDecimal)

object CurrencyDto {
  def form(currencyService: CurrencyService, optionalItemsRemaining: Boolean = true): Form[CurrencyDto] = Form(
    mapping(
      "currency" -> text
        .transform[String](s => currencyService.getCodeByDisplayName(s).getOrElse(""), s => currencyService.getDisplayNameByCode(s).getOrElse(""))
        .verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "itemsRemaining" -> optional(number).verifying("error.required", i => optionalItemsRemaining || i.isDefined).transform[Int](_.getOrElse(0), i => Some(i))
    )(CurrencyDto.apply)(CurrencyDto.unapply)
  )
}
case class CurrencyDto(currency: String, itemsRemaining: Int)

object CostDto {

  def form(optionalItemsRemaining: Boolean = true): Form[CostDto] = Form(
    mapping(
      "cost" -> text
        .transform[String](s => if (s.lastOption.contains('.')) (s + "00").replace(",", "") else s.replace(",", ""), s => s)
        .verifying(bigDecimalCostCheckConstraint("cost"))
        .transform[BigDecimal](cost => BigDecimal(cost), bd => formatMonetaryValue(bd)),
      "itemsRemaining" -> optional(number).verifying("error.required", i => optionalItemsRemaining || i.isDefined).transform[Int](_.getOrElse(0), i => Some(i))
    )(CostDto.apply)(CostDto.unapply)
  )
}
case class CostDto(cost: BigDecimal, itemsRemaining: Int)

case class CalculatorResponseDto(items: List[Item], calculation: Calculation, allItemsUseGBP: Boolean)

object EnterYourDetailsDto {

  private def nonEmptyMaxLength(maxLength: Int, fieldName: String): Constraint[String] = Constraint("constraint.required") {
    text =>
      if (text.isEmpty) Invalid(ValidationError(s"error.required.$fieldName"))
      else if (text.length > maxLength) Invalid(ValidationError(s"error.max-length.$fieldName"))
      else Valid
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

  def form(declarationTime: DateTime): Form[EnterYourDetailsDto] = Form(
    mapping(
      "firstName" -> text.verifying(nonEmptyMaxLength(35, "first_name")),
      "lastName" -> text.verifying(nonEmptyMaxLength(35, "last_name")),
      "passportNumber" -> text.verifying(nonEmptyMaxLength(40, "passport_number")),
      "placeOfArrival" -> text.verifying(nonEmptyMaxLength(40, "place_of_arrival")),
      "dateTimeOfArrival" -> mapping(
        "dateOfArrival" -> mandatoryDate("error.enter_a_date"),
        "timeOfArrival" -> mandatoryTime("error.enter_a_time")
      )(DateTimeOfArrival.apply)(DateTimeOfArrival.unapply)
        .verifying("error.not_in_past", dto => dto.dateOfArrival.toDateTime(dto.timeOfArrival).isAfter(declarationTime.minus(Hours.THREE)))
        .verifying("error.72_hours", dto => dto.dateOfArrival.toDateTime(dto.timeOfArrival).isBefore(declarationTime.plus(Days.THREE)))
    )(EnterYourDetailsDto.apply)(EnterYourDetailsDto.unapply)
  )
}

case class DateTimeOfArrival(dateOfArrival: LocalDate, timeOfArrival: LocalTime)

case class EnterYourDetailsDto(firstName: String, lastName: String, passportNumber: String, placeOfArrival: String, dateTimeOfArrival: DateTimeOfArrival)




