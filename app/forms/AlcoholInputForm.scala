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

import models.{AlcoholDto, ProductPath}
import play.api.data.Form
import play.api.data.Forms._
import services._
import util._

import javax.inject.Inject
import scala.util.Try

class AlcoholInputForm @Inject() (
  countriesService: CountriesService,
  currencyService: CurrencyService
) {

  val resilientForm: Form[AlcoholDto] = Form(
    mapping(
      "weightOrVolume" -> optional(text)
        .transform[BigDecimal](_.flatMap(x => Try(BigDecimal(x)).toOption).getOrElse(0), _ => None),
      "country"        -> ignored(""),
      "originCountry"  -> optional(text),
      "currency"       -> ignored(""),
      "cost"           -> ignored(BigDecimal(0)),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(AlcoholDto.apply)(AlcoholDto.unapply)
  )

  def alcoholForm(path: ProductPath): Form[AlcoholDto] = Form(
    mapping(
      "weightOrVolume" -> optional(text)
        .verifying("error.required.volume." + path.toMessageKey, _.isDefined)
        .verifying(
          "error.invalid.characters.volume",
          x => x.isEmpty || x.flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0)).getOrElse(false)
        )
        .transform[BigDecimal](_.fold(BigDecimal(0))(x => BigDecimal(x)), x => Some(x.toString))
        .verifying("error.max.decimal.places.volume", _.scale <= 3)
        .transform[BigDecimal](identity, identity),
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
    )(AlcoholDto.apply)(AlcoholDto.unapply)
  )

}
