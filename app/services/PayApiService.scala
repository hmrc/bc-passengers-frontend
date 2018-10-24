package services

import javax.inject.{Inject, Singleton}
import models.ChargeReference
import play.api.Mode.Mode
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.{Configuration, Environment}
import play.mvc.Http.Status._
import services.http.WsAllMethods
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

trait PayApiServiceResponse
case object PayApiServiceFailureResponse extends PayApiServiceResponse
case class PayApiServiceSuccessResponse(url: String) extends PayApiServiceResponse


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
  lazy val bcPassengersFrontendHost = configuration.getString("bc-passengers-frontend.host").getOrElse("")


  def requestPaymentUrl(chargeReference: ChargeReference, amountPence: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PayApiServiceResponse] = {

    val extras = Json.obj(
      "taxType" -> "pngr",
      "reference" -> chargeReference.value,
      "description" -> "Customs Declaration Payment",
      "amountInPence" -> amountPence,
      "extras"  -> Json.obj(),
      "backUrl" -> s"$bcPassengersFrontendHost/back",
      "returnUrl" -> s"$bcPassengersFrontendHost/return",
      "returnUrlFailure" -> s"$bcPassengersFrontendHost/return-fail",
      "returnUrlCancel" -> s"$bcPassengersFrontendHost/return-cancel"
    )

    wsAllMethods.POST[JsValue,HttpResponse](payApiBaseUrl+"/pay-api/payment", extras) map { r =>
      r.status match {
        case CREATED => PayApiServiceSuccessResponse((r.json \ "links" \ "nextUrl").as[JsString].value)
        case _ => PayApiServiceFailureResponse
      }
    }
  }
}
