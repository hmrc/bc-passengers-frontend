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

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.{DashboardAction, PublicAction}
import controllers.ControllerHelpers
import controllers.enforce.DashboardAction
import forms.TobaccoInputForm
import models.{ProductPath, TobaccoDto}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TobaccoInputController @Inject() (
  val tobaccoForm: TobaccoInputForm,
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,
  dashboardAction: DashboardAction,
  val errorTemplate: views.html.errorTemplate,
  val tobacco_input: views.html.tobacco.tobacco_input,
  val weight_or_volume_input: views.html.tobacco.weight_or_volume_input,
  val no_of_sticks_input: views.html.tobacco.no_of_sticks_input,
  val no_of_sticks_weight_or_volume_input: views.html.tobacco.no_of_sticks_weight_or_volume_input,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  def displayNoOfSticksAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(
            Ok(
              no_of_sticks_input(
                tobaccoForm
                  .noOfSticksForm(path)
                  .bind(
                    Map(
                      "country"       -> defaultCountry.getOrElse(""),
                      "originCountry" -> defaultOriginCountry.getOrElse(""),
                      "currency"      -> defaultCurrency.getOrElse("")
                    )
                  )
                  .discardingErrors,
                product,
                path,
                None,
                countriesService.getAllCountries,
                countriesService.getAllCountriesAndEu,
                currencyService.getAllCurrencies,
                context.getJourneyData.euCountryCheck
              )
            )
          )
        }
      }
    }
  }

  def displayWeightAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(
            Ok(
              weight_or_volume_input(
                tobaccoForm
                  .weightOrVolumeForm(path)
                  .bind(
                    Map(
                      "country"       -> defaultCountry.getOrElse(""),
                      "originCountry" -> defaultOriginCountry.getOrElse(""),
                      "currency"      -> defaultCurrency.getOrElse("")
                    )
                  )
                  .discardingErrors,
                product,
                path,
                None,
                countriesService.getAllCountries,
                countriesService.getAllCountriesAndEu,
                currencyService.getAllCurrencies,
                context.getJourneyData.euCountryCheck
              )
            )
          )
        }
      }
    }
  }

  def displayNoOfSticksWeightAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(
            Ok(
              no_of_sticks_weight_or_volume_input(
                tobaccoForm
                  .weightOrVolumeNoOfSticksForm(path)
                  .bind(
                    Map(
                      "country"       -> defaultCountry.getOrElse(""),
                      "originCountry" -> defaultOriginCountry.getOrElse(""),
                      "currency"      -> defaultCurrency.getOrElse("")
                    )
                  )
                  .discardingErrors,
                product,
                path,
                None,
                countriesService.getAllCountries,
                countriesService.getAllCountriesAndEu,
                currencyService.getAllCurrencies,
                context.getJourneyData.euCountryCheck
              )
            )
          )
        }
      }
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        TobaccoDto
          .fromPurchasedProductInstance(ppi)
          .fold(logAndRenderError("Unable to construct dto from PurchasedProductInstance")) { dto =>
            Future.successful {
              product.templateId match {
                case "cigarettes" =>
                  Ok(
                    no_of_sticks_input(
                      tobaccoForm.noOfSticksForm(ppi.path).fill(dto),
                      product,
                      ppi.path,
                      Some(iid),
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
                case "tobacco"    =>
                  Ok(
                    weight_or_volume_input(
                      tobaccoForm.weightOrVolumeForm(ppi.path).fill(dto),
                      product,
                      ppi.path,
                      Some(iid),
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
                case _            =>
                  Ok(
                    no_of_sticks_weight_or_volume_input(
                      form = tobaccoForm.weightOrVolumeNoOfSticksForm(ppi.path).fill(dto),
                      product = product,
                      path = ppi.path,
                      iid = Some(iid),
                      countries = countriesService.getAllCountries,
                      countriesEU = countriesService.getAllCountriesAndEu,
                      currencies = currencyService.getAllCurrencies,
                      journeyStart = context.getJourneyData.euCountryCheck
                    )
                  )
              }
            }
          }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireLimitUsage {
      val dto = tobaccoForm.resilientForm.bindFromRequest().value.get
      newPurchaseService
        .insertPurchases(
          path,
          dto.weightOrVolume,
          dto.noOfSticks,
          dto.country,
          dto.originCountry,
          dto.currency,
          List(dto.cost)
        )
        ._1
    } { limits =>
      requireProduct(path) { product =>
        def processNoOfSticksAddForm       =
          tobaccoForm
            .noOfSticksForm(path)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    no_of_sticks_input(
                      formWithErrors,
                      product,
                      path,
                      None,
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
                ),
              dto =>
                if (calculatorLimitConstraintOptionInt(limits, product.applicableLimits)) {
                  val item = newPurchaseService.insertPurchases(
                    path,
                    dto.weightOrVolume,
                    dto.noOfSticks,
                    dto.country,
                    dto.originCountry,
                    dto.currency,
                    List(dto.cost)
                  )
                  cache.store(item._1) map { _ =>
                    (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                      case (Some(true), Some("greatBritain")) =>
                        Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path, item._2))
                      case (Some(false), Some("euOnly"))      =>
                        if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                          Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, item._2))
                        } else {
                          Redirect(routes.SelectProductController.nextStep)
                        }
                      case _                                  => Redirect(routes.SelectProductController.nextStep)
                    }
                  }
                } else {
                  Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(path)))
                }
            )
        def processWeightAddForm           =
          tobaccoForm
            .weightOrVolumeForm(path)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    weight_or_volume_input(
                      formWithErrors,
                      product,
                      path,
                      None,
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
                ),
              dto =>
                if (calculatorLimitConstraintOptionBigDecimal(limits, product.applicableLimits)) {
                  val item = newPurchaseService.insertPurchases(
                    path,
                    dto.weightOrVolume,
                    dto.noOfSticks,
                    dto.country,
                    dto.originCountry,
                    dto.currency,
                    List(dto.cost)
                  )
                  cache.store(item._1) map { _ =>
                    (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                      case (Some(true), Some("greatBritain")) =>
                        Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path, item._2))
                      case (Some(false), Some("euOnly"))      =>
                        if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                          Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, item._2))
                        } else {
                          Redirect(routes.SelectProductController.nextStep)
                        }
                      case _                                  => Redirect(routes.SelectProductController.nextStep)
                    }
                  }
                } else {
                  Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(path)))
                }
            )
        def processNoOfSticksWeightAddForm =
          tobaccoForm
            .weightOrVolumeNoOfSticksForm(path)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    no_of_sticks_weight_or_volume_input(
                      formWithErrors,
                      product,
                      path,
                      None,
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
                ),
              dto =>
                if (calculatorLimitConstraintOptionInt(limits, product.applicableLimits)) {
                  val item = newPurchaseService.insertPurchases(
                    path,
                    dto.weightOrVolume,
                    dto.noOfSticks,
                    dto.country,
                    dto.originCountry,
                    dto.currency,
                    List(dto.cost)
                  )
                  cache.store(item._1) map { _ =>
                    (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                      case (Some(true), Some("greatBritain")) =>
                        Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path, item._2))
                      case (Some(false), Some("euOnly"))      =>
                        if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                          Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, item._2))
                        } else {
                          Redirect(routes.SelectProductController.nextStep)
                        }
                      case _                                  => Redirect(routes.SelectProductController.nextStep)
                    }
                  }
                } else {
                  Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(path)))
                }
            )

        product.templateId match {
          case "cigarettes" =>
            processNoOfSticksAddForm
          case "tobacco"    =>
            processWeightAddForm
          case _            =>
            processNoOfSticksWeightAddForm
        }
      }
    }
  }

  def processEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        requireLimitUsage {
          val dto = tobaccoForm.resilientForm.bindFromRequest().value.get
          newPurchaseService.updatePurchase(
            ppi.path,
            iid,
            dto.weightOrVolume,
            dto.noOfSticks,
            dto.country,
            dto.originCountry,
            dto.currency,
            dto.cost
          )
        } { limits =>
          def processCigarettesEditForm: Future[Result] =
            tobaccoForm
              .noOfSticksForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      no_of_sticks_input(
                        formWithErrors,
                        product,
                        ppi.path,
                        Some(iid),
                        countriesService.getAllCountries,
                        countriesService.getAllCountriesAndEu,
                        currencyService.getAllCurrencies,
                        context.getJourneyData.euCountryCheck
                      )
                    )
                  ),
                dto =>
                  if (calculatorLimitConstraintOptionInt(limits, product.applicableLimits)) {
                    cache.store(
                      newPurchaseService.updatePurchase(
                        ppi.path,
                        iid,
                        dto.weightOrVolume,
                        dto.noOfSticks,
                        dto.country,
                        dto.originCountry,
                        dto.currency,
                        dto.cost
                      )
                    ) map { _ =>
                      (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                        case (Some(true), Some("greatBritain")) =>
                          Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                        case (Some(false), Some("euOnly"))      =>
                          if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                            Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                          } else {
                            Redirect(routes.SelectProductController.nextStep)
                          }
                        case _                                  => Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                  } else {
                    Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(ppi.path)))
                  }
              )

          def processTobaccoEditForm: Future[Result] =
            tobaccoForm
              .weightOrVolumeForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      weight_or_volume_input(
                        formWithErrors,
                        product,
                        ppi.path,
                        Some(iid),
                        countriesService.getAllCountries,
                        countriesService.getAllCountriesAndEu,
                        currencyService.getAllCurrencies,
                        context.getJourneyData.euCountryCheck
                      )
                    )
                  ),
                dto =>
                  if (calculatorLimitConstraintOptionBigDecimal(limits, product.applicableLimits)) {
                    cache.store(
                      newPurchaseService.updatePurchase(
                        ppi.path,
                        iid,
                        dto.weightOrVolume,
                        dto.noOfSticks,
                        dto.country,
                        dto.originCountry,
                        dto.currency,
                        dto.cost
                      )
                    ) map { _ =>
                      (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                        case (Some(true), Some("greatBritain")) =>
                          Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                        case (Some(false), Some("euOnly"))      =>
                          if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                            Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                          } else {
                            Redirect(routes.SelectProductController.nextStep)
                          }
                        case _                                  => Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                  } else {
                    Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(ppi.path)))
                  }
              )

          def processOtherTobaccoEditForm: Future[Result] =
            tobaccoForm
              .weightOrVolumeNoOfSticksForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      no_of_sticks_weight_or_volume_input(
                        formWithErrors,
                        product,
                        ppi.path,
                        Some(iid),
                        countriesService.getAllCountries,
                        countriesService.getAllCountriesAndEu,
                        currencyService.getAllCurrencies,
                        context.getJourneyData.euCountryCheck
                      )
                    )
                  ),
                dto =>
                  if (calculatorLimitConstraintOptionInt(limits, product.applicableLimits)) {
                    cache.store(
                      newPurchaseService.updatePurchase(
                        ppi.path,
                        iid,
                        dto.weightOrVolume,
                        dto.noOfSticks,
                        dto.country,
                        dto.originCountry,
                        dto.currency,
                        dto.cost
                      )
                    ) map { _ =>
                      (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                        case (Some(true), Some("greatBritain")) =>
                          Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                        case (Some(false), Some("euOnly"))      =>
                          if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                            Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                          } else {
                            Redirect(routes.SelectProductController.nextStep)
                          }
                        case _                                  => Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                  } else {
                    Future.successful(Redirect(routes.LimitExceedController.loadLimitExceedPage(ppi.path)))
                  }
              )

          product.templateId match {
            case "cigarettes" =>
              processCigarettesEditForm
            case "tobacco"    =>
              processTobaccoEditForm
            case _            =>
              processOtherTobaccoEditForm
          }
        }
      }
    }
  }
}
