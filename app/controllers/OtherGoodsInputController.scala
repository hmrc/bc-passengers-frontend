package controllers

import config.AppConfig
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CurrencyService, ProductTreeService, PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class OtherGoodsInputController @Inject() (
  val travelDetailsService: TravelDetailsService,
  val purchasedProductService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  def displayQuantityInput(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.other_goods.quantity_input(QuantityDto.form, product.name, product.token, path)))
    }
  }

  def processQuantityInput(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    QuantityDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.other_goods.quantity_input(formWithErrors, product.name, product.token, path)))
        }
      },
      quantityDto => {
        Future.successful(Redirect(routes.OtherGoodsInputController.displayCurrencyInput(path, generateIid, quantityDto.quantity)))
      }
    )
  }

  def displayCurrencyInput(path: ProductPath, iid: String, ir: Int): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, _, _, _, Some(currency), _)) => CurrencyDto.form(currencyService).fill(CurrencyDto(currency, ir)).discardingErrors
        case _ => CurrencyDto.form(currencyService).bind(Map("itemsRemaining" -> ir.toString)).discardingErrors
      }
    }

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.other_goods.currency_input(form, product, path, currencyService.getAllCurrencies, iid)))
    }
  }

  def displayCurrencyUpdate(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) flatMap { updatedJourneyData =>
        val form = {
          updatedJourneyData.workingInstance match {
            case Some(PurchasedProductInstance(_, _, _, _, Some(currency), _)) =>
              CurrencyDto.form(currencyService).fill(CurrencyDto(currency, 0)).discardingErrors
            case _ =>
              CurrencyDto.form(currencyService)
          }
        }

        requireProduct(path) { product =>
          Future.successful(Ok(views.html.other_goods.currency_input(form, product, path, currencyService.getAllCurrencies, iid)))
        }
      }
    }
  }


  def processCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.other_goods.currency_input(formWithErrors, product, path, currencyService.getAllCurrencies, iid)))
        }
      },
      currencyDto => {

        purchasedProductService.storeCurrency(context.getJourneyData, path, iid, currencyDto.currency) map { _ =>
          Redirect(routes.OtherGoodsInputController.displayCostInput(path, iid, currencyDto.itemsRemaining))
        }
      }
    )
  }

  def replaceProductInPlace(purchasedProductInstances: List[PurchasedProductInstance], productToReplace: PurchasedProductInstance): List[PurchasedProductInstance] = {
    (purchasedProductInstances.takeWhile(_.iid != productToReplace.iid), purchasedProductInstances.dropWhile(_.iid != productToReplace.iid)) match {
      case (x, Nil) => productToReplace :: x  //Prepend
      case (x, y) => x ++ (productToReplace :: y.tail)  //Replace in place
    }
  }


  def displayCostInput(path: ProductPath, iid: String, ir: Int): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, _, _, _, _, Some(cost))) => CostDto.form(optionalItemsRemaining = false).bind(Map("cost" -> cost.toString, "itemsRemaining" -> ir.toString)).discardingErrors
        case _ => CostDto.form(optionalItemsRemaining = false).bind(Map("itemsRemaining" -> ir.toString)).discardingErrors
      }
    }

    requireWorkingInstanceCurrency { currency =>

      requireProduct(path) { product =>
        Future.successful(Ok(views.html.other_goods.cost_input(form, product, path, iid, currency.displayName)))
      }
    }
  }


  def processCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CostDto.form(optionalItemsRemaining = false).bindFromRequest.fold(
      formWithErrors => {

        requireWorkingInstanceCurrency { currency =>
          requireProduct(path) { product =>
            Future.successful(BadRequest(views.html.other_goods.cost_input(formWithErrors, product, path, iid, currency.displayName)))
          }
        }
      },
      costDto => {

        val itemsRemaining = costDto.itemsRemaining - 1

        requireWorkingInstance { workingInstance =>

          requireProduct(path) { product =>

            val wi = workingInstance.copy(cost = Some(costDto.cost))

            if (product.isValid(wi)) {
              context.journeyData.map { jd =>
                val updatedPurchasedProductInstances = replaceProductInPlace(jd.purchasedProductInstances, wi)
                purchasedProductService.cacheJourneyData(jd.copy(purchasedProductInstances = updatedPurchasedProductInstances, workingInstance = None))
              }
            } else {
              Logger.warn("Working instance was not valid")
            }

            if (itemsRemaining > 0) {
              Future.successful(Redirect(routes.OtherGoodsInputController.displayCurrencyInput(path, generateIid, itemsRemaining)))
            } else {
              Future.successful(Redirect(routes.SelectProductController.nextStep()))
            }
          }
        }
      }
    )
  }


}
