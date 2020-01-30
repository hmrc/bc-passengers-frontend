package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.DashboardAction
import javax.inject.Inject
import models.{OtherGoodsSearchDto, OtherGoodsSearchItem, ProductAlias}
import play.api.data.Form
import play.api.data.Forms.{mapping, number, optional, text}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CalculatorService, ProductTreeService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class OtherGoodsSearchController @Inject()(
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val calculatorService: CalculatorService,

  dashboardAction: DashboardAction,

  val error_template: views.html.error_template,
  val other_goods_search: views.html.other_goods.other_goods_search,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext

) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def otherGoodsSearchForm(otherGoodsSearchItems: List[OtherGoodsSearchItem] = Nil)(implicit context: LocalContext): Form[OtherGoodsSearchDto] = Form(
    mapping(
      "searchTerm" -> optional(text).verifying("error.other_goods_search", _ => {

        context.request.body.asFormUrlEncoded.fold(false) { body =>

          val data = for(t <- List("searchTerm", "remove", "action")) yield body.get(t).map(_.mkString).mkString
          data.exists(t => !t.isEmpty)
        }
      }).verifying("error.other_goods_search", _.fold(true) { term =>
          !otherGoodsSearchItems.filter(_.name == term).isEmpty
        }
      ).transform[Option[OtherGoodsSearchItem]](_.flatMap(term => otherGoodsSearchItems.filter(_.name == term).headOption), _.map(_.name)),
      "remove" -> optional(number),
      "action" -> optional(text).verifying("error.add", _ match {
        case Some("add") =>

          context.request.body.asFormUrlEncoded.fold(false) { body =>

            !body.get("searchTerm").map(_.mkString).mkString.isEmpty
          }
        case _ => true
      })
    )(OtherGoodsSearchDto.apply)(OtherGoodsSearchDto.unapply)
  )


  val clearAndSearchGoods: Action[AnyContent] = dashboardAction { implicit context =>

    val journeyData = context.getJourneyData.copy(selectedAliases = Nil)

    cache.storeJourneyData(journeyData).map(_ => Redirect(routes.OtherGoodsSearchController.searchGoods()))
  }

  val searchGoods: Action[AnyContent] = dashboardAction { implicit context =>

    Future.successful {

      Ok(other_goods_search(otherGoodsSearchForm(), productTreeService.otherGoodsSearchItems, context.getJourneyData.selectedAliases))
    }
  }

  val processSearchGoods: Action[AnyContent] = dashboardAction { implicit context =>

    val otherGoodsSearchItems: List[OtherGoodsSearchItem] = productTreeService.otherGoodsSearchItems

    otherGoodsSearchForm(otherGoodsSearchItems).bindFromRequest.fold(
      formWithErrors => {
        Future.successful {
          BadRequest(other_goods_search(formWithErrors, otherGoodsSearchItems, context.getJourneyData.selectedAliases))
        }
      },
      otherGoodsSearchDto =>  {

        otherGoodsSearchDto match {

          case OtherGoodsSearchDto(_, Some(removeIndex), _) =>

            val updatedJourneyData = context.getJourneyData.copy(selectedAliases = context.getJourneyData.selectedAliases.patch(removeIndex, Nil, 1))

            cache.storeJourneyData(updatedJourneyData) map { _ =>
              Redirect(controllers.routes.OtherGoodsSearchController.searchGoods())
            }

          case OtherGoodsSearchDto(_, _, Some("continue")) =>

            Future.successful {
              Redirect(routes.SelectProductController.nextStep())
            }

          case OtherGoodsSearchDto(Some(otherGoodsSearchItem), _, _) =>

            val productAlias = ProductAlias(otherGoodsSearchItem.name, otherGoodsSearchItem.path)

            val updatedJourneyData = context.getJourneyData.copy(selectedAliases = (context.getJourneyData.selectedAliases :+ productAlias).take(appConfig.maxOtherGoods))

            cache.storeJourneyData(updatedJourneyData) map { _ =>

              Redirect(controllers.routes.OtherGoodsSearchController.searchGoods())
            }

        }
      }
    )
  }
}