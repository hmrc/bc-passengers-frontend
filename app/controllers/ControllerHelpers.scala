/*
 * Copyright 2026 HM Revenue & Customs
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
import models.*
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.*
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

  given appConfig: AppConfig
  given ec: ExecutionContext

  private val logger = Logger(this.getClass)

  protected val returnToAddedItemSessionKey          = "return-to-added-item-url"
  protected val returnToAddedItemSelectUrlSessionKey = "return-to-added-item-select-url"
  protected val returnToAddedItemProductPathKey      = "return-to-added-item-product-path"
  protected val returnToAddedItemDashboardUrlSessionKey = "return-to-added-item-dashboard-url"
  protected val returnToAddedItemStackSessionKey     = "return-to-added-item-stack"

  private case class AddedItemBackLink(editUrl: String, selectUrl: String, productPath: String, dashboardUrl: String)

  private val addedItemBackLinkPartSeparator  = "|"
  private val addedItemBackLinkEntrySeparator = "\n"

  private def serialiseAddedItemBackLinks(backLinks: List[AddedItemBackLink]): String =
    backLinks
      .map(backLink =>
        List(backLink.editUrl, backLink.selectUrl, backLink.productPath, backLink.dashboardUrl)
          .mkString(addedItemBackLinkPartSeparator)
      )
      .mkString(addedItemBackLinkEntrySeparator)

  private def deserialiseAddedItemBackLinks(value: String): List[AddedItemBackLink] =
    value
      .split(addedItemBackLinkEntrySeparator)
      .toList
      .flatMap { entry =>
        entry.split("\\|", -1).toList match {
          case editUrl :: selectUrl :: productPath :: dashboardUrl :: Nil =>
            Some(AddedItemBackLink(editUrl, selectUrl, productPath, dashboardUrl))
          case editUrl :: selectUrl :: productPath :: Nil                 =>
            Some(AddedItemBackLink(editUrl, selectUrl, productPath, editUrl))
          case _                                                          => None
        }
      }

  private def currentAddedItemBackLink(implicit context: LocalContext): Option[AddedItemBackLink] =
    for {
      editUrl     <- context.request.session.get(returnToAddedItemSessionKey)
      selectUrl   <- context.request.session.get(returnToAddedItemSelectUrlSessionKey)
      productPath <- context.request.session.get(returnToAddedItemProductPathKey)
    } yield AddedItemBackLink(
      editUrl,
      selectUrl,
      productPath,
      context.request.session.get(returnToAddedItemDashboardUrlSessionKey).getOrElse(editUrl)
    )

  private def addedItemBackLinkStack(implicit context: LocalContext): List[AddedItemBackLink] =
    context.request.session
      .get(returnToAddedItemStackSessionKey)
      .fold(currentAddedItemBackLink.toList)(
        deserialiseAddedItemBackLinks
      )

  protected def markReturnToAddedItem(
    result: Result,
    editUrl: String,
    productPath: ProductPath,
    dashboardUrl: Option[String] = None
  )(implicit
    context: LocalContext
  ): Result = {
    val selectUrl = routes.SelectProductController
      .askProductSelection(ProductPath(productPath.components.dropRight(1)))
      .url
    val backLink  = AddedItemBackLink(editUrl, selectUrl, productPath.toString, dashboardUrl.getOrElse(editUrl))
    val stack     = addedItemBackLinkStack.filterNot(_.editUrl == editUrl) :+ backLink
    val session   = context.request.session.data ++ Map(
      returnToAddedItemSessionKey             -> editUrl,
      returnToAddedItemSelectUrlSessionKey    -> selectUrl,
      returnToAddedItemProductPathKey         -> productPath.toString,
      returnToAddedItemDashboardUrlSessionKey -> backLink.dashboardUrl,
      returnToAddedItemStackSessionKey        -> serialiseAddedItemBackLinks(stack)
    )
    val updatedSession =
      if (dashboardUrl.isDefined) session - AddAnotherItemDto.sessionKey else session

    result.withSession(Session(updatedSession))
  }

  protected def clearReturnToAddedItem(result: Result)(implicit context: LocalContext): Result =
    result.removingFromSession(
      returnToAddedItemSessionKey,
      returnToAddedItemSelectUrlSessionKey,
      returnToAddedItemProductPathKey,
      returnToAddedItemDashboardUrlSessionKey,
      returnToAddedItemStackSessionKey
    )(using context.request)

  protected def popReturnToAddedItem(result: Result)(implicit context: LocalContext): Result =
    addedItemBackLinkStack.dropRight(1).lastOption match {
      case Some(backLink) =>
        result.addingToSession(
          returnToAddedItemSessionKey          -> backLink.editUrl,
          returnToAddedItemSelectUrlSessionKey -> backLink.selectUrl,
          returnToAddedItemProductPathKey      -> backLink.productPath,
          returnToAddedItemDashboardUrlSessionKey -> backLink.dashboardUrl,
          returnToAddedItemStackSessionKey     -> serialiseAddedItemBackLinks(addedItemBackLinkStack.dropRight(1))
        )(using context.request)
      case None           => clearReturnToAddedItem(result)
    }

  protected def clearReturnToAddedItemUnlessCurrentEdit(result: Result, editUrl: String)(implicit
    context: LocalContext
  ): Result =
    if (context.request.session.get(returnToAddedItemSessionKey).contains(editUrl)) result
    else clearReturnToAddedItem(result)

  protected def backLinkForAddedItemEdit(defaultBackLink: Option[String], editUrl: String)(implicit
    context: LocalContext
  ): Option[String] =
    context.request.session
      .get(returnToAddedItemSessionKey)
      .filter(_ == editUrl)
      .flatMap(_ => context.request.session.get(returnToAddedItemSelectUrlSessionKey))
      .orElse(defaultBackLink)

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
    context.getJourneyData.calculatorResponse.fold(
      logAndRedirect(
        "[ControllerHelpers][requireCalculatorResponse] Missing calculator response in journeyData! Redirecting to dashboard...",
        routes.DashboardController.showDashboard
      )
    )(block)

  def requireLimitUsage(journeyData: JourneyData)(
    block: Map[String, BigDecimal] => Future[Result]
  )(implicit context: LocalContext, hc: HeaderCarrier): Future[Result] =
    calculatorService.limitUsage(journeyData).flatMap { (response: LimitUsageResponse) =>
      response match {
        case LimitUsageSuccessResponse(r) =>
          block(r.map(x => (x._1, BigDecimal(x._2))))
        case _                            =>
          logAndRenderError("[ControllerHelpers][requireLimitUsage] Fetching limits was unsuccessful")
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
          "[ControllerHelpers][requireJourneyData] Unable to get journeyData! Starting a new session...",
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
        case None      =>
          logAndRenderError(
            s"[ControllerHelpers][requirePurchasedProductInstance] No purchasedProductInstance found in journeyData for iid: $iid!",
            NotFound
          )
      }
    }

  private def withClearWorkingInstance(block: => Future[Result])(implicit context: LocalContext): Future[Result] =
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
        logAndRenderError(
          s"[ControllerHelpers][requireProductOrCategory] Product or category not found at $path!",
          NotFound
        )
    }

  def requireProduct(path: ProductPath)(
    block: ProductTreeLeaf => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    requireProductOrCategory(path) {
      case leaf: ProductTreeLeaf => block(leaf)
      case _                     =>
        logAndRenderError(s"[ControllerHelpers][requireProduct] Product not found at $path!", NotFound)
    }

  def requireCategory(path: ProductPath)(
    block: ProductTreeBranch => Future[Result]
  )(implicit context: LocalContext): Future[Result] =
    requireProductOrCategory(path) {
      case branch: ProductTreeBranch => block(branch)
      case _                         =>
        logAndRenderError(s"[ControllerHelpers][requireCategory] Category not found at $path!", NotFound)
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
