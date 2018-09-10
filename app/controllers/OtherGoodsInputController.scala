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
import scala.util.Random

class OtherGoodsInputController @Inject() (
  val travelDetailsService: TravelDetailsService,
  val productDetailsService: PurchasedProductService,
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
        Future.successful(Redirect(routes.OtherGoodsInputController.loadCurrencyInput(path, generateIid, quantityDto.quantity)))
      }
    )
  }


// FIXME: Add more enforcers to get expected NOT_FOUND in test
  def loadCurrencyInput(path: ProductPath, iid: String, ir: Int): Action[AnyContent] = DashboardAction { implicit context =>

    val form = CurrencyDto.form(currencyService).bind(Map("itemsRemaining" -> ir.toString)).discardingErrors
    requireProduct(path) { product =>
      Future.successful(Ok(views.html.other_goods.currency_input(form, product, path, currencyService.getAllCurrencies, iid)))
    }

  }

  // FIXME: Add more enforcers to get expected BAD_REQUEST in test
  def processCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.other_goods.currency_input(formWithErrors, product, path, currencyService.getAllCurrencies, iid)))
        }
      },
      currencyDto => {

        productDetailsService.storeCurrency(context.getJourneyData, path, iid, currencyDto.currency) map { _ =>
          Redirect(routes.OtherGoodsInputController.loadCostInput(path, iid, currencyDto.itemsRemaining))
        }
      }
    )
  }


  def loadCostInput(path: ProductPath, iid: String, ir: Int): Action[AnyContent] = DashboardAction { implicit context =>

    val form = CostDto.form(optionalItemsRemaining = false).bind(Map("itemsRemaining" -> ir.toString)).discardingErrors
    requirePurchasedProductInstanceCurrency(path, iid) { currency: Currency =>
      requireProduct(path) { product =>
        Future.successful(Ok(views.html.other_goods.cost_input(form, product, path, iid, currency.displayName)))
      }
    }
  }

  def processCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CostDto.form(optionalItemsRemaining = false).bindFromRequest.fold(
      formWithErrors => {

        requirePurchasedProductInstanceCurrency(path, iid) { currency: Currency =>
          requireProduct(path) { product =>
            Future.successful(BadRequest(views.html.other_goods.cost_input(formWithErrors, product, path, iid, currency.displayName)))
          }
        }
      },
      costDto => {

        val itemsRemaining = costDto.itemsRemaining
        val newItemsRemaining = itemsRemaining-1

        withPurchasedProductInstanceCount(path, iid) { count =>
          productDetailsService.storeCost(context.getJourneyData, path, iid, costDto.cost) map { _ =>
            if(newItemsRemaining > 0)
              Redirect(routes.OtherGoodsInputController.loadCurrencyInput(path, generateIid, newItemsRemaining))
            else
              Redirect(routes.SelectProductController.nextStep())
          }
        }
      }
    )
  }


}
