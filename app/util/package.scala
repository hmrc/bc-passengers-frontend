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

import models.JourneyData
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, JsNull, JsObject, JsValue}
import utils.ProductDetector

import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}
import java.util.Locale
import scala.util.Try

package object util extends ProductDetector {

  val decimalFormat10: DecimalFormat = {
    val df = new DecimalFormat("0.##########")
    df.setRoundingMode(RoundingMode.DOWN)
    df
  }

  val decimalFormat5: DecimalFormat = {
    val df = new DecimalFormat("0.#####")
    df.setRoundingMode(RoundingMode.HALF_UP)
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
              JsArray(jsa.value.flatMap {
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

  def looseTobaccoWeightConstraint(looseTobaccoWeight: BigDecimal): Boolean = {
    val oneThousandGrams: BigDecimal = 1000
    looseTobaccoWeight <= oneThousandGrams
  }

  def alcoholVolumeConstraint(journeyData: JourneyData, alcoholVolume: BigDecimal, productToken: String): Boolean = {
    val wineLimit: BigDecimal          = 90
    val sparklingWineLimit: BigDecimal = if (checkProductExists(journeyData, "alcohol/wine")) 90 else 60
    val beerLimit: BigDecimal          = 110
    val spiritsLimit: BigDecimal       = 10
    val ciderOrOtherLimit: BigDecimal  = 20

    productToken match {
      case "wine"           => alcoholVolume <= wineLimit
      case "sparkling-wine" => alcoholVolume <= sparklingWineLimit
      case "beer"           => alcoholVolume <= beerLimit
      case "spirits"        => alcoholVolume <= spiritsLimit
      case _                => alcoholVolume <= ciderOrOtherLimit
    }
  }

  def calculatorLimitConstraint(
    limits: Map[String, BigDecimal] = Map.empty,
    applicableLimits: List[String] = Nil
  ): Boolean = {

    val errors =
      for (limit <- applicableLimits; amount <- limits.get(limit) if amount > BigDecimal(1.0)) yield (limit, amount)

    errors.isEmpty
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

  // TODO this should be removed in 30 days after it was released in prod 1 Feb 2025
  private val oldLocalTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:m a", Locale.UK)
  private val localTimeFormat: DateTimeFormatter    = DateTimeFormatter.ofPattern("H:m", Locale.UK)
  private val localDateFormat: DateTimeFormatter    = DateTimeFormatter.ofPattern("yyyy-M-d", Locale.UK)

  def parseLocalTime(time: String): LocalTime =
    if (time.toLowerCase.contains("am") || time.toLowerCase.contains("pm")) {
      LocalTime.parse(time, oldLocalTimeFormat)
    } else {
      LocalTime.parse(time, localTimeFormat)
    }

  def formatLocalTime(time: LocalTime): String =
    time.format(localTimeFormat)

  def parseLocalDate(date: String): LocalDate =
    LocalDate.parse(date, localDateFormat)

  def formatLocalDate(date: LocalDate, pattern: String): String =
    date.format(DateTimeFormatter.ofPattern(pattern, Locale.UK))
}
