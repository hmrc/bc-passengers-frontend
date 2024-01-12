/*
 * Copyright 2024 HM Revenue & Customs
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

package forms

import models.{ProductPath, TobaccoDto}
import play.api.data.Form
import play.api.data.Forms._
import services._
import util._

import javax.inject.Inject
import scala.util.Try

class TobaccoInputForm @Inject() (
  countriesService: CountriesService,
  currencyService: CurrencyService
) {

  val resilientForm: Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks"     -> optional(text)
        .transform[Option[Int]](_.fold(Some(0))(x => Some(Try(x.toInt).getOrElse(Int.MaxValue))), _.map(_.toString)),
      "weightOrVolume" -> optional(text)
        .transform[Option[BigDecimal]](_.map(x => Try(BigDecimal(x) / 1000).getOrElse(0)), _.map(_.toString)),
      "country"        -> ignored(""),
      "originCountry"  -> optional(text),
      "currency"       -> ignored(""),
      "cost"           -> ignored(BigDecimal(0)),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def cigarAndCigarilloForm(path: ProductPath): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks"     -> text
        .verifying("error.no_of_sticks.required." + path.toMessageKey, noOfSticks => noOfSticks.nonEmpty)
        .verifying(
          "error.invalid.characters.noofsticks." + path.toMessageKey,
          noOfSticks => noOfSticks.isEmpty || Try(BigInt(noOfSticks) > 0).getOrElse(false)
        )
        .transform[Option[Int]](
          noOfSticks => Some(Try(noOfSticks.toInt).toOption.getOrElse(Integer.MAX_VALUE)),
          int => int.mkString
        ),
      "weightOrVolume" -> optional(text)
        .verifying("error.weight_or_volume.required." + path.toMessageKey, weightOrVolume => weightOrVolume.isDefined)
        .verifying(
          "error.invalid.characters.weight",
          weightOrVolume =>
            weightOrVolume.isEmpty || weightOrVolume
              .flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0))
              .getOrElse(false)
        )
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(x)), kilos => kilos.map(x => x.toString))
        .verifying("error.max.decimal.places.weight", weightOrVolume => weightOrVolume.fold(true)(x => x.scale <= 2))
        .transform[Option[BigDecimal]](
          grams => grams.map(x => (x / 1000).setScale(2, BigDecimal.RoundingMode.HALF_UP)),
          kilos => kilos.map(x => (x * 1000).setScale(2, BigDecimal.RoundingMode.HALF_UP))
        ),
      "country"        -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry"  -> optional(text),
      "currency"       -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost"           -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def cigaretteAndHeatedTobaccoForm(path: ProductPath): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks"     -> text
        .verifying("error.no_of_sticks.required." + path.toMessageKey, noOfSticks => noOfSticks.nonEmpty)
        .verifying(
          "error.invalid.characters.noofsticks." + path.toMessageKey,
          noOfSticks => noOfSticks.isEmpty || Try(BigInt(noOfSticks) > 0).getOrElse(false)
        )
        .transform[Option[Int]](
          noOfSticks => Some(Try(noOfSticks.toInt).toOption.getOrElse(Integer.MAX_VALUE)),
          int => int.mkString
        ),
      "weightOrVolume" -> ignored[Option[BigDecimal]](None),
      "country"        -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry"  -> optional(text),
      "currency"       -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost"           -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def looseTobaccoWeightForm(path: ProductPath): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks"     -> ignored[Option[Int]](None),
      "weightOrVolume" -> optional(text)
        .verifying("error.required.weight." + path.toMessageKey, weightOrVolume => weightOrVolume.isDefined)
        .verifying(
          "error.invalid.characters.weight",
          weightOrVolume =>
            weightOrVolume.isEmpty || weightOrVolume
              .flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0))
              .getOrElse(false)
        )
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(x)), kilos => kilos.map(x => x.toString))
        .verifying("error.max.decimal.places.weight", weightOrVolume => weightOrVolume.fold(true)(x => x.scale <= 2))
        .transform[Option[BigDecimal]](
          grams => grams.map(x => (x / 1000)),
          kilos => kilos.map(x => (x * 1000).setScale(2, BigDecimal.RoundingMode.HALF_UP))
        ),
      "country"        -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry"  -> optional(text),
      "currency"       -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost"           -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

}
