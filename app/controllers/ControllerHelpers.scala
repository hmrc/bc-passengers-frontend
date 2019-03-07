package controllers

import config.AppConfig
import connectors.Cache
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendHeaderCarrierProvider, Utf8MimeTypes, WithJsonBody}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


trait ControllerHelpers extends MessagesBaseController
  with Utf8MimeTypes
  with WithJsonBody
  with FrontendHeaderCarrierProvider with I18nSupport {

  def cache: Cache
  def productTreeService: ProductTreeService
  def currencyService: CurrencyService
  def countriesService: CountriesService
  def calculatorService: CalculatorService

  def error_template: views.html.error_template

  implicit def appConfig: AppConfig
  implicit def messagesApi: play.api.i18n.MessagesApi
  implicit def ec: ExecutionContext


  def PublicAction(block: LocalContext => Future[Result]): Action[AnyContent] = {

    Action.async { implicit request =>

      trimmingFormUrlEncodedData { implicit request =>

        request.session.get(SessionKeys.sessionId) match {
          case Some(s) =>
            block(LocalContext(request, s))
          case None =>
            Future.successful(Redirect(routes.TravelDetailsController.newSession()))
        }

      }
    }
  }

  def DashboardAction(block: LocalContext => Future[Result]): Action[AnyContent] = {

    PublicAction { implicit context =>

      cache.fetch(hc(context.request)) flatMap {

        case Some(journeyData) =>

          implicit val ctxWithJd = context.withJourneyData(journeyData)

          requireTravelDetails {

            block(ctxWithJd)
          }(ctxWithJd, implicitly)
        case None =>
          logAndRedirect("Unable to get journeyData! Redirecting to country-of-purchase...", routes.TravelDetailsController.newSession())
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

  implicit def contextToRequest(implicit localContext: LocalContext)= localContext.request

  def logAndRenderError(logMessage: String, status: Status = InternalServerError)(implicit context: LocalContext): Future[Result] = {
    Logger.warn(logMessage)
    Future.successful(status(error_template("Technical problem", "Technical problem", "There has been a technical problem.")))
  }

  def logAndRedirect(logMessage: String, redirectLocation: Call)(implicit context: LocalContext): Future[Result] = {
    Logger.warn(logMessage)
    Future.successful(Redirect(redirectLocation))
  }

  def requireLimitUsage(journeyData: JourneyData)(block: Map[String, BigDecimal] => Future[Result])(implicit context: LocalContext, hc: HeaderCarrier) = {

    calculatorService.limitUsage(journeyData) flatMap { response: LimitUsageResponse =>

      response match {
        case LimitUsageSuccessResponse(r) =>
          block(r.map( x => (x._1, BigDecimal(x._2)) ))
        case _ =>
          logAndRenderError("Fetching limits was unsuccessful")
      }

    }
  }

  def requireJourneyData(block: JourneyData => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    context.journeyData match {
      case Some(journeyData) =>
        block(journeyData)
      case None =>
        logAndRedirect("Unable to get journeyData! Redirecting to country-of-purchase...", routes.TravelDetailsController.newSession())
    }
  }


  def requireCalculatorResponse(block: CalculatorResponse => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    context.getJourneyData match {
      case JourneyData(_, _, _, _, _, _, _, _, _, Some(calculatorResponse)) => block(calculatorResponse)
      case _ =>
        logAndRedirect(s"Missing calculator response in journeyData! Redirecting to dashboard...", routes.DashboardController.showDashboard())
    }
  }


  def requireUserInformation(block: UserInformation => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    context.getJourneyData match {
      case JourneyData(_, _, _, _, _, _, _, _, Some(userInformation), _) => block(userInformation)
      case _ =>
        logAndRedirect(s"Missing user info in journeyData! Redirecting to dashboard...", routes.DashboardController.showDashboard())
    }
  }

  def requirePurchasedProductInstance(path: ProductPath, iid: String)(block: PurchasedProductInstance => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    block(context.getJourneyData.getOrCreatePurchasedProductInstance(path, iid))
  }

  def requirePurchasedProductInstanceDescription(product: ProductTreeLeaf, path: ProductPath, iid: String)(block: String => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    requirePurchasedProductInstance(path, iid) { purchasedProductInstance =>
      product.getDescription(purchasedProductInstance) match {
        case Some(desc) => block(desc)
        case None =>
          logAndRenderError(s"No purchasedProductInstance found in journeyData for $path:$iid!")
      }
    }
  }

  def requireWorkingInstance(block: PurchasedProductInstance => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    context.getJourneyData match {
      case JourneyData(_, _, _, _, _, _, _, Some(workingInstance), _, _) => block(workingInstance)
      case _ =>
        logAndRedirect(s"Missing working instance in journeyData! Redirecting to dashboard...", routes.DashboardController.showDashboard())
    }
  }

  def requireTravelDetails(block: => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    context.getJourneyData match {
      case JourneyData(Some(_), _, _, Some(_), Some(_), _, _, _, _, _) if appConfig.usingVatResJourney => block
      case JourneyData(Some(_), None, None, Some(_), Some(_), _, _, _, _, _) if !appConfig.usingVatResJourney => block
      case _ =>
        logAndRedirect(s"Incomplete or missing travel details found in journeyData! Redirecting to country-of-purchase...", routes.TravelDetailsController.newSession())
    }
  }

  def requireCountryByName(countryName: String)(block: Country => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    countriesService.getCountryByName(countryName) match {
      case Some(country) => block(country)
      case _ =>
        logAndRedirect(s"Country missing in the countries lookup service! Redirecting to country-of-purchase...", routes.TravelDetailsController.newSession())
    }
  }


  def requireWorkingInstanceWeightOrVolume(block: BigDecimal => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    requireWorkingInstance { purchasedProductInstance =>
      purchasedProductInstance.weightOrVolume match {
        case Some(weightOrVolume) => block(weightOrVolume)
        case None =>
          logAndRedirect(s"No weightOrVolume found in working instance! Redirecting to dashboard...", routes.DashboardController.showDashboard())
      }
    }
  }

  def requireWorkingInstanceDescription(product: ProductTreeLeaf)(block: String => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    requireWorkingInstance { purchasedProductInstance =>
      block(product.getDescription(purchasedProductInstance).getOrElse(""))
    }

  }

  def requireWorkingInstanceCurrency(block: Currency => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    requireWorkingInstance {
      case PurchasedProductInstance(_, _, _, _, _, Some(currency), _) =>
        currencyService.getCurrencyByCode(currency) match {
          case Some(curr) => block(curr)
          case None =>
            logAndRedirect(s"Unable to fetch currency for $currency!", routes.DashboardController.showDashboard())

        }
      case _ =>
        logAndRedirect(s"Unable to extract working currency", routes.DashboardController.showDashboard())
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

  def acceptingValidWorkingInstance(workingInstance: Option[PurchasedProductInstance], product: ProductTreeLeaf)(block: Option[JourneyData] => Future[Result])(implicit context: LocalContext): Future[Result] = {

    workingInstance match {
      case Some(workingInstance) if product.isValid(workingInstance) =>
        val updatedPurchasedProductInstances = replaceProductInPlace(context.getJourneyData.purchasedProductInstances, workingInstance)
        block(Some(context.getJourneyData.copy(purchasedProductInstances = updatedPurchasedProductInstances, workingInstance = None)))
      case _ =>
        Logger.warn("Working instance was not valid")
        block(None)
    }
  }

  def replaceProductInPlace(purchasedProductInstances: List[PurchasedProductInstance], productToReplace: PurchasedProductInstance): List[PurchasedProductInstance] = {
    (purchasedProductInstances.takeWhile(_.iid != productToReplace.iid), purchasedProductInstances.dropWhile(_.iid != productToReplace.iid)) match {
      case (x, Nil) => productToReplace :: x  //Prepend
      case (x, y) => x ++ (productToReplace :: y.tail)  //Replace in place
    }
  }


  def addingWeightOrVolumeToWorkingInstance(journeyData: JourneyData, path: ProductPath, iid: String, weightOrVolume: Option[BigDecimal])(implicit hc: HeaderCarrier, ex: ExecutionContext): JourneyData = {

    journeyData.withUpdatedWorkingInstance(path, iid) { workingInstance =>
      workingInstance.copy(path = path, iid = iid, weightOrVolume = weightOrVolume)
    }

  }

  def addingNoOfSticksToWorkingInstance(journeyData: JourneyData, path: ProductPath, iid: String, noOfSticks: Option[Int])(implicit hc: HeaderCarrier, ex: ExecutionContext): JourneyData = {

    journeyData.withUpdatedWorkingInstance(path, iid) { workingInstance =>
      workingInstance.copy(path = path, iid = iid, noOfSticks = noOfSticks)
    }

  }

  def addingNoOfSticksAndWeightOrVolumeToWorkingInstance(journeyData: JourneyData, path: ProductPath, iid: String, noOfSticks: Option[Int], weightOrVolume: Option[BigDecimal])(implicit hc: HeaderCarrier, ex: ExecutionContext): JourneyData = {

    journeyData.withUpdatedWorkingInstance(path, iid) { workingInstance =>
      workingInstance.copy(path = path, iid = iid, noOfSticks = noOfSticks, weightOrVolume = weightOrVolume)
    }

  }

  def generateIid: String = Random.alphanumeric.take(6).mkString
}