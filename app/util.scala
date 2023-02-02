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

import models.ProductPath
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, JsNull, JsObject, JsValue}

import java.math.RoundingMode
import java.text.DecimalFormat
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
    val df            = new DecimalFormat("#,###.00")
    val monetaryValue = BigDecimal(value)
    if (monetaryValue < 1) "0" + df.format(BigDecimal(value)) else df.format(BigDecimal(value))
  }

  def formatMonetaryValue(value: BigDecimal): String = {
    val df = new DecimalFormat("#,###.00")
    if (value < 1) "0" + df.format(value) else df.format(value)
  }

  implicit class EnhancedJsObject(jsObject: JsObject) {
    def stripNulls: JsObject =
      alterFields { case (_, JsNull) =>
        None
      }

    def alterFields(customOperation: PartialFunction[(String, JsValue), Option[(String, JsValue)]]): JsObject = {
      val standardOperation: PartialFunction[(String, JsValue), Option[(String, JsValue)]] = {
        case (name, jso: JsObject) => Some((name, jso.alterFields(customOperation)))
        case (name, jsa: JsArray)  =>
          Some(
            (
              name,
              JsArray(jsa.value flatMap {
                case JsNull      => None
                case o: JsObject => Some(o.alterFields(customOperation))
                case v: JsValue  => Some(v)
              })
            )
          )
        case (name, jsv)           => Some((name, jsv))
      }
      val executePartials: PartialFunction[(String, JsValue), Option[(String, JsValue)]]   =
        customOperation orElse standardOperation
      JsObject(jsObject.fields.flatMap(executePartials(_).toList))
    }
  }

  def bigDecimalCheckConstraint(errorSubString: String, decimalPlaces: Int): Constraint[String] =
    Constraint("constraints.bigdecimalcheck") { plainText =>
      val errors = plainText match {
        case s if s == ""                                               => Seq(ValidationError(s"error.required.$errorSubString"))
        case s if Try(s.toDouble).toOption.fold(true)(d => d <= 0.0)    =>
          Seq(ValidationError(s"error.invalid.characters.$errorSubString"))
        case s if !s.matches(s"^[0-9]+(\\.[0-9]{1,$decimalPlaces})?$$") =>
          Seq(ValidationError(s"error.invalid.format.$errorSubString"))
        case _                                                          => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    }

  def calculatorLimitConstraintOptionInt(
    limits: Map[String, BigDecimal] = Map.empty,
    applicableLimits: List[String] = Nil
  ): Boolean = {

    val errors =
      for (limit <- applicableLimits; amount <- limits.get(limit) if amount > BigDecimal(1.0)) yield (limit, amount)

    if (errors.nonEmpty) false else true
  }

  def calculatorLimitConstraintOptionBigDecimal(
    limits: Map[String, BigDecimal] = Map.empty,
    applicableLimits: List[String] = Nil
  ): Boolean = {

    val errors =
      for (limit <- applicableLimits; amount <- limits.get(limit) if amount > BigDecimal(1.0)) yield (limit, amount)

    if (errors.nonEmpty) false else true
  }

  def calculatorLimitConstraintBigDecimal(
    limits: Map[String, BigDecimal] = Map.empty,
    applicableLimits: List[String] = Nil,
    path: ProductPath
  ): Option[ProductPath] = {

    val errors =
      for (limit <- applicableLimits; amount <- limits.get(limit); if amount > BigDecimal(1.0)) yield (limit, amount)

    if (errors.isEmpty) { None }
    else {
      Some(
        errors
          .sortBy(_._2)
          .reverse
          .take(1)
          .map { x =>
            x._1.toLowerCase match {
              case "l-wine" => ProductPath("alcohol/wine")
              case _        => path
            }
          }
          .head
      )
    }
  }

  def bigDecimalCostCheckConstraint(errorSubString: String): Constraint[String] =
    Constraint("constraints.bigdecimalcostcheck") { plainText =>
      val errors = plainText match {
        case s if s == ""                                                      => Seq(ValidationError(s"error.required.$errorSubString"))
        case s if Try(BigDecimal(s)).isFailure || s.toDouble <= "0.0".toDouble =>
          Seq(ValidationError("error.invalid.characters"))
        case s if BigDecimal(s).scale > 2                                      => Seq(ValidationError("error.invalid.format"))
        case s if s.toDouble > "9999999999".toDouble                           => Seq(ValidationError("error.exceeded.max"))
        case _                                                                 => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    }

  def blankOkCostCheckConstraint(productPathMessageKey: String): Constraint[String] =
    Constraint("constraints.bigdecimalcostcheck") { plainText =>
      val sanitizedText = plainText.filter(_ != ',')

      val errors = sanitizedText match {
        case s if s == ""                                                      => Nil
        case s if Try(BigDecimal(s)).isFailure || s.toDouble <= "0.0".toDouble =>
          Seq(ValidationError(s"error.invalid.characters.cost.$productPathMessageKey"))
        case s if BigDecimal(s).scale > 2                                      => Seq(ValidationError(s"error.invalid.format.cost.$productPathMessageKey"))
        case s if s.toDouble > "9999999999".toDouble                           =>
          Seq(ValidationError(s"error.exceeded.max.cost.$productPathMessageKey"))
        case _                                                                 => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    }

  def formatYesNo(value: Boolean)(implicit messages: Messages): String =
    if (value) {
      messages("label.yes")
    } else {
      messages("label.no")
    }

  def prefixErrorMessage(title: String, hasError: Boolean)(implicit messages: Messages): String =
    if (hasError) {
      messages("label.error") + " " + title
    } else {
      title
    }

}
