package controllers

import config.AppConfig
import javax.inject.Inject
import models.{ConfirmRemoveDto, ProductPath}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CurrencyService, ProductTreeService, PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class AlterProductsController @Inject() (
  val travelDetailsService: TravelDetailsService,
  val purhasedProductService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  def confirmRemove(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      requirePurchasedProductInstanceDescription(product, path, iid) { description =>

        Future.successful(Ok(views.html.purchased_products.remove(ConfirmRemoveDto.form, description, path, iid)))
      }
    }
  }

  def remove(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    ConfirmRemoveDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          requirePurchasedProductInstanceDescription(product, path, iid) { description =>
            Future.successful(BadRequest(views.html.purchased_products.remove(formWithErrors, description, path, iid)))
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
