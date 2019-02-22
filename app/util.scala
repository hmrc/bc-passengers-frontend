import java.math.RoundingMode
import java.text.DecimalFormat

import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.libs.json.{JsArray, JsNull, JsObject, JsValue}

import scala.util.Try

package object util {

  val decimalFormat10 = {
    val df = new DecimalFormat("0.##########")
    df.setRoundingMode(RoundingMode.DOWN)
    df
  }

  val decimalFormat5 = {
    val df = new DecimalFormat("0.#####")
    df.setRoundingMode(RoundingMode.UP)
    df
  }

  def formatMonetaryValue(value: String): String = {
    val df = new DecimalFormat("#,###.00")
    val monetaryValue = BigDecimal(value)
    if (monetaryValue < 1) "0" + df.format(BigDecimal(value)) else df.format(BigDecimal(value))
  }

  def formatMonetaryValue(value: BigDecimal): String = {
    val df = new DecimalFormat("#,###.00")
    if (value < 1) "0" + df.format(value) else df.format(value)
  }

  implicit class EnhancedJsObject(jsObject: JsObject) {
    def stripNulls: JsObject = {
      alterFields {
        case (_, JsNull) => None
      }
    }

    def alterFields(customOperation: PartialFunction[(String, JsValue),Option[(String, JsValue)]]): JsObject = {
      val standardOperation: PartialFunction[(String, JsValue),Option[(String, JsValue)]] = {
        case (name, jso: JsObject) => Some((name, jso.alterFields(customOperation)))
        case (name, jsa: JsArray) => Some((name, JsArray(jsa.value flatMap {
          case JsNull => None
          case o: JsObject => Some(o.alterFields(customOperation))
          case v: JsValue => Some(v)
        })))
        case (name, jsv) => Some((name, jsv))
      }
      val executePartials: PartialFunction[(String, JsValue), Option[(String, JsValue)]] = customOperation orElse standardOperation
      JsObject(jsObject.fields.flatMap(executePartials(_).toList))
    }
  }

  def bigDecimalCheckConstraint(errorSubString: String, decimalPlaces: Int): Constraint[String] = Constraint("constraints.bigdecimalcheck")({
    plainText =>
      val errors = plainText match {
        case s if s == "" => Seq(ValidationError(s"error.required.${errorSubString}"))
        case s if Try(s.toDouble).toOption.fold(true)(d => d <= 0.0) => Seq(ValidationError(s"error.invalid.characters.${errorSubString}"))
        case s if !s.matches(s"^[0-9]+(\\.[0-9]{1,${decimalPlaces}})?$$") => Seq(ValidationError(s"error.invalid.format.${errorSubString}"))
        case _ => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
  })

  def calculatorLimitConstraintInt(limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Constraint[Int] = Constraint("constraints.calclimit") {

    calculatorLimitConstraintBigDecimal( limits.mapValues(i => i), applicableLimits )(_)
  }

  def calculatorLimitConstraintBigDecimal(limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Constraint[BigDecimal] = Constraint("constraints.calclimit") { _ =>

    val errors = for(limit <- applicableLimits; amount <- limits.get(limit) if amount > BigDecimal(1.0)) yield (limit, amount)

    if(errors.isEmpty) Valid
    else Invalid(errors.sortBy(_._2).reverse.take(1).map(x => ValidationError(s"error.${x._1.toLowerCase}.limit-exceeded")))

  }

  def bigDecimalCostCheckConstraint(errorSubString: String): Constraint[String] = Constraint("constraints.bigdecimalcostcheck")({
    plainText =>
      val errors = plainText match {
        case s if s == "" => Seq(ValidationError(s"error.required.${errorSubString}"))
        case s if !s.matches("^(\\d+|\\d{1,3}(,\\d{3})*)(\\.\\d*)?$") || s.toDouble == "0.0".toDouble => Seq(ValidationError(s"error.invalid.characters.${errorSubString}"))
        case s if !s.matches("^(\\d+|\\d{1,3}(,\\d{3})*)(\\.\\d{1,2})?$") => Seq(ValidationError(s"error.invalid.format.${errorSubString}"))
        case s if s.toDouble > "9999999999".toDouble => Seq(ValidationError(s"error.exceeded.max.${errorSubString}"))
        case _ => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
  })

  def noOfSticksConstraint(errorSubString: String): Constraint[String] = Constraint("constraints.noofsticks")({
    plainText =>
      val errors = plainText match {
        case s if s == "" => Seq(ValidationError(s"error.required.${errorSubString}"))
        case s if !s.matches("^[0-9]*$") || s.toDouble == "0.0".toDouble => Seq(ValidationError(s"error.invalid.characters.${errorSubString}"))
        case _ => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
  })

  def quantityConstraint(errorSubString: String): Constraint[String] = Constraint("constraints.quantity")({
    plainText =>
      val errors = plainText match {
        case s if s == "" => Seq(ValidationError(s"error.required.${errorSubString}"))
        case s if !s.matches("^[0-9]*$") || s.toDouble == "0.0".toDouble => Seq(ValidationError(s"error.invalid.characters.${errorSubString}"))
        case _ => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
  })


}
