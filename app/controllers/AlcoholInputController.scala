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

class AlcoholInputController @Inject() (
  val travelDetailsService: TravelDetailsService,
  val productDetailsService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {


  def startInputJourney(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    Future.successful(Redirect(routes.AlcoholInputController.displayVolumeInput(path, generateIid)))
  }

  def displayVolumeInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.alcohol.volume_input(VolumeDto.form, product.name, product.token, path, iid)))
    }
  }

  def processVolumeInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    VolumeDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.alcohol.volume_input(formWithErrors, product.name, product.token, path, iid)))
        }
      },
      volumeDto => {
        productDetailsService.storeWeightOrVolume(context.getJourneyData, path, iid, volumeDto.volume) map { _ =>
          Redirect(routes.AlcoholInputController.displayCurrencyInput(path, iid))
        }
      }
    )
  }

  def displayCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requireWorkingInstanceWeightOrVolume { volume =>
      requireProduct(path) { product =>
        Future.successful(Ok(views.html.alcohol.currency_input(CurrencyDto.form(currencyService), product, path, iid, currencyService.getAllCurrencies, volume)))
      }
    }
  }

  def processCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {
        requireWorkingInstanceWeightOrVolume { volume =>
          requireProduct(path) { product =>
            Future.successful(BadRequest(views.html.alcohol.currency_input(formWithErrors, product, path, iid, currencyService.getAllCurrencies, volume)))
          }
        }
      },
      currencyDto => {
        productDetailsService.storeCurrency(context.getJourneyData, path, iid, currencyDto.currency) map { _ =>
          Redirect(routes.AlcoholInputController.displayCostInput(path, iid))
        }
      }
    )
  }

  def displayCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requireWorkingInstanceWeightOrVolume { volume =>
      requireWorkingInstanceCurrency { currency: Currency =>
        requireProduct(path) { product =>
          Future.successful(Ok(views.html.alcohol.cost_input(CostDto.form(), product, path, iid, volume, currency.displayName)))
        }
      }
    }
  }

  def processCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CostDto.form().bindFromRequest.fold(
      formWithErrors => {
        requireWorkingInstanceWeightOrVolume { volume =>
          requireWorkingInstanceCurrency { currency: Currency =>
            requireProduct(path) { product =>
              Future.successful(BadRequest(views.html.alcohol.cost_input(formWithErrors, product, path, iid, volume, currency.displayName)))
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
