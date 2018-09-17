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

class TobaccoInputController @Inject()(
  val travelDetailsService: TravelDetailsService,
  val productDetailsService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  def startInputJourney(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    val iid = generateIid

    Future.successful {
      path.components.last match {
        case "cigarettes"            =>  Redirect(routes.TobaccoInputController.displayNoOfSticksInput(path, iid))
        case "cigars" | "cigarillos" =>  Redirect(routes.TobaccoInputController.displayNoOfSticksWeightInput(path, iid))
        case "rolling" | "chewing"   =>  Redirect(routes.TobaccoInputController.displayWeightInput(path, iid))
      }
    }
  }


  def displayNoOfSticksInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.no_of_sticks_input(NoOfSticksDto.form, product.token, product.name, path, iid)))
    }
  }

  def processNoOfSticksInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    NoOfSticksDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.no_of_sticks_input(formWithErrors, product.token, product.name, path, iid)))
        }
      },
      noOfSticksDto => {
        productDetailsService.storeNoOfSticks(context.getJourneyData, path, iid, noOfSticksDto.noOfSticks) map { _ =>
          Redirect(routes.TobaccoInputController.displayCurrencyInput(path, iid))
        }
      }
    )
  }




  def displayNoOfSticksWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.no_of_sticks_weight_input(NoOfSticksAndWeightDto.form, product.token, product.name, path, iid)))
    }

  }

  def processNoOfSticksWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    NoOfSticksAndWeightDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.no_of_sticks_weight_input(formWithErrors, product.token, product.name, path, iid)))
        }
      },
      dto => {
        productDetailsService.storeNoOfSticksAndWeightOrVolume(context.getJourneyData, path, iid, dto.noOfSticks, dto.weight) map { _ =>
          Redirect(routes.TobaccoInputController.displayCurrencyInput(path, iid))
        }
      }
    )
  }





  def displayWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.weight_input(WeightDto.form, product.token, product.name, path, iid)))
    }
  }

  def processWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    WeightDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.weight_input(formWithErrors, product.token, product.name, path, iid)))
        }
      },
      dto => {
        productDetailsService.storeWeightOrVolume(context.getJourneyData, path, iid, dto.weight) map { _ =>
          Redirect(routes.TobaccoInputController.displayCurrencyInput(path, iid))
        }
      }
    )
  }





  def displayCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      requireWorkingInstanceDescription(product) { description =>
        Future.successful(Ok(views.html.tobacco.currency_input(CurrencyDto.form(currencyService), product, currencyService.getAllCurrencies, description, path, iid)))
      }
    }

  }

  def processCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          requireWorkingInstanceDescription(product) { description =>
            Future.successful(BadRequest(views.html.tobacco.currency_input(formWithErrors, product, currencyService.getAllCurrencies, description, path, iid)))
          }
        }
      },
      currencyDto => {
        productDetailsService.storeCurrency(context.getJourneyData, path, iid, currencyDto.currency) map { _ =>
          Redirect(routes.TobaccoInputController.displayCostInput(path, iid))
        }
      }
    )
  }





  def displayCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      requireWorkingInstanceDescription(product) { description =>
        requireWorkingInstanceCurrency { currency =>
          requireProduct(path) { product =>
            Future.successful(Ok(views.html.tobacco.cost_input(CostDto.form(), product, path, iid, description, currency.displayName)))
          }
        }
      }
    }
  }

  def processCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CostDto.form().bindFromRequest.fold(
      formWithErrors => {

        requireProduct(path) { product =>
          requireWorkingInstanceDescription(product) { description =>
            requireWorkingInstanceCurrency { currency =>
              requireProduct(path) { product =>
                Future.successful(BadRequest(views.html.tobacco.cost_input(formWithErrors, product, path, iid, description, currency.displayName)))
              }
            }
          }
        }
      },
      costDto => {

        requireWorkingInstance { workingInstance =>

          requireProduct(path) { product =>

            val wi = workingInstance.copy(cost = Some(costDto.cost))

            if (product.isValid(wi)) {
              context.journeyData.map { jd =>
                val updatedJourneyData = jd.updatePurchasedProduct(path) { product =>
                  val l = product.purchasedProductInstances
                  val m = (l.takeWhile(_.iid != iid), l.dropWhile(_.iid != iid)) match {
                    case (x, Nil) => wi :: x  //Prepend
                    case (x, y) => x ++ (wi :: y.tail)  //Replace in place
                  }
                  product.copy(purchasedProductInstances = m)
                }
                productDetailsService.cacheJourneyData(updatedJourneyData.copy(workingInstance = None))
              }
            } else {
              Logger.warn("Working instance was not valid")
            }

            Future.successful(Redirect(routes.SelectProductController.nextStep()))
          }
        }
      }
    )
  }


}
