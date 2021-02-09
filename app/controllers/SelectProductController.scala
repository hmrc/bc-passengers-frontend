/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.{DashboardAction, PublicAction}
import javax.inject.Inject
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class SelectProductController @Inject()(
  val productTreeService: ProductTreeService,
  val cache: Cache,
  val calculatorService: CalculatorService,
  val currencyService: CurrencyService,
  val countriesService: CountriesService,
  val selectProductService: SelectProductService,

  publicAction: PublicAction,
  dashboardAction: DashboardAction,

  val purchasedProductService: PurchasedProductService,
  val select_products: views.html.purchased_products.select_products,
  val error_template: views.html.error_template,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def cancel():  Action[AnyContent] = dashboardAction { implicit context =>
    revertWorkingInstance {
      Future.successful(Redirect(routes.SelectProductController.nextStep()))
    }
  }

  def nextStep(): Action[AnyContent] = dashboardAction { implicit context =>

    withNextSelectedProductAlias {

      case None =>
        Future.successful(Redirect(routes.DashboardController.showDashboard()))

      case Some(ProductAlias(_, productPath)) =>

        selectProductService.removeSelectedAlias(context.getJourneyData) flatMap { _ =>  //FIXME - if an invalid path is supplied, this would still remove the top item from the stack
          requireProductOrCategory(productPath) {

            case ProductTreeBranch(_, _, children) =>
              Future.successful(Redirect(routes.SelectProductController.askProductSelection(productPath)))

            case ProductTreeLeaf(_, _, _, templateId, _ ) =>

              templateId match {
                case "alcohol" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/"+productPath+"/tell-us"))
                case "cigarettes" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/"+productPath+"/tell-us"))
                case "cigars" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/"+productPath+"/tell-us"))
                case "tobacco" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/"+productPath+"/tell-us"))
                case "other-goods" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/"+productPath+"/tell-us"))
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
        Future.successful(Ok(select_products(SelectProductsDto.form, children.map( i => ( i.name, i.token ) ), path)))
      case _ =>
        Future.successful(InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem.")))

    }
  }


  def processProductSelection(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>

    requireCategory(path) { branch =>

      SelectProductsDto.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful {
            BadRequest(select_products(formWithErrors, branch.children.map(i => (i.name, i.token)), path))
          }
        },
        selectProductsDto => {

          val updatedJourneyData = context.getJourneyData

          selectProductService.addSelectedProductsAsAliases(updatedJourneyData, selectProductsDto.tokens.map(path.addingComponent)) flatMap { journeyData =>
            
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

      SelectProductsDto.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful {
            BadRequest(select_products(formWithErrors, branch.children.map(i => (i.name, i.token)), path))
          }
        },
        selectProductsDto => {

          val updatedJourneyData = context.getJourneyData
          val paths: List[ProductPath] = selectProductsDto.tokens.map(path.addingComponent)

          selectProductService.addSelectedProductsAsAliases(updatedJourneyData, paths) flatMap { journeyData =>
            val pathsOrdered = journeyData.selectedAliases.map(_.productPath)

            pathsOrdered match {
              case x :: xs if productTreeService.productTree.getDescendant(x).fold(false)(_.isBranch) =>
                Future.successful(Redirect(routes.SelectProductController.nextStep()))

              case _ =>
                purchasedProductService.clearWorkingInstance(journeyData) map { _ =>
                  Redirect(routes.OtherGoodsSearchController.searchGoods())
                }
            }
          }
        }
      )
    }
  }
}
