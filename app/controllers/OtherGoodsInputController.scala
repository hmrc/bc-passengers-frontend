package controllers

import config.AppConfig
import javax.inject.Inject
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CurrencyService, PurchasedProductService, ProductTreeService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class OtherGoodsInputController @Inject() (
  val travelDetailsService: TravelDetailsService,
  val productDetailsService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with PublicActions with ControllerHelpers {

  def displayQuantityInput(path: ProductPath): Action[AnyContent] = PublicAction { implicit request =>

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.other_goods.quantity_input(QuantityDto.form, product.name, product.token, path)))
    }
  }

  def processQuantityInput(path: ProductPath): Action[AnyContent] = PublicAction { implicit request =>

    QuantityDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.other_goods.quantity_input(formWithErrors, product.name, product.token, path)))
        }
      },
      quantityDto => {
        requireJourneyData { journeyData =>
          val nextIndex = journeyData.getOrCreatePurchasedProduct(path).purchasedProductInstances.size
          productDetailsService.storeQuantity(journeyData, path, quantityDto.quantity) map { _ =>
            Redirect(routes.OtherGoodsInputController.loadCurrencyInput(path, nextIndex))
          }
        }
      }
    )
  }


// FIXME: Add more enforcers to get expected NOT_FOUND in test
  def loadCurrencyInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.other_goods.currency_input(CurrencyDto.form(currencyService), product, path, currencyService.getAllCurrencies, index)))
    }

  }

  // FIXME: Add more enforcers to get expected BAD_REQUEST in test
  def processCurrencyInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.other_goods.currency_input(formWithErrors, product, path, currencyService.getAllCurrencies, index)))
        }
      },
      currencyDto => {
        requireJourneyData { journeyData =>

          productDetailsService.storeCurrency(journeyData, path, index, currencyDto.currency) map { _ =>
            Redirect(routes.OtherGoodsInputController.loadCostInput(path, index))
          }
        }
      }
    )
  }


  def loadCostInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    requireJourneyData { journeyData =>
      requirePurchasedProductInstanceCurrency(journeyData)(path, index) { currency: Currency =>
        requireProduct(path) { product =>
          Future.successful(Ok(views.html.other_goods.cost_input(CostDto.form, product, path, index, currency.displayName)))
        }
      }
    }
  }

  def processCostInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    CostDto.form.bindFromRequest.fold(
      formWithErrors => {

        requireJourneyData { journeyData =>
          requirePurchasedProductInstanceCurrency(journeyData)(path, index) { currency: Currency =>
            requireProduct(path) { product =>
              Future.successful(BadRequest(views.html.other_goods.cost_input(formWithErrors, product, path, index, currency.displayName)))
            }
          }
        }
      },
      costDto => {
        requireJourneyData { journeyData =>

          productDetailsService.storeCost(journeyData, path, index, costDto.cost) map { _ =>

            val count = journeyData.getOrCreatePurchasedProduct(path).quantity.getOrElse(0)
            val nextIndex = index+1

            if(nextIndex < count)
              Redirect(routes.OtherGoodsInputController.loadCurrencyInput(path, nextIndex))
            else
              Redirect(routes.SelectProductController.nextStep())
          }
        }
      }
    )
  }


}
