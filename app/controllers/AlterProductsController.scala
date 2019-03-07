package controllers

import config.AppConfig
import connectors.Cache
import javax.inject.Inject
import models.{ConfirmRemoveDto, ProductPath}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerComponents, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class AlterProductsController @Inject() (
  val cache: Cache,
  val calculatorService: CalculatorService,
  val purhasedProductService: PurchasedProductService,
  val currencyService: CurrencyService,
  val countriesService: CountriesService,
  val productTreeService: ProductTreeService,

  val remove: views.html.purchased_products.remove,
  val error_template: views.html.error_template,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def confirmRemove(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      requirePurchasedProductInstanceDescription(product, path, iid) { description =>

        Future.successful(Ok(remove(ConfirmRemoveDto.form, description, path, iid)))
      }
    }
  }

  def remove(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    ConfirmRemoveDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          requirePurchasedProductInstanceDescription(product, path, iid) { description =>
            Future.successful(BadRequest(remove(formWithErrors, description, path, iid)))
          }
        }
      },
      confirmRemoveDto => {
        if (confirmRemoveDto.confirmRemove) {
          purhasedProductService.removePurchasedProductInstance(context.getJourneyData, path, iid) map { _ =>
            Redirect(routes.DashboardController.showDashboard())
          }
        }
        else {
          Future.successful(Redirect(routes.DashboardController.showDashboard()))
        }
      }
    )
  }
}
