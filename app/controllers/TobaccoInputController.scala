package controllers

import config.AppConfig
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Result}
import services.{CurrencyService, ProductTreeService, PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class TobaccoInputController @Inject()(
  val travelDetailsService: TravelDetailsService,
  val purchasedProductService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  def loadTobaccoInputPage(path: ProductPath, iid: String): Future[Result] = {
    Future.successful {
      path.components.last match {
        case "cigarettes"            =>  Redirect(routes.TobaccoInputController.displayNoOfSticksInput(path, iid))
        case "cigars" | "cigarillos" =>  Redirect(routes.TobaccoInputController.displayNoOfSticksWeightInput(path, iid))
        case "rolling" | "chewing"   =>  Redirect(routes.TobaccoInputController.displayWeightInput(path, iid))
      }
    }
  }

  def inputNoOfSticksAndWeight(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>
    val iid = generateIid
    loadTobaccoInputPage(path, iid)
  }

  def updateNoOfSticksAndWeight(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) flatMap { _ =>
        loadTobaccoInputPage(path, iid)
      }
    }
  }


  def displayNoOfSticksInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, _, _, Some(qty), _, _)) => NoOfSticksDto.form.fill(NoOfSticksDto(qty))
        case _ => NoOfSticksDto.form
      }
    }

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.no_of_sticks_input(form, product.token, product.name, path, iid)))
    }
  }

  def processNoOfSticksInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    NoOfSticksDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.no_of_sticks_input(formWithErrors, product.token, product.name, path, iid)))
        }
      },
      dto => {

        context.getJourneyData.workingInstance match {
          case Some(wi) if wi.iid == iid => purchasedProductService.updateNoOfSticks(context.getJourneyData, path, iid, dto.noOfSticks) map { _ =>
            Redirect(routes.DashboardController.showDashboard())
          }
          case _ => purchasedProductService.storeNoOfSticks(context.getJourneyData, path, iid, dto.noOfSticks) map { _ =>
            Redirect(routes.TobaccoInputController.displayCurrencyInput(path, iid))
          }
        }
      }
    )
  }


  def displayNoOfSticksWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, _, Some(weight), Some(qty), _, _)) => NoOfSticksAndWeightDto.form.fill(NoOfSticksAndWeightDto(qty, weight))
        case _ => NoOfSticksAndWeightDto.form
      }
    }
    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.no_of_sticks_weight_input(form, product.token, product.name, path, iid)))
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

        context.getJourneyData.workingInstance match {
          case Some(wi) if wi.iid == iid => purchasedProductService.updateNoOfSticksAndWeightOrVolume(context.getJourneyData, path, iid, dto.noOfSticks, dto.weight) map { _ =>
            Redirect(routes.DashboardController.showDashboard())
          }
          case _ => purchasedProductService.storeNoOfSticksAndWeightOrVolume(context.getJourneyData, path, iid, dto.noOfSticks, dto.weight) map { _ =>
            Redirect(routes.TobaccoInputController.displayCurrencyInput(path, iid))
          }
        }
      }
    )
  }


  def displayWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, _, Some(weight), _, _, _)) => WeightDto.form.fill(WeightDto(weight))
        case _ => WeightDto.form
      }
    }

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.weight_input(form, product.token, product.name, path, iid)))
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

        context.getJourneyData.workingInstance match {
          case Some(wi) if wi.iid == iid => purchasedProductService.updateWeightOrVolume(context.getJourneyData, path, iid, dto.weight) map { _ =>
            Redirect(routes.DashboardController.showDashboard())
          }
          case _ => purchasedProductService.storeWeightOrVolume(context.getJourneyData, path, iid, dto.weight) map { _ =>
            Redirect(routes.TobaccoInputController.displayCurrencyInput(path, iid))
          }
        }
      }
    )
  }

  def displayCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, _, _, _, Some(currency), _)) => CurrencyDto.form(currencyService).fill(CurrencyDto(currency, 0))
        case _ => CurrencyDto.form(currencyService)
      }
    }

    requireProduct(path) { product =>
      requireWorkingInstanceDescription(product) { description =>
        Future.successful(Ok(views.html.tobacco.currency_input(form, product, currencyService.getAllCurrencies, description, path, iid)))
      }
    }
  }

  def displayCurrencyUpdate(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) flatMap { _ =>
        Future.successful(Redirect(routes.TobaccoInputController.displayCurrencyInput(path, iid)))
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
        purchasedProductService.storeCurrency(context.getJourneyData, path, iid, currencyDto.currency) map { _ =>
          Redirect(routes.TobaccoInputController.displayCostInput(path, iid))
        }
      }
    )
  }


  def displayCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, _, _, _, _, Some(cost)))  => CostDto.form().fill(CostDto(cost, 0))
        case _ => CostDto.form()
      }
    }

    requireProduct(path) { product =>
      requireWorkingInstanceDescription(product) { description =>
        requireWorkingInstanceCurrency { currency =>
            Future.successful(Ok(views.html.tobacco.cost_input(form, product, path, iid, description, currency.displayName)))
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
      dto => {

        requireWorkingInstance { workingInstance =>

          requireProduct(path) { product =>

            val wi = workingInstance.copy(cost = Some(dto.cost))

            if (product.isValid(wi)) {
              context.journeyData.map { jd =>
                val l = jd.purchasedProductInstances
                val m = (l.takeWhile(_.iid != iid), l.dropWhile(_.iid != iid)) match {
                  case (x, Nil) => wi :: x  //Prepend
                  case (x, y) => x ++ (wi :: y.tail)  //Replace in place
                }
                purchasedProductService.cacheJourneyData(jd.copy(purchasedProductInstances = m, workingInstance = None))
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
