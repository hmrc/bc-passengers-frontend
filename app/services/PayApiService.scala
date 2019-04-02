package services


import connectors.Cache
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
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PayApiService @Inject()(
  cache: Cache,
  val wsAllMethods: WsAllMethods,
  configuration: Configuration,
  environment: Environment,
  productTreeService: ProductTreeService,
  currencyService: CurrencyService,
  servicesConfig: ServicesConfig,
  implicit val ec: ExecutionContext
) {


  lazy val payApiBaseUrl = servicesConfig.baseUrl("pay-api")

  lazy val returnUrl = configuration.getOptional[String]("feedback-frontend.host").getOrElse("") + "/feedback/passengers"

  lazy val returnUrlFailed = configuration.getOptional[String]("bc-passengers-frontend.host").getOrElse("") + routes.DashboardController.showCalculation()
  lazy val returnUrlCancelled = returnUrlFailed

  lazy val backUrl = configuration.getOptional[String]("bc-passengers-frontend.host").getOrElse("") + routes.DeclarationController.enterYourDetails()

  def requestPaymentUrl(chargeReference: ChargeReference, userInformation: UserInformation, calculatorResponse: CalculatorResponse, amountPence: Int, receiptDateTime: DateTime)(implicit hc: HeaderCarrier): Future[PayApiServiceResponse] = {

    val requestBody = Json.obj(
      "chargeReference" -> chargeReference.value,
      "taxToPayInPence" -> amountPence,
      "dateOfArrival" -> userInformation.dateOfArrival.toDateTime(userInformation.timeOfArrival).toString("yyyy-MM-dd'T'HH:mm:ss"),
      "passengerName" -> s"${userInformation.firstName} ${userInformation.lastName}",
      "placeOfArrival" -> userInformation.placeOfArrival,
      "returnUrl" -> returnUrl,
      "returnUrlFailed" -> returnUrlFailed,
      "returnUrlCancelled" -> returnUrlCancelled,
      "backUrl" -> backUrl,
      "items" -> JsArray(calculatorResponse.getItemsWithTaxToPay.map { item =>
        Json.obj(
          "name" -> item.metadata.description,
          "costInGbp" -> item.calculation.allTax
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
