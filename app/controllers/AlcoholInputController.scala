package controllers

import config.AppConfig
import javax.inject.Inject
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CurrencyService, PurchasedProductService, ProductTreeService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class AlcoholInputController @Inject() (
  val travelDetailsService: TravelDetailsService,
  val productDetailsService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with PublicActions with ControllerHelpers {


  def startInputJourney(path: ProductPath): Action[AnyContent] = PublicAction { implicit request =>

    requireJourneyData { journeyData =>
      val nextIndex = journeyData.getOrCreatePurchasedProduct(path).purchasedProductInstances.fold(0)(_.size)
      productDetailsService.storeQuantity(journeyData, path, 1) map { _ =>
        Redirect(routes.AlcoholInputController.displayVolumeInput(path, nextIndex))
      }
    }
  }

  def displayVolumeInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.alcohol.volume_input(VolumeDto.form, product.name, product.token, path, index)))
    }
  }

  def processVolumeInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    VolumeDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.alcohol.volume_input(formWithErrors, product.name, product.token, path, index)))
        }
      },
      volumeDto => {
        requireJourneyData { journeyData =>
          productDetailsService.storeWeightOrVolume(journeyData, path, index, volumeDto.volume) map { _ =>
            Redirect(routes.AlcoholInputController.displayCurrencyInput(path, index))
          }
        }

      }
    )
  }

  def displayCurrencyInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    requireJourneyData { journeyData =>
      requirePurchasedProductInstanceWeightOrVolume(journeyData)(path, index) { volume =>
        requireProduct(path) { product =>
          Future.successful(Ok(views.html.alcohol.currency_input(CurrencyDto.form(currencyService), product, path, index, currencyService.getAllCurrencies, volume)))
        }
      }
    }
  }

  def processCurrencyInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {

        requireJourneyData { journeyData =>
          requirePurchasedProductInstanceWeightOrVolume(journeyData)(path, index) { volume =>
            requireProduct(path) { product =>
              Future.successful(BadRequest(views.html.alcohol.currency_input(formWithErrors, product, path, index, currencyService.getAllCurrencies, volume)))
            }
          }
        }

      },
      currencyDto => {
        requireJourneyData { journeyData =>

          productDetailsService.storeCurrency(journeyData, path, index, currencyDto.currency) map { _ =>
            Redirect(routes.AlcoholInputController.displayCostInput(path, index))
          }
        }
      }
    )
  }

  def displayCostInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    requireJourneyData { journeyData =>
      requirePurchasedProductInstanceWeightOrVolume(journeyData)(path, index) { volume =>
        requirePurchasedProductInstanceCurrency(journeyData)(path, index) { currency: Currency =>
          requireProduct(path) { product =>
            Future.successful(Ok(views.html.alcohol.cost_input(CostDto.form, product, path, index, volume, currency.displayName)))
          }
        }
      }
    }
  }

  def processCostInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    CostDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireJourneyData { journeyData =>
          requirePurchasedProductInstanceWeightOrVolume(journeyData)(path, index) { volume =>
            requirePurchasedProductInstanceCurrency(journeyData)(path, index) { currency: Currency =>
              requireProduct(path) { product =>
                Future.successful(BadRequest(views.html.alcohol.cost_input(formWithErrors, product, path, index, volume, currency.displayName)))
              }
            }
          }
        }
      },
      costDto => {
        requireJourneyData { journeyData =>

          productDetailsService.storeCost(journeyData, path, index, costDto.cost) map { _ =>
            Redirect(routes.SelectProductController.nextStep())
          }
        }
      }
    )
  }


}
