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

package controllers

import config.AppConfig
import connectors.Cache
import controllers.ControllerHelpers
import controllers.enforce.DashboardAction
import forms.AlcoholInputForm
import models.{AlcoholDto, JourneyData, ProductPath}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

class AlcoholInputController @Inject() (
  val cache: Cache,
  alcoholInputForm: AlcoholInputForm,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,
  alcoholAndTobaccoCalculationService: AlcoholAndTobaccoCalculationService,
  val backLinkModel: BackLinkModel,
  dashboardAction: DashboardAction,
  val errorTemplate: views.html.errorTemplate,
  val alcohol_input: views.html.alcohol.alcohol_input,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  // scalastyle:off
  def displayAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(
            Ok(
              alcohol_input(
                alcoholInputForm
                  .alcoholForm(path)
                  .bind(
                    Map(
                      "country"       -> defaultCountry.getOrElse(""),
                      "originCountry" -> defaultOriginCountry.getOrElse(""),
                      "currency"      -> defaultCurrency.getOrElse("")
                    )
                  )
                  .discardingErrors,
                backLinkModel.backLink,
                customBackLink = false,
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
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      requirePurchasedProductInstance(iid) { ppi =>
        requireProduct(ppi.path) { product =>
          AlcoholDto.fromPurchasedProductInstance(ppi) match {
            case Some(dto) =>
              Future.successful(
                Ok(
                  alcohol_input(
                    alcoholInputForm.alcoholForm(ppi.path).fill(dto),
                    backLinkModel.backLink,
                    customBackLink = true,
                    product,
                    ppi.path,
                    Some(iid),
                    countriesService.getAllCountries,
                    countriesService.getAllCountriesAndEu,
                    currencyService.getAllCurrencies,
                    context.getJourneyData.euCountryCheck
                  )
                )
              )
            case None      => logAndRenderError("Unable to construct dto from PurchasedProductInstance")
          }
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireLimitUsage {
      val dto =
        alcoholInputForm.resilientForm.bindFromRequest().value.get
      newPurchaseService
        .insertPurchases(
          path,
          Some(dto.weightOrVolume),
          None,
          dto.country,
          dto.originCountry,
          dto.currency,
          List(dto.cost)
        )
        ._1
    } { limits =>
      requireProduct(path) { product =>
        alcoholInputForm
          .alcoholForm(path)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  alcohol_input(
                    formWithErrors,
                    backLinkModel.backLink,
                    customBackLink = false,
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
            dto => {
              lazy val totalWeightAndVolume =
                alcoholAndTobaccoCalculationService.alcoholAddHelper(context.getJourneyData, dto.weightOrVolume, product.token)

              if (calculatorLimitConstraintBigDecimal(limits, product.applicableLimits, path).isEmpty) {
                val (journeyData, item) =
                  newPurchaseService.insertPurchases(
                    path,
                    Some(dto.weightOrVolume),
                    None,
                    dto.country,
                    dto.originCountry,
                    dto.currency,
                    List(dto.cost)
                  )
                cache.store(journeyData) map { _ =>
                  (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                    case (Some(true), Some("greatBritain")) =>
                      Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path, item))
                    case (Some(false), Some("euOnly"))      =>
                      if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                        Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, item))
                      } else {
                        Redirect(routes.SelectProductController.nextStep)
                      }
                    case _                                  => Redirect(routes.SelectProductController.nextStep)
                  }
                }
              } else {
                Future(
                  Redirect(
                    routes.LimitExceedController.onPageLoadAddJourneyAlcoholVolume(
                      path = calculatorLimitConstraintBigDecimal(limits, product.applicableLimits, path).get
                    )
                  ).removingFromSession(s"user-amount-input-${product.token}")
                    .addingToSession(s"user-amount-input-${product.token}" -> dto.weightOrVolume.toString())
                )
              }
            }
          )
      }
    }
  }

  def processEditForm(iid: String): Action[AnyContent] =
    dashboardAction { implicit context =>
      requirePurchasedProductInstance(iid) { ppi =>
        requireProduct(ppi.path) { product =>
          requireLimitUsage {
            val dto = alcoholInputForm.resilientForm.bindFromRequest().value.get
            newPurchaseService.updatePurchase(
              ppi.path,
              iid,
              Some(dto.weightOrVolume),
              None,
              dto.country,
              dto.originCountry,
              dto.currency,
              dto.cost
            )
          } { limits =>
            alcoholInputForm
              .alcoholForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      alcohol_input(
                        formWithErrors,
                        backLinkModel.backLink,
                        customBackLink = true,
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
                success = dto => {
                  lazy val alcoholTotalVolume =
                    alcoholAndTobaccoCalculationService.alcoholEditHelper(context.getJourneyData, dto.weightOrVolume, product.token)
                  if (calculatorLimitConstraintBigDecimal(limits, product.applicableLimits, ppi.path).isEmpty) {
                    cache.store(
                      newPurchaseService.updatePurchase(
                        ppi.path,
                        iid,
                        Some(dto.weightOrVolume),
                        None,
                        dto.country,
                        dto.originCountry,
                        dto.currency,
                        dto.cost
                      )
                    ) map { _: JourneyData =>
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
                    Future(
                      Redirect(
                        routes.LimitExceedController.onPageLoadEditAlcoholVolume(path = ppi.path)
                      )
                        .removingFromSession(s"user-amount-input-${product.token}")
                        .addingToSession(
                          s"user-amount-input-${product.token}" -> dto.weightOrVolume.toString()
                        )
                    )
                  }
                }
              )
          }
        }
      }
    }
}
