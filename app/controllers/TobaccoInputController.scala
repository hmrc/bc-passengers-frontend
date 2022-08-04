/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.{DashboardAction, PublicAction}

import javax.inject.Inject
import models.{ProductPath, TobaccoDto}
import play.api.data.Form
import play.api.data.Forms.{mapping, text, _}
import play.api.i18n.I18nSupport
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TobaccoInputController @Inject()(
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,

  publicAction: PublicAction,
  dashboardAction: DashboardAction,

  val error_template: views.html.error_template,
  val tobacco_input: views.html.tobacco.tobacco_input,
  val weight_or_volume_input: views.html.tobacco.weight_or_volume_input,
  val no_of_sticks_input: views.html.tobacco.no_of_sticks_input,
  val no_of_sticks_weight_or_volume_input: views.html.tobacco.no_of_sticks_weight_or_volume_input,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  val resilientForm: Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks" -> optional(text).transform[Option[Int]](_.fold(Some(0))(x => Some(Try(x.toInt).getOrElse(Int.MaxValue))), _.map(_.toString)),
      "weightOrVolume" -> optional(text).transform[Option[BigDecimal]](_.map(x => Try(BigDecimal(x)/1000).getOrElse(0)), _.map(_.toString)),
      "country" -> ignored(""),
      "originCountry" -> optional(text),
      "currency" -> ignored(""),
      "cost" -> ignored(BigDecimal(0)),
      "isVatPaid" -> optional(boolean),
      "isExcisePaid" -> optional(boolean),
      "isCustomPaid" -> optional(boolean),
      "hasEvidence" -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def weightOrVolumeNoOfSticksForm(path: ProductPath): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks" -> text
        .verifying("error.no_of_sticks.required." + path.toMessageKey, noOfSticks => !noOfSticks.isEmpty)
        .verifying("error.invalid.characters.noofsticks." + path.toMessageKey, noOfSticks => noOfSticks.isEmpty || (Try(BigInt(noOfSticks) > 0).getOrElse(false)) )
        .transform[Option[Int]](noOfSticks => Some(Try(noOfSticks.toInt).toOption.getOrElse(Integer.MAX_VALUE)), int => int.mkString),
      "weightOrVolume" -> optional(text)
        .verifying("error.weight_or_volume.required." + path.toMessageKey, weightOrVolume => weightOrVolume.isDefined)
        .verifying("error.invalid.characters.weight", weightOrVolume => !weightOrVolume.isDefined || weightOrVolume.flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0)).getOrElse(false))
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(x)), kilos => kilos.map( x => x.toString ))
        .verifying("error.max.decimal.places.weight", weightOrVolume => weightOrVolume.fold(true)(x => x.scale  <= 2))
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(decimalFormat5.format(x.toDouble/1000))), kilos => kilos.map(x => BigDecimal(decimalFormat5.format(x * 1000)))),
      "country" -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry" -> optional(text),
      "currency" -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost" -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid" -> optional(boolean),
      "isExcisePaid" -> optional(boolean),
      "isCustomPaid" -> optional(boolean),
      "hasEvidence" -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def noOfSticksForm(path: ProductPath): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks" -> text
        .verifying("error.no_of_sticks.required." + path.toMessageKey, noOfSticks => !noOfSticks.isEmpty)
        .verifying("error.invalid.characters.noofsticks." + path.toMessageKey, noOfSticks => noOfSticks.isEmpty || (Try(BigInt(noOfSticks) > 0).getOrElse(false)) )
        .transform[Option[Int]](noOfSticks => Some(Try(noOfSticks.toInt).toOption.getOrElse(Integer.MAX_VALUE)), int => int.mkString),
      "weightOrVolume" -> ignored[Option[BigDecimal]](None),
      "country" -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry" -> optional(text),
      "currency" -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost" -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid" -> optional(boolean),
      "isExcisePaid" -> optional(boolean),
      "isCustomPaid" -> optional(boolean),
      "hasEvidence" -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def weightOrVolumeForm(path: ProductPath): Form[TobaccoDto] = Form(
    mapping("noOfSticks" -> ignored[Option[Int]](None),
      "weightOrVolume" -> optional(text)
        .verifying("error.required.weight." + path.toMessageKey, weightOrVolume => weightOrVolume.isDefined)
        .verifying("error.invalid.characters.weight", weightOrVolume => weightOrVolume.isEmpty || weightOrVolume.flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0)).getOrElse(false))
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(x)), kilos => kilos.map( x => x.toString ))
        .verifying("error.max.decimal.places.weight", weightOrVolume => weightOrVolume.fold(true)(x => x.scale  <= 2))
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(decimalFormat5.format(x.toDouble/1000))), kilos => kilos.map(x => BigDecimal(decimalFormat5.format(x * 1000)))),
      "country" -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry" -> optional(text),
      "currency" -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost" -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid" -> optional(boolean),
      "isExcisePaid" -> optional(boolean),
      "isCustomPaid" -> optional(boolean),
      "hasEvidence" -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def displayNoOfSticksAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if(context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")){
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    }
    else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry =>
          defaultOriginCountry =>
            defaultCurrency =>
              Future.successful(Ok(no_of_sticks_input(noOfSticksForm(path).bind(Map("country" -> defaultCountry.getOrElse(""), "originCountry" -> defaultOriginCountry.getOrElse(""), "currency" -> defaultCurrency.getOrElse(""))).discardingErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
        }
      }
    }
  }


  def displayWeightAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if(context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")){
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    }
    else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry =>
          defaultOriginCountry =>
            defaultCurrency =>
              Future.successful(Ok(weight_or_volume_input(weightOrVolumeForm(path).bind(Map("country" -> defaultCountry.getOrElse(""), "originCountry" -> defaultOriginCountry.getOrElse(""), "currency" -> defaultCurrency.getOrElse(""))).discardingErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
        }
      }
    }
  }

  def displayNoOfSticksWeightAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if(context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")){
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    }
    else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry =>
          defaultOriginCountry =>
            defaultCurrency =>
              Future.successful(Ok(no_of_sticks_weight_or_volume_input(weightOrVolumeNoOfSticksForm(path).bind(Map("country" -> defaultCountry.getOrElse(""), "originCountry" -> defaultOriginCountry.getOrElse(""), "currency" -> defaultCurrency.getOrElse(""))).discardingErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
        }
      }
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        TobaccoDto.fromPurchasedProductInstance(ppi).fold(logAndRenderError("Unable to construct dto from PurchasedProductInstance")) { dto =>
          Future.successful {
            product.templateId match {
              case "cigarettes" =>
                Ok(no_of_sticks_input(noOfSticksForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck))
              case "tobacco" =>
                Ok(weight_or_volume_input(weightOrVolumeForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck))
              case _ =>
                Ok(no_of_sticks_weight_or_volume_input(weightOrVolumeNoOfSticksForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck))
            }
          }
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>

    requireLimitUsage({
      val dto = resilientForm.bindFromRequest.value.get
      newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, List(dto.cost))._1
    }) { limits =>
      requireProduct(path) { product =>
        def processNoOfSticksAddForm = {
          noOfSticksForm(path).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(no_of_sticks_input(formWithErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
            },
            dto => {
              if (calculatorLimitConstraintOptionInt(limits, product.applicableLimits)) {
                val item =  newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, List(dto.cost))
                cache.store( item._1 ) map { _ =>
                  (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                    case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path,item._2))
                    case (Some(false), Some("euOnly")) => {
                      if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                        Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, item._2))
                      } else {
                        Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                    case _ => Redirect(routes.SelectProductController.nextStep)
                  }
                }
              } else
                Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(path)))
            }
          )
        }
        def processWeightAddForm = {
          weightOrVolumeForm(path).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(weight_or_volume_input(formWithErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
            },
            dto => {
              if (calculatorLimitConstraintOptionBigDecimal(limits, product.applicableLimits)) {
                  val item = newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, List(dto.cost))
                  cache.store( item._1 ) map { _ =>
                    (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                      case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path,item._2))
                      case (Some(false), Some("euOnly")) => {
                        if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                          Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, item._2))
                        } else {
                          Redirect(routes.SelectProductController.nextStep)
                        }
                      }
                      case _ => Redirect(routes.SelectProductController.nextStep)
                    }
                  }
              } else
                Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(path)))
            }
          )
        }
        def processNoOfSticksWeightAddForm = {
          weightOrVolumeNoOfSticksForm(path).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(no_of_sticks_weight_or_volume_input(formWithErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
            },
            dto => {
              if (calculatorLimitConstraintOptionInt(limits, product.applicableLimits)) {
                val item = newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, List(dto.cost))
                cache.store( item._1 ) map { _ =>
                  (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                    case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path,item._2))
                    case (Some(false), Some("euOnly")) => {
                      if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                        Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, item._2))
                      } else {
                        Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                    case _ => Redirect(routes.SelectProductController.nextStep)
                  }
                }
              } else
                Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(path)))
            }
          )
        }

        product.templateId match {
          case "cigarettes" =>
            processNoOfSticksAddForm
          case "tobacco" =>
            processWeightAddForm
          case _ =>
            processNoOfSticksWeightAddForm
        }
      }
    }
  }


  def processEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>

    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>

        requireLimitUsage({
          val dto = resilientForm.bindFromRequest.value.get
          newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, dto.cost)
        }) { limits =>

            def processCigarettesEditForm = {
              noOfSticksForm(ppi.path).bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(no_of_sticks_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
                },
                dto => {
                  if (calculatorLimitConstraintOptionInt(limits, product.applicableLimits)) {
                    cache.store(newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, dto.cost)) map { _ =>
                      (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                        case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                        case (Some(false), Some("euOnly")) => {
                          if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                            Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                          } else {
                            Redirect(routes.SelectProductController.nextStep)
                          }
                        }
                        case _ => Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                  } else
                    Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(ppi.path)))
                }
              )
            }

            def processTobaccoEditForm = {
              weightOrVolumeForm(ppi.path).bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(weight_or_volume_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
                },
                dto => {
                  if (calculatorLimitConstraintOptionBigDecimal(limits, product.applicableLimits)) {
                    cache.store(newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, dto.cost)) map { _ =>
                      (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                        case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                        case (Some(false), Some("euOnly")) => {
                          if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                            Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                          } else {
                            Redirect(routes.SelectProductController.nextStep)
                          }
                        }
                        case _ => Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                  } else
                    Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(ppi.path)))
                }
              )
            }

            def processOtherTobaccoEditForm = {
              weightOrVolumeNoOfSticksForm(ppi.path).bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(no_of_sticks_weight_or_volume_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
                },
                dto => {
                  if (calculatorLimitConstraintOptionInt(limits, product.applicableLimits)) {
                    cache.store(newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, dto.cost)) map { _ =>
                      (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                        case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                        case (Some(false), Some("euOnly")) => {
                          if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                            Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                          } else {
                            Redirect(routes.SelectProductController.nextStep)
                          }
                        }
                        case _ => Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                  } else
                    Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(ppi.path)))
                }
              )
            }

            product.templateId match {
              case "cigarettes" =>
                processCigarettesEditForm
              case "tobacco" =>
                processTobaccoEditForm
              case _ =>
                processOtherTobaccoEditForm
            }
          }
        }
      }
    }
}
