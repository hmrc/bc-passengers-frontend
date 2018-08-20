package controllers

import config.AppConfig
import javax.inject.Inject
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{CurrencyService, ProductTreeService, PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class TobaccoInputController @Inject()(
  val travelDetailsService: TravelDetailsService,
  val productDetailsService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with PublicActions with ControllerHelpers {

  def startInputJourney(path: ProductPath): Action[AnyContent] = PublicAction { implicit request =>

    requireJourneyData { journeyData =>
      val nextIndex = journeyData.getOrCreatePurchasedProduct(path).purchasedProductInstances.size
      productDetailsService.storeQuantity(journeyData, path, 1) map { _ =>

        path.components.last match {
          case "cigarettes"            =>  Redirect(routes.TobaccoInputController.displayNoOfSticksInput(path, nextIndex))
          case "cigars" | "cigarillos" =>  Redirect(routes.TobaccoInputController.displayNoOfSticksWeightInput(path, nextIndex))
          case "rolling" | "chewing"   =>  Redirect(routes.TobaccoInputController.displayWeightInput(path, nextIndex))
        }
      }
    }
  }


  def displayNoOfSticksInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>
    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.no_of_sticks_input(NoOfSticksDto.form, product.token, product.name, path, index)))
    }
  }

  def processNoOfSticksInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    NoOfSticksDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.no_of_sticks_input(formWithErrors, product.token, product.name, path, index)))
        }
      },
      noOfSticksDto => {
        requireJourneyData { journeyData =>
          productDetailsService.storeNoOfSticks(journeyData, path, index, noOfSticksDto.noOfSticks) map { _ =>
            Redirect(routes.TobaccoInputController.displayCurrencyInput(path, index))
          }
        }
      }
    )
  }




  def displayNoOfSticksWeightInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>
    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.no_of_sticks_weight_input(NoOfSticksAndWeightDto.form, product.token, product.name, path, index)))
    }

  }

  def processNoOfSticksWeightInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    NoOfSticksAndWeightDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.no_of_sticks_weight_input(formWithErrors, product.token, product.name, path, index)))
        }
      },
      dto => {
        requireJourneyData { journeyData =>
          productDetailsService.storeNoOfSticksAndWeightOrVolume(journeyData, path, index, dto.noOfSticks, dto.weight) map { _ =>
            Redirect(routes.TobaccoInputController.displayCurrencyInput(path, index))
          }
        }
      }
    )
  }





  def displayWeightInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>
    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.weight_input(WeightDto.form, product.token, product.name, path, index)))
    }
  }

  def processWeightInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>
    WeightDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.weight_input(formWithErrors, product.token, product.name, path, index)))
        }
      },
      dto => {
        requireJourneyData { journeyData =>
          productDetailsService.storeWeightOrVolume(journeyData, path, index, dto.weight) map { _ =>
            Redirect(routes.TobaccoInputController.displayCurrencyInput(path, index))
          }
        }
      }
    )
  }





  def displayCurrencyInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    requireProduct(path) { product =>
      requireJourneyData { journeyData =>
        requirePurchasedProductInstanceDescription(journeyData)(product, path, index) { description =>
          Future.successful(Ok(views.html.tobacco.currency_input(CurrencyDto.form(currencyService), product, currencyService.getAllCurrencies, description, path, index)))
        }
      }
    }

  }

  def processCurrencyInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          requireJourneyData { journeyData =>
            requirePurchasedProductInstanceDescription(journeyData)(product, path, index) { description =>
              Future.successful(BadRequest(views.html.tobacco.currency_input(formWithErrors, product, currencyService.getAllCurrencies, description, path, index)))
            }
          }
        }
      },
      currencyDto => {
        requireJourneyData { journeyData =>

          productDetailsService.storeCurrency(journeyData, path, index, currencyDto.currency) map { _ =>
            Redirect(routes.TobaccoInputController.displayCostInput(path, index))
          }
        }
      }
    )
  }





  def displayCostInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    requireProduct(path) { product =>
      requireJourneyData { journeyData =>
        requirePurchasedProductInstanceDescription(journeyData)(product, path, index) { description =>
          requirePurchasedProductInstanceCurrency(journeyData)(path, index) { currency =>
            requireProduct(path) { product =>
              Future.successful(Ok(views.html.tobacco.cost_input(CostDto.form, product, path, index, description, currency.displayName)))
            }
          }
        }

      }
    }
  }

  def processCostInput(path: ProductPath, index: Int): Action[AnyContent] = PublicAction { implicit request =>

    CostDto.form.bindFromRequest.fold(
      formWithErrors => {

        requireProduct(path) { product =>
          requireJourneyData { journeyData =>
            requirePurchasedProductInstanceDescription(journeyData)(product, path, index) { description =>
              requirePurchasedProductInstanceCurrency(journeyData)(path, index) { currency =>
                requireProduct(path) { product =>
                  Future.successful(BadRequest(views.html.tobacco.cost_input(formWithErrors, product, path, index, description, currency.displayName)))
                }
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
