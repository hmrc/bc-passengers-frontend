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
import forms.TobaccoInputForm
import models.{JourneyData, ProductPath, TobaccoDto}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util._
import utils.FormatsAndConversions

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off
class TobaccoInputController @Inject() (
  val cache: Cache,
  tobaccoInputForm: TobaccoInputForm,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,
  alcoholAndTobaccoCalculationService: AlcoholAndTobaccoCalculationService,
  val backLinkModel: BackLinkModel,
  val dashboardAction: DashboardAction,
  val errorTemplate: views.html.errorTemplate,
  val tobacco_input: views.html.tobacco.tobacco_input,
  val weight_or_volume_input: views.html.tobacco.weight_or_volume_input,
  val no_of_sticks_input: views.html.tobacco.no_of_sticks_input,
  val no_of_sticks_weight_or_volume_input: views.html.tobacco.no_of_sticks_weight_or_volume_input,
  override val controllerComponents: MessagesControllerComponents
)(implicit val appConfig: AppConfig, implicit val ec: ExecutionContext)
    extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers
    with FormatsAndConversions {

  private def navigationHelper(
    jd: JourneyData,
    productPath: ProductPath,
    iid: String,
    originCountry: Option[String]
  ) =
    (jd.arrivingNICheck, jd.euCountryCheck) match {
      case (Some(true), Some("greatBritain"))                                                    =>
        Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(productPath, iid))
      case (Some(false), Some("euOnly")) if countriesService.isInEu(originCountry.getOrElse("")) =>
        Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(productPath, iid))
      case _                                                                                     =>
        Redirect(routes.SelectProductController.nextStep)
    }

  def displayCigaretteAndHeatedTobaccoForm(path: ProductPath): Action[AnyContent] = dashboardAction {
    implicit context =>
      if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
        Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
      } else {
        requireProduct(path) { product =>
          withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
            Future.successful(
              Ok(
                no_of_sticks_input(
                  tobaccoInputForm
                    .cigaretteAndHeatedTobaccoForm(path)
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
              ).removingFromSession(s"user-amount-input-${product.token}")
            )
          }
        }
      }
  }

  def displayLooseTobaccoForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(
            Ok(
              weight_or_volume_input(
                tobaccoInputForm
                  .looseTobaccoWeightForm(path)
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
            ).removingFromSession(s"user-amount-input-${product.token}")
          )
        }
      }
    }
  }

  def displayCigarAndCigarilloForm(path: ProductPath): Action[AnyContent] =
    dashboardAction { implicit context =>
      if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
        Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
      } else {
        requireProduct(path) { product =>
          withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
            Future.successful(
              Ok(
                no_of_sticks_weight_or_volume_input(
                  tobaccoInputForm
                    .cigarAndCigarilloForm(path)
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
              ).removingFromSession(s"user-amount-input-${product.token}")
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
                      tobaccoInputForm.cigaretteAndHeatedTobaccoForm(ppi.path).fill(dto),
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
                case "tobacco"    =>
                  Ok(
                    weight_or_volume_input(
                      tobaccoInputForm.looseTobaccoWeightForm(ppi.path).fill(dto),
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
                case _            =>
                  Ok(
                    no_of_sticks_weight_or_volume_input(
                      tobaccoInputForm.cigarAndCigarilloForm(ppi.path).fill(dto),
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
              }
            }
          }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    val dto              = tobaccoInputForm.resilientForm.bindFromRequest().value.get
    val (journeyData, _) =
      newPurchaseService.insertPurchases(
        path,
        dto.weightOrVolume,
        dto.noOfSticks,
        dto.country,
        dto.originCountry,
        dto.currency,
        List(dto.cost)
      )

    requireLimitUsage(journeyData) { limits =>
      requireProduct(path) { product =>
        def processNoOfSticksAddForm =
          tobaccoInputForm
            .cigaretteAndHeatedTobaccoForm(path)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    no_of_sticks_input(
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
                lazy val totalNoOfSticksForItemType =
                  alcoholAndTobaccoCalculationService
                    .noOfSticksTobaccoAddHelper(context.getJourneyData, dto.noOfSticks, product.token)
                if (cigaretteAndHeatedTobaccoConstraint(totalNoOfSticksForItemType)) {
                  val (journeyData: JourneyData, iid: String) =
                    newPurchaseService.insertPurchases(
                      path = path,
                      weightOrVolume = dto.weightOrVolume,
                      noOfSticks = dto.noOfSticks,
                      countryCode = dto.country,
                      originCountryCode = dto.originCountry,
                      currency = dto.currency,
                      costs = List(dto.cost)
                    )

                  cache.store(journeyData) map { _ =>
                    navigationHelper(context.getJourneyData, path, iid, dto.originCountry)
                  }
                } else {
                  Future(
                    Redirect(
                      routes.LimitExceedController.onPageLoadAddJourneyNoOfSticks(path)
                    )
                      .removingFromSession(s"user-amount-input-${product.token}")
                      .addingToSession(s"user-amount-input-${product.token}" -> dto.noOfSticks.getOrElse(0).toString)
                  )
                }
              }
            )

        def processWeightAddForm =
          tobaccoInputForm
            .looseTobaccoWeightForm(path)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    weight_or_volume_input(
                      form = formWithErrors,
                      backLink = backLinkModel.backLink,
                      customBackLink = false,
                      product = product,
                      path = path,
                      iid = None,
                      countries = countriesService.getAllCountries,
                      countriesEU = countriesService.getAllCountriesAndEu,
                      currencies = currencyService.getAllCurrencies,
                      journeyStart = context.getJourneyData.euCountryCheck
                    )
                  )
                ),
              success = dto => {
                lazy val totalWeightForLooseTobacco =
                  alcoholAndTobaccoCalculationService.looseTobaccoAddHelper(context.getJourneyData, dto.weightOrVolume)
                if (looseTobaccoWeightConstraint(totalWeightForLooseTobacco * 1000)) {
                  val (journeyData: JourneyData, iid: String) =
                    newPurchaseService.insertPurchases(
                      path,
                      dto.weightOrVolume,
                      dto.noOfSticks,
                      dto.country,
                      dto.originCountry,
                      dto.currency,
                      List(dto.cost)
                    )
                  cache.store(journeyData).map { _ =>
                    navigationHelper(context.getJourneyData, path, iid, dto.originCountry)
                  }
                } else {
                  Future(
                    Redirect(routes.LimitExceedController.onPageLoadAddJourneyTobaccoWeight(path))
                      .removingFromSession(s"user-amount-input-${product.token}")
                      .addingToSession(
                        s"user-amount-input-${product.token}" -> dto.weightOrVolume.getOrElseZero.toString
                      )
                  )
                }
              }
            )

        def processNoOfSticksWeightAddForm =
          tobaccoInputForm
            .cigarAndCigarilloForm(path)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    no_of_sticks_weight_or_volume_input(
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
                lazy val totalNoOfSticksForItemType =
                  alcoholAndTobaccoCalculationService
                    .noOfSticksTobaccoAddHelper(context.getJourneyData, dto.noOfSticks, product.token)
                if (cigarAndCigarilloConstraint(totalNoOfSticksForItemType, product.token)) {
                  val (journeyData: JourneyData, iid: String) =
                    newPurchaseService.insertPurchases(
                      path,
                      dto.weightOrVolume,
                      dto.noOfSticks,
                      dto.country,
                      dto.originCountry,
                      dto.currency,
                      List(dto.cost)
                    )
                  cache.store(journeyData) map { _ =>
                    navigationHelper(context.getJourneyData, path, iid, dto.originCountry)
                  }
                } else {
                  Future(
                    Redirect(
                      routes.LimitExceedController.onPageLoadAddJourneyNoOfSticks(path)
                    )
                      .removingFromSession(s"user-amount-input-${product.token}")
                      .addingToSession(
                        s"user-amount-input-${product.token}" -> dto.noOfSticks.getOrElse(0).toString
                      )
                  )
                }
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
          val dto = tobaccoInputForm.resilientForm.bindFromRequest().value.get
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
            tobaccoInputForm
              .cigaretteAndHeatedTobaccoForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      no_of_sticks_input(
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
                (dto: TobaccoDto) =>
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
                      navigationHelper(context.getJourneyData, ppi.path, iid, dto.originCountry)
                    }
                  } else {
                    Future(
                      Redirect(
                        routes.LimitExceedController.onPageLoadEditNoOfSticks(ppi.path, iid)
                      )
                        .removingFromSession(s"user-amount-input-${product.token}")
                        .addingToSession(s"user-amount-input-${product.token}" -> dto.noOfSticks.getOrElse(0).toString)
                    )
                  }
              )

          def processTobaccoEditForm: Future[Result] =
            tobaccoInputForm
              .looseTobaccoWeightForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      weight_or_volume_input(
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
                  val updatedUserAnswers =
                    alcoholAndTobaccoCalculationService
                      .looseTobaccoEditHelper(context.getJourneyData, dto.weightOrVolume, iid)
                  if (looseTobaccoWeightConstraint(updatedUserAnswers * 1000)) {
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
                      navigationHelper(context.getJourneyData, ppi.path, iid, dto.originCountry)
                    }
                  } else {
                    Future(
                      Redirect(
                        routes.LimitExceedController.onPageLoadEditTobaccoWeight(ppi.path, iid)
                      ).removingFromSession(s"user-amount-input-${product.token}")
                        .addingToSession(
                          s"user-amount-input-${product.token}" ->
                            dto.weightOrVolume.getOrElseZero.toString
                        )
                    )
                  }
                }
              )

          def processOtherTobaccoEditForm: Future[Result] =
            tobaccoInputForm
              .cigarAndCigarilloForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      no_of_sticks_weight_or_volume_input(
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
                dto => {
                  lazy val totalNoOfSticksForItemType =
                    alcoholAndTobaccoCalculationService
                      .noOfSticksTobaccoEditHelper(context.getJourneyData, dto.noOfSticks, product.token, iid)
                  if (cigarAndCigarilloConstraint(totalNoOfSticksForItemType, product.token)) {
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
                      navigationHelper(context.getJourneyData, ppi.path, iid, dto.originCountry)
                    }
                  } else {
                    Future(
                      Redirect(
                        routes.LimitExceedController.onPageLoadEditNoOfSticks(ppi.path, iid)
                      )
                        .removingFromSession(s"user-amount-input-${product.token}")
                        .addingToSession(s"user-amount-input-${product.token}" -> dto.noOfSticks.getOrElse(0).toString)
                    )
                  }
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
