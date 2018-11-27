package services

import controllers.routes
import javax.inject.{Inject, Singleton}
import models.{CalculatorResponse, ChargeReference, Item, UserInformation}
import org.joda.time.DateTime
import play.api.Mode.Mode
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.api.{Configuration, Environment}
import play.mvc.Http.Status._
import services.http.WsAllMethods
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PayApiService @Inject()(
  val localSessionCache: LocalSessionCache,
  val wsAllMethods: WsAllMethods,
  configuration: Configuration,
  environment: Environment,
  productTreeService: ProductTreeService,
  currencyService: CurrencyService
) extends ServicesConfig with UsesJourneyData {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration = configuration

  lazy val payApiBaseUrl = baseUrl("pay-api")
  lazy val redirectUrl = configuration.getString("bc-passengers-frontend.host").getOrElse("") + routes.TravelDetailsController.checkDeclareGoodsStartPage().url

  def requestPaymentUrl(chargeReference: ChargeReference, userInformation: UserInformation, calculatorResponse: CalculatorResponse, amountPence: Int, receiptDateTime: DateTime)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PayApiServiceResponse] = {

    val requestBody = Json.obj(
      "chargeReference" -> chargeReference.value,
      "taxToPayInPence" -> amountPence,
      "dateOfArrival" -> (userInformation.dateOfArrival + "T00:00:00"),
      "passengerName" -> s"${userInformation.firstName} ${userInformation.lastName}",
      "placeOfArrival" -> userInformation.placeOfArrival,
      "items" -> JsArray(calculatorResponse.getItemsWithTaxToPay.map { item =>
        Json.obj(
          "name" -> item.metadata.description,
          "costInCurrency" -> item.metadata.currency.displayName,
          "costInGbp" -> item.metadata.cost
        )
      })
    )

    wsAllMethods.POST[JsValue, HttpResponse](payApiBaseUrl + "/pay-api/pngr/pngr/journey/start", requestBody) map { r =>
      r.status match {
        case CREATED => PayApiServiceSuccessResponse((r.json \ "nextUrl").as[JsString].value)
        case _ => PayApiServiceFailureResponse
      }
    }

  }
}

trait PayApiServiceResponse
case object PayApiServiceFailureResponse extends PayApiServiceResponse
case class PayApiServiceSuccessResponse(url: String) extends PayApiServiceResponse