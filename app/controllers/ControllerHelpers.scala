/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.AppConfig
import connectors.Cache
import models._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.controller.{Utf8MimeTypes, WithJsonBody}
import scala.concurrent.{ExecutionContext, Future}

trait ControllerHelpers
    extends MessagesBaseController
    with Utf8MimeTypes
    with WithJsonBody
    with FrontendHeaderCarrierProvider
    with I18nSupport {

  def cache: Cache
  def productTreeService: ProductTreeService
  def calculatorService: CalculatorService

  def errorTemplate: views.html.errorTemplate

  implicit def appConfig: AppConfig
  implicit def ec: ExecutionContext

  private val logger = Logger(this.getClass)

  implicit def contextToRequest(implicit localContext: LocalContext): Request[AnyContent] = localContext.request

  def logAndRenderError(logMessage: String, status: Status = InternalServerError)(implicit
    context: LocalContext
  ): Future[Result] = {
    logger.warn(logMessage)
    Future.successful(status(errorTemplate()))
  }

  def logAndRedirect(logMessage: String, redirectLocation: Call): Future[Result] = {
    logger.warn(logMessage)
    Future.successful(Redirect(redirectLocation))
  }

  def requireCalculatorResponse(
    block: CalculatorResponse => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    context.getJourneyData match {
      case JourneyData(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            Some(calculatorResponse),
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        block(calculatorResponse)
      case _ =>
        logAndRedirect(
          s"Missing calculator response in journeyData! Redirecting to dashboard...",
          routes.DashboardController.showDashboard
        )
    }

  def requireLimitUsage(journeyData: JourneyData)(
    block: Map[String, BigDecimal] => Future[Result]
  )(implicit context: LocalContext, hc: HeaderCarrier): Future[Result] =
    calculatorService.limitUsage(journeyData) flatMap { response: LimitUsageResponse =>
      response match {
        case LimitUsageSuccessResponse(r) =>
          block(r.map(x => (x._1, BigDecimal(x._2))))
        case _                            =>
          logAndRenderError("Fetching limits was unsuccessful")
      }

    }

  def requireJourneyData(
    block: JourneyData => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    context.journeyData match {
      case Some(journeyData) =>
        block(journeyData)
      case None              =>
        logAndRedirect(
          "Unable to get journeyData! Starting a new session...",
          routes.TravelDetailsController.newSession
        )
    }

  def revertWorkingInstance(
    block: => Future[Result]
  )(implicit context: LocalContext): Future[Result] = {

    val edit       = context.getJourneyData.workingInstance.exists(_.cost.isDefined)
    val workingIid = context.getJourneyData.workingInstance.map(_.iid)
    if (workingIid.isDefined) {
      if (edit) {
        cache.store(context.getJourneyData.revertPurchasedProductInstance()).flatMap(_ => block)
      } else {
        cache.store(context.getJourneyData.removePurchasedProductInstance(workingIid.get)).flatMap(_ => block)
      }
    } else {
      block
    }
  }

  def requirePurchasedProductInstance(iid: String)(
    block: PurchasedProductInstance => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    requireJourneyData { journeyData =>
      journeyData.getPurchasedProductInstance(iid) match {
        case Some(ppi) =>
          if (context.getJourneyData.workingInstance.isEmpty) {
            cache.store(context.getJourneyData.copy(workingInstance = Some(ppi))).flatMap(_ => block(ppi))
          } else {
            block(ppi)
          }
        case None      => logAndRenderError(s"No purchasedProductInstance found in journeyData for iid: $iid!", NotFound)
      }
    }

  def requireTravelDetails(
    block: => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    context.getJourneyData match {
      case JourneyData(
            _,
            Some(_),
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            Some(_),
            Some(_),
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) if appConfig.isVatResJourneyEnabled =>
        block
      case JourneyData(
            _,
            Some(_),
            _,
            _,
            _,
            _,
            _,
            None,
            None,
            _,
            Some(_),
            Some(_),
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) if !appConfig.isVatResJourneyEnabled =>
        block
      case _ =>
        logAndRedirect(
          s"Incomplete or missing travel details found in journeyData! Starting a new session... ",
          routes.TravelDetailsController.newSession
        )
    }

  def withClearWorkingInstance(block: => Future[Result])(implicit context: LocalContext): Future[Result] =
    cache.store(context.getJourneyData.copy(workingInstance = None)).flatMap(_ => block)

  def withNextSelectedProductAlias(
    block: Option[ProductAlias] => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    withClearWorkingInstance {
      context.getJourneyData.selectedAliases match {
        case Nil               => block(None)
        case productAlias :: _ => block(Some(productAlias))
      }
    }

  def requireProductOrCategory(path: ProductPath)(
    block: ProductTreeNode => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    productTreeService.productTree.getDescendant(path) match {
      case Some(node) => block(node)
      case None       =>
        logAndRenderError(s"Product or category not found at $path!", NotFound)
    }

  def requireProduct(path: ProductPath)(
    block: ProductTreeLeaf => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    requireProductOrCategory(path) {
      case leaf: ProductTreeLeaf => block(leaf)
      case _                     =>
        logAndRenderError(s"Product not found at $path!", NotFound)
    }

  def requireCategory(path: ProductPath)(
    block: ProductTreeBranch => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    requireProductOrCategory(path) {
      case branch: ProductTreeBranch => block(branch)
      case _                         =>
        logAndRenderError(s"Category not found at $path!", NotFound)
    }

  def withDefaults(jd: JourneyData)(
    block: Option[String] => Option[String] => Option[String] => Future[Result]
  ): Future[Result] =
    jd match {
      case JourneyData(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            defaultCountry,
            defaultOriginCountry,
            defaultCurrency,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        block(defaultCountry)(defaultOriginCountry)(defaultCurrency)
    }
}
