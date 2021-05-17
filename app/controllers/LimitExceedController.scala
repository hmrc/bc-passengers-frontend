/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.LimitExceedAction
import javax.inject.Inject
import models.ProductPath
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CalculatorService, ProductTreeService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class LimitExceedController @Inject()(
                                      val cache: Cache,
                                      val productTreeService: ProductTreeService,
                                      val calculatorService: CalculatorService,
                                      limitExceedAction: LimitExceedAction,
                                      val error_template: views.html.error_template,
                                      val limitExceedPage: views.html.purchased_products.limit_exceed,
                                      override val controllerComponents: MessagesControllerComponents,
                                      implicit val appConfig: AppConfig,
                                      val backLinkModel: BackLinkModel,
                                      implicit val ec: ExecutionContext
                                    ) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def loadLimitExceedPage(path: ProductPath): Action[AnyContent] = limitExceedAction { implicit context =>
    requireProduct(path) { product =>
      Future.successful(Ok(limitExceedPage(product.name, s"heading.${product.applicableLimits.last.toLowerCase}.limit-exceeded")))
    }
  }
}
