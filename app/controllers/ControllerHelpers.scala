package controllers

import config.AppConfig
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{CurrencyService, ProductTreeService, TravelDetailsService}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future
import scala.util.Random

trait ControllerHelpers extends FrontendController with I18nSupport {

  def travelDetailsService: TravelDetailsService
  def productTreeService: ProductTreeService
  def currencyService: CurrencyService

  implicit def appConfig: AppConfig
  implicit def messagesApi: play.api.i18n.MessagesApi


  def PublicAction(block: LocalContext => Future[Result]): Action[AnyContent] = {

    UnauthorisedAction.async { implicit request =>

      trimmingFormUrlEncodedData { implicit request =>

        request.session.get(SessionKeys.sessionId) match {
          case Some(s) =>
            block(LocalContext(request))
          case None =>
            Future.successful(Redirect(routes.TravelDetailsController.newSession()))
        }

      }
    }
  }

  def DashboardAction(block: LocalContext => Future[Result]): Action[AnyContent] = {

    PublicAction { implicit context =>
      requireJourneyData { jd =>
        implicit val ctxWithJd = context.withJourneyData(jd)
        requireTravelDetails {
          block(ctxWithJd)
        }(ctxWithJd, implicitly)
      }
    }
  }

  def trimmingFormUrlEncodedData(block: Request[AnyContent] => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    block {
      request.map {
        case AnyContentAsFormUrlEncoded(data) =>
          AnyContentAsFormUrlEncoded(data.map {
            case (key, vals) => (key, vals.map(_.trim))
          })
        case b => b
      }
    }
  }

  def logAndRenderError(logMessage: String, status: Status = InternalServerError)(implicit context: LocalContext): Future[Result] = {
    Future.successful(status(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem.")))
  }

  def logAndRedirect(logMessage: String, redirectLocation: Call)(implicit context: LocalContext): Future[Result] = {
    Logger.warn(logMessage)
    Future.successful(Redirect(redirectLocation))
  }

  def requireJourneyData(block: JourneyData => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    travelDetailsService.getJourneyData flatMap {
      case Some(journeyData) =>
        block(journeyData)
      case None =>
        logAndRedirect("Unable to load required journeyData! Redirecting to country-of-purchase...", routes.TravelDetailsController.newSession())
    }
  }

  def requireTravelDetails(block: => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    context.getJourneyData match {
      case JourneyData(_, Some(ageOver17), Some(privateCraft), _, _) => block
      case _ =>
        logAndRedirect(s"Incomplete or missing travel details found in journeyData! Redirecting to country-of-purchase...", routes.TravelDetailsController.newSession())
    }
  }

  def requirePurchasedProductInstance(path: ProductPath, iid: String)(block: PurchasedProductInstance => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    context.getJourneyData.getOrCreatePurchasedProduct(path).purchasedProductInstances.find(_.iid==iid) match {
      case Some(purchasedProductInstance) => block(purchasedProductInstance)
      case None =>
        logAndRedirect(s"No purchasedProductInstance found in journeyData for $path:$iid! Redirecting to dashboard...", routes.DashboardController.showDashboard())
    }
  }

  def withPurchasedProductInstanceCount(path: ProductPath, iid: String)(block: Int => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {
    block(context.getJourneyData.getOrCreatePurchasedProduct(path).purchasedProductInstances.size)
  }


  def requirePurchasedProductInstanceWeightOrVolume(path: ProductPath, iid: String)(block: BigDecimal => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    requirePurchasedProductInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.weightOrVolume match {
        case Some(weightOrVolume) => block(weightOrVolume)
        case None =>
          logAndRedirect(s"No weightOrVolume found in journeyData for $path:$iid! Redirecting to dashboard...", routes.DashboardController.showDashboard())
      }
    }
  }

  def requirePurchasedProductInstanceNoOfSticks(path: ProductPath, iid: String)(block: Int => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    requirePurchasedProductInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.noOfSticks match {
        case Some(noOfSticks) => block(noOfSticks)
        case None =>
          logAndRedirect(s"No noOfSticks found in journeyData for $path:$iid! Redirecting to dashboard...", routes.DashboardController.showDashboard())
      }
    }
  }

  def requirePurchasedProductInstanceDescription(product: ProductTreeLeaf, path: ProductPath, iid: String)(block: String => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    requirePurchasedProductInstance(path, iid) { purchasedProductInstance =>
      block(product.getDescription(purchasedProductInstance).getOrElse(""))
    }
  }

  def requirePurchasedProductInstanceCurrency(path: ProductPath, iid: String)(block: Currency => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    requirePurchasedProductInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.currency match {
        case Some(currency) =>
          currencyService.getCurrencyByCode(currency) match {
            case Some(curr) => block(curr)
            case None =>
              logAndRedirect(s"Unable to fetch currency for $currency!", routes.DashboardController.showDashboard())

          }
        case None => logAndRedirect(s"No currency found in journeyData for $path:$iid! Redirecting to dashboard...", routes.DashboardController.showDashboard())
      }
    }
  }


  def withNextSelectedProductPath(block: Option[ProductPath] => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {
    context.getJourneyData.selectedProducts match {
      case Nil => block(None)
      case x :: _ => block(Some(ProductPath(x)))
    }
  }

  def requireProductOrCategory(path: ProductPath)(block: ProductTreeNode => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {
    productTreeService.getProducts.getDescendant(path) match {
      case Some(node) => block(node)
      case None =>
        logAndRenderError(s"Product or category not found at $path!", NotFound)
    }
  }

  def requireProduct(path: ProductPath)(block: ProductTreeLeaf => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {
    requireProductOrCategory(path) {
      case leaf: ProductTreeLeaf => block(leaf)
      case _ =>
        logAndRenderError(s"Product not found at $path!", NotFound)
    }
  }

  def requireCategory(path: ProductPath)(block: ProductTreeBranch => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {
    requireProductOrCategory(path) {
      case branch: ProductTreeBranch => block(branch)
      case _ =>
        logAndRenderError(s"Category not found at $path!", NotFound)
    }
  }

  def generateIid: String = Random.alphanumeric.take(6).mkString
}