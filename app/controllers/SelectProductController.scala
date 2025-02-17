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

package controllers

import config.AppConfig
import connectors.Cache
import controllers.ControllerHelpers
import controllers.enforce.DashboardAction
import models.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import services.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectProductController @Inject() (
  val productTreeService: ProductTreeService,
  val cache: Cache,
  val calculatorService: CalculatorService,
  val currencyService: CurrencyService,
  val countriesService: CountriesService,
  val selectProductService: SelectProductService,
  backLinkModel: BackLinkModel,
  dashboardAction: DashboardAction,
  val purchasedProductService: PurchasedProductService,
  val select_products: views.html.purchased_products.select_products,
  val errorTemplate: views.html.errorTemplate,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  def cancel(): Action[AnyContent] = dashboardAction { implicit context =>
    revertWorkingInstance {
      Future.successful(Redirect(routes.SelectProductController.nextStep()))
    }
  }

  def nextStep(): Action[AnyContent] = dashboardAction { implicit context =>
    withNextSelectedProductAlias {

      case None =>
        Future.successful(Redirect(routes.DashboardController.showDashboard))

      case Some(ProductAlias(_, productPath)) =>
        selectProductService.removeSelectedAlias(context.getJourneyData).flatMap { _ =>
          requireProductOrCategory(productPath) {

            case ProductTreeBranch(_, _, _) =>
              Future.successful(Redirect(routes.SelectProductController.askProductSelection(productPath)))

            case ProductTreeLeaf(_, _, _, templateId, _) =>
              templateId match {
                case "alcohol"     =>
                  Future.successful(
                    Redirect("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/" + productPath + "/tell-us")
                  )
                case "cigarettes"  =>
                  Future.successful(
                    Redirect("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/" + productPath + "/tell-us")
                  )
                case "cigars"      =>
                  Future.successful(
                    Redirect("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/" + productPath + "/tell-us")
                  )
                case "tobacco"     =>
                  Future.successful(
                    Redirect("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/" + productPath + "/tell-us")
                  )
                case "other-goods" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/tell-us"))
              }

          }
        }
    }
  }

  def clearAndAskProductSelection(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    val journeyData = context.getJourneyData.copy(selectedAliases = Nil)

    cache.storeJourneyData(journeyData).map(_ => Redirect(routes.SelectProductController.askProductSelection(path)))
  }

  def askProductSelection(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireProductOrCategory(path) {

      case ProductTreeBranch(_, _, children) =>
        Future.successful(
          Ok(
            select_products(
              SelectProductsDto.form,
              children.map(i => (i.token, i.name)),
              path,
              backLinkModel.backLink
            )
          )
        )
      case _                                 =>
        Future.successful(InternalServerError(errorTemplate()))
    }
  }

  def processProductSelection(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireCategory(path) { branch =>
      SelectProductsDto.form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful {
              BadRequest(
                select_products(
                  formWithErrors,
                  branch.children.map(i => (i.token, i.name)),
                  path,
                  backLinkModel.backLink
                )
              )
            },
          selectProductsDto => {

            val updatedJourneyData = context.getJourneyData

            selectProductService
              .addSelectedProductsAsAliases(
                updatedJourneyData,
                selectProductsDto.tokens.map(path.addingComponent)
              )
              .flatMap { journeyData =>
                purchasedProductService.clearWorkingInstance(journeyData) map { _ =>
                  Redirect(routes.SelectProductController.nextStep())
                }
              }
          }
        )
    }
  }

  def processProductSelectionOtherGoods(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireCategory(path) { branch =>
      SelectProductsDto.form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful {
              BadRequest(
                select_products(
                  formWithErrors,
                  branch.children.map(i => (i.token, i.name)),
                  path,
                  backLinkModel.backLink
                )
              )
            },
          selectProductsDto => {

            val updatedJourneyData       = context.getJourneyData
            val paths: List[ProductPath] = selectProductsDto.tokens.map(path.addingComponent)

            selectProductService.addSelectedProductsAsAliases(updatedJourneyData, paths).flatMap { journeyData =>
              val pathsOrdered = journeyData.selectedAliases.map(_.productPath)

              pathsOrdered match {
                case x :: _ if productTreeService.productTree.getDescendant(x).fold(false)(_.isBranch) =>
                  Future.successful(Redirect(routes.SelectProductController.nextStep()))

                case _ =>
                  purchasedProductService.clearWorkingInstance(journeyData) map { _ =>
                    Redirect(routes.OtherGoodsInputController.displayAddForm())
                  }
              }
            }
          }
        )
    }
  }
}
