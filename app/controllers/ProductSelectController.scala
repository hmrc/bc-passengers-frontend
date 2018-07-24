package controllers

import config.AppConfig
import javax.inject.Inject
import models.{JourneyData, SelectProductsDto}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Result}
import services.ProductsService.{Branch, Leaf}
import services.{ProductsService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class ProductSelectController @Inject() (
  val messagesApi: MessagesApi,
  implicit val appConfig: AppConfig,
  val travelDetailsService: TravelDetailsService
) extends FrontendController with I18nSupport with PublicActions {


  def selectProducts(path: String): Action[AnyContent] = PublicAction { implicit request =>

    Future.successful {
      ProductsService.getProducts.getDescendant(path.split("/")) match {
        case Some(Branch(_, _, children)) =>
          Ok(views.html.passengers.select_products(SelectProductsDto.form, children.map( i => ( i.name, i.token ) ), path))
        case Some(Leaf(_, name, _, templateID)) =>
          Ok(views.html.passengers.product_details(name, templateID))
        case None => NotFound(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem."))
      }

    }
  }

  def processSelectedProducts(path: String): Action[AnyContent] = PublicAction { implicit request =>

    SelectProductsDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful {
          ProductsService.getProducts.getDescendant(path.split("/")) match {
            case Some(Branch(_, _, children)) =>
              BadRequest(views.html.passengers.select_products(formWithErrors, children.map(i => (i.name, i.token)), path))
            case _ => NotFound(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem."))
          }
        }
      },
      selectProductsDto => {

        val productPaths = selectProductsDto.tokens map { token =>
          val fullPath = path + "/" + token
          fullPath.split("/").toList
        }

        travelDetailsService.addSelectedProducts( productPaths ) map { _ =>
          Redirect(routes.ProductSelectController.nextStep())
        }
      }
    )
  }

  def nextStep(): Action[AnyContent] = PublicAction { implicit request =>

    travelDetailsService.getUserInputData flatMap {
      case Some(journeyData) => {
        journeyData.selectedProducts match {
          case Some(List()) => Future.successful { Redirect(routes.TravelDetailsController.productDashboard()) }
          case Some(ls:List[List[String]]) => {
            travelDetailsService.removeSelectedProduct() map { _ =>
              val path = ls.head mkString "/"
              ProductsService.getProducts.getDescendant(ls.head) match {
                case Some(_) => Redirect(routes.ProductSelectController.selectProducts(path))
                case None    => NotFound(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem."))
              }
            }
          }
          case None => Future.successful {
            NotFound(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem."))}
        }
      }
      case None =>  Future.successful {
        NotFound(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem."))}
    }
  }

}
