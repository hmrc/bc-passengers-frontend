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


  implicit class EnhancedJsObject(jsObject: JsObject) {
    def stripNulls: JsObject = {
      alterFields {
        case (_, JsNull) => None
      }
    }

    def alterFields(customOperation: PartialFunction[(String, JsValue),Option[(String, JsValue)]]): JsObject = {
      val standardOperation: PartialFunction[(String, JsValue),Option[(String, JsValue)]] = {
        case (name, jso: JsObject) => Some((name, jso.alterFields(customOperation)))
        case (name, jsa: JsArray) => Some((name, JsArray((jsa.value map {
          case JsNull => None
          case o: JsObject => Some(o.alterFields(customOperation))
          case v: JsValue => Some(v)
        }).flatten)))
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

  def bigDecimalCostCheckConstraint(errorSubString: String): Constraint[String] = Constraint("constraints.bigdecimalcostcheck")({
    plainText =>
      val errors = plainText match {
        case s if s == "" => Seq(ValidationError(s"error.required.${errorSubString}"))
        case s if !s.matches("[0-9]+(\\.[0-9]*)?$") || s.toDouble == "0.0".toDouble => Seq(ValidationError(s"error.invalid.characters.${errorSubString}"))
        case s if !s.matches("^[0-9]+(\\.[0-9]{1,2})?$") => Seq(ValidationError(s"error.invalid.format.${errorSubString}"))
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
