/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.{FrontendHeaderCarrierProvider, Utf8MimeTypes, WithJsonBody}

import scala.concurrent.{ExecutionContext, Future}


trait ControllerHelpers extends MessagesBaseController
  with Utf8MimeTypes
  with WithJsonBody
  with FrontendHeaderCarrierProvider with I18nSupport {

  def cache: Cache
  def productTreeService: ProductTreeService
  def calculatorService: CalculatorService

  def error_template: views.html.error_template

  implicit def appConfig: AppConfig
  implicit def ec: ExecutionContext


  implicit def contextToRequest(implicit localContext: LocalContext)= localContext.request

  def logAndRenderError(logMessage: String, status: Status = InternalServerError)(implicit context: LocalContext): Future[Result] = {
    Logger.warn(logMessage)
    Future.successful(status(error_template("Technical problem", "Technical problem", "There has been a technical problem.")))
  }

  def logAndRedirect(logMessage: String, redirectLocation: Call)(implicit context: LocalContext): Future[Result] = {
    Logger.warn(logMessage)
    Future.successful(Redirect(redirectLocation))
  }

  def requireCalculatorResponse(block: CalculatorResponse => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    context.getJourneyData match {
      case JourneyData(_, _,_, _, _, _, _, _, _, _, _, _, Some(calculatorResponse), _, _) => block(calculatorResponse)
      case _ =>
        logAndRedirect(s"Missing calculator response in journeyData! Redirecting to dashboard...", routes.DashboardController.showDashboard())
    }
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
        logAndRedirect("Unable to get journeyData! Starting a new session...", routes.TravelDetailsController.newSession())
    }
  }

  def requirePurchasedProductInstance(iid: String)(block: PurchasedProductInstance => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    requireJourneyData { journeyData =>

      journeyData.getPurchasedProductInstance(iid) match {
        case Some(ppi) => block(ppi)
        case None => logAndRenderError(s"No purchasedProductInstance found in journeyData for iid: $iid!", NotFound)
      }
    }
  }


  def requireTravelDetails(block: => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {

    context.getJourneyData match {
      case JourneyData(Some(_), _,_, _,  _,Some(_), Some(_), _, _, _, _, _, _, _, _) if appConfig.isVatResJourneyEnabled => block
      case JourneyData(Some(_), _, None, None, _, Some(_), Some(_), _, _, _, _, _, _, _, _) if !appConfig.isVatResJourneyEnabled => block
      case _ =>
        logAndRedirect(s"Incomplete or missing travel details found in journeyData! Starting a new session... ", routes.TravelDetailsController.newSession())
    }
  }


  def withNextSelectedProductAlias(block: Option[ProductAlias] => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {
    context.getJourneyData.selectedAliases match {
      case Nil => block(None)
      case productAlias :: _ => block(Some(productAlias))
    }
  }

  def requireProductOrCategory(path: ProductPath)(block: ProductTreeNode => Future[Result])(implicit context: LocalContext, messagesApi: MessagesApi): Future[Result] = {
    productTreeService.productTree.getDescendant(path) match {
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

  def withDefaults(jd: JourneyData)(block: Option[String] =>  Option[String] => Future[Result])(implicit context: LocalContext): Future[Result] = {
    jd match {
      case JourneyData(_, _,_, _, _, _, _, _, _, _, _, _, _, defaultCountry, defaultCurrency) => block(defaultCountry)(defaultCurrency)
    }
  }
}
