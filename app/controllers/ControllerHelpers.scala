package controllers

import config.AppConfig
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Request, Result}
import services.{CurrencyService, ProductTreeService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

trait ControllerHelpers extends FrontendController with I18nSupport {

  def travelDetailsService: TravelDetailsService
  def productTreeService: ProductTreeService
  def currencyService: CurrencyService

  implicit def appConfig: AppConfig

  private def logAndRenderError(logMessage: String, status: Status = InternalServerError)(implicit request: Request[_]) = {
    Logger.warn(logMessage)
    Future.successful(status(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem.")))
  }

  def requireJourneyData(block: JourneyData => Future[Result])(implicit request: Request[_], messageApi: MessagesApi): Future[Result] = {

    travelDetailsService.getJourneyData flatMap {
      case Some(journeyData) =>
        block(journeyData)
      case None =>
        logAndRenderError("Unable to load required journeyData!")
    }
  }

  def requireSelectedProductPaths(block: List[ProductPath] => Future[Result])(implicit request: Request[_], messageApi: MessagesApi): Future[Result] = {

    requireJourneyData {
      case JourneyData(_, _, _, Some(selectedProducts), _) => block(selectedProducts.map(ProductPath.apply))
      case _ => Future.successful(InternalServerError(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem.")))
    }
  }



  def requirePurchasedProductInstance(journeyData: JourneyData)(path: ProductPath, index: Int)(block: PurchasedProductInstance => Future[Result])(implicit request: Request[_], messageApi: MessagesApi) = {

    journeyData.getOrCreatePurchasedProduct(path).purchasedProductInstances.getOrElse(Nil).find(_.index==index) match {
      case Some(purchasedProductInstance) => block(purchasedProductInstance)
      case None =>
        logAndRenderError(s"No purchasedProductInstance found in journeyData for $path:$index")
    }
  }


  def requirePurchasedProductInstanceWeightOrVolume(journeyData: JourneyData)(path: ProductPath, index: Int)(block: BigDecimal => Future[Result])(implicit request: Request[_], messageApi: MessagesApi) = {

    requirePurchasedProductInstance(journeyData)(path, index) { purchasedProductInstance =>
      purchasedProductInstance.weightOrVolume match {
        case Some(weightOrVolume) => block(weightOrVolume)
        case None =>
          logAndRenderError(s"No weightOrVolume found in journeyData for $path:$index")
      }
    }
  }

  def requirePurchasedProductInstanceNoOfSticks(journeyData: JourneyData)(path: ProductPath, index: Int)(block: Int => Future[Result])(implicit request: Request[_], messageApi: MessagesApi) = {

    requirePurchasedProductInstance(journeyData)(path, index) { purchasedProductInstance =>
      purchasedProductInstance.noOfSticks match {
        case Some(noOfSticks) => block(noOfSticks)
        case None =>
          logAndRenderError(s"No noOfSticks found in journeyData for $path:$index")
      }
    }
  }

  def requirePurchasedProductInstanceDescription(journeyData: JourneyData)(product: ProductTreeLeaf, path: ProductPath, index: Int)(block: String => Future[Result])(implicit request: Request[_], messageApi: MessagesApi) = {

    requirePurchasedProductInstance(journeyData)(path, index) { purchasedProductInstance =>

      block(product.getDescription(purchasedProductInstance).getOrElse(""))
    }
  }

  def requirePurchasedProductInstanceCurrency(journeyData: JourneyData)(path: ProductPath, index: Int)(block: Currency => Future[Result])(implicit request: Request[_], messageApi: MessagesApi) = {

    requirePurchasedProductInstance(journeyData)(path, index) { purchasedProductInstance =>
      purchasedProductInstance.currency match {
        case Some(currency) =>
          currencyService.getCurrencyByCode(currency) match {
            case Some(currency) => block(currency)
            case None =>
              logAndRenderError("Unable to fetch currency for " + currency)

          }

        case None => logAndRenderError(s"No currency found in journeyData for $path:$index")
      }
    }
  }


  def withNextSelectedProductPath(block: Option[ProductPath] => Future[Result])(implicit request: Request[_], messageApi: MessagesApi): Future[Result] = {
    requireSelectedProductPaths {
      case Nil => block(None)
      case x :: _ => block(Some(x))
    }
  }

  def requireProductOrCategory(path: ProductPath)(block: ProductTreeNode => Future[Result])(implicit request: Request[_], messageApi: MessagesApi): Future[Result] = {
    productTreeService.getProducts.getDescendant(path) match {
      case Some(node) => block(node)
      case None =>
        logAndRenderError(s"Product or category not found at $path", NotFound)
    }
  }

  def requireProduct(path: ProductPath)(block: ProductTreeLeaf => Future[Result])(implicit request: Request[_], messageApi: MessagesApi): Future[Result] = {
    requireProductOrCategory(path) {
      case leaf: ProductTreeLeaf => block(leaf)
      case _ =>
        logAndRenderError(s"Product not found at $path", NotFound)
    }
  }

  def requireCategory(path: ProductPath)(block: ProductTreeBranch => Future[Result])(implicit request: Request[_], messageApi: MessagesApi): Future[Result] = {
    requireProductOrCategory(path) {
      case branch: ProductTreeBranch => block(branch)
      case _ =>
        logAndRenderError(s"Category not found at $path", NotFound)
    }
  }

}