/*
 * Copyright 2026 HM Revenue & Customs
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
import controllers.enforce.DashboardAction
import controllers.ControllerHelpers
import forms.VapingProductsInputForm
import models.{ProductPath, VapeDto}
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VapingProductsInputController @Inject() (
  val cache: Cache,
  vapingProductsInputForm: VapingProductsInputForm,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,
  val backLinkModel: BackLinkModel,
  dashboardAction: DashboardAction,
  val vaping_products_input: views.html.vaping_products.vaping_products_input,
  val errorTemplate: views.html.errorTemplate,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  private def submittedIid(implicit context: LocalContext): Option[String] =
    context.request.body.asFormUrlEncoded
      .flatMap(_.get("iid").flatMap(_.headOption))

  def displayAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
        val term: List[String] = context.getJourneyData.selectedAliases.map(_.term)
        val baseForm           = vapingProductsInputForm.vapingProductsForm(path)
        val formForView        =
          defaultOriginCountry.filter(_.trim.nonEmpty) match {
            case Some(oc) =>
              baseForm
                .bind(Map("originCountry" -> oc))
                .discardingErrors
            case None     =>
              baseForm
          }
        term.size match {
          case 1 =>
            cache
              .storeJourneyData(context.getJourneyData.copy(selectedAliases = Nil))
              .map(_ =>
                Ok(
                  vaping_products_input(
                    formForView,
                    backLinkModel.backLink,
                    customBackLink = false,
                    path,
                    None,
                    countriesService.getAllCountries,
                    countriesService.getAllCountriesAndEu,
                    currencyService.getAllCurrencies,
                    context.getJourneyData.euCountryCheck
                  )
                )
              )
          case _ =>
            cache
              .storeJourneyData(context.getJourneyData.copy(selectedAliases = Nil))
              .map(_ =>
                Ok(
                  vaping_products_input(
                    formForView,
                    backLinkModel.backLink,
                    customBackLink = false,
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
          VapeDto.fromPurchasedProductInstance(ppi) match {
            case Some(dto) =>
              Future.successful(
                Ok(
                  vaping_products_input(
                    vapingProductsInputForm.vapingProductsForm(ppi.path).fill(dto),
                    backLinkForAddedItemEdit(
                      backLinkModel.backLink,
                      routes.VapingProductsInputController.displayEditForm(iid).url
                    ),
                    customBackLink = true,
                    ppi.path,
                    Some(iid),
                    countriesService.getAllCountries,
                    countriesService.getAllCountriesAndEu,
                    currencyService.getAllCurrencies,
                    context.getJourneyData.euCountryCheck
                  )
                )
              )
            case None      =>
              logAndRenderError(
                "[VapingProductsInputController][displayEditForm] Unable to construct dto from PurchasedProductInstance"
              )
          }
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    val processContinue = vapingProductsInputForm
      .vapingProductsForm(path)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              vaping_products_input(
                formWithErrors,
                backLinkModel.backLink,
                customBackLink = false,
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
          requireProduct(path) { _ =>
            val jd = submittedIid.fold(
              newPurchaseService.insertPurchases(
                path,
                Some(dto.weightOrVolume),
                None,
                dto.country,
                dto.originCountry,
                dto.currency,
                List(dto.cost)
              )
            )(iid =>
              newPurchaseService.insertPurchasesWithIid(
                path,
                Some(dto.weightOrVolume),
                None,
                dto.country,
                dto.originCountry,
                dto.currency,
                List(dto.cost),
                iid
              )
            )
            cache.store(jd._1) map { _ =>
              markReturnToAddedItem(
                (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                  case (Some(true), Some("greatBritain")) =>
                    Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path, jd._2))
                  case (Some(false), Some("euOnly"))      =>
                    if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                      Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, jd._2))
                    } else {
                      Redirect(routes.GoodsCheckYourAnswersController.show(path, jd._2))
                    }
                  case _                                  => Redirect(routes.GoodsCheckYourAnswersController.show(path, jd._2))
                },
                routes.VapingProductsInputController.displayEditForm(jd._2).url,
                path,
                Some(routes.GoodsCheckYourAnswersController.show(path, jd._2).url)
              )
            }
          }
      )
    processContinue

  }

  def processEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { _ =>
        def processContinue = vapingProductsInputForm
          .vapingProductsForm(ppi.path)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  vaping_products_input(
                    formWithErrors,
                    backLinkModel.backLink,
                    customBackLink = true,
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
              val jd = newPurchaseService.updatePurchase(
                ppi.path,
                iid,
                Some(dto.weightOrVolume),
                None,
                dto.country,
                dto.originCountry,
                dto.currency,
                dto.cost
              )
              cache.store(jd) map { _ =>
                val result = (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                  case (Some(true), Some("greatBritain")) =>
                    Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                  case (Some(false), Some("euOnly"))      =>
                    if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                      Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                    } else {
                      Redirect(routes.GoodsCheckYourAnswersController.show(ppi.path, iid))
                    }
                  case _                                  => Redirect(routes.GoodsCheckYourAnswersController.show(ppi.path, iid))
                }
                clearReturnToAddedItemUnlessCurrentEdit(
                  result,
                  routes.VapingProductsInputController.displayEditForm(iid).url
                )
              }
            }
          )
        processContinue
      }
    }
  }

}
