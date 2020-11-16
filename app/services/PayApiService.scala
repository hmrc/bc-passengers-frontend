/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services


import controllers.routes
import javax.inject.{Inject, Singleton}
import models.{CalculatorResponse, ChargeReference, UserInformation}
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.api.Configuration
import play.api.i18n.Messages
import play.api.libs.json._
import play.mvc.Http.Status._
import services.http.WsAllMethods
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PayApiService @Inject()(
  val wsAllMethods: WsAllMethods,
  configuration: Configuration,
  servicesConfig: ServicesConfig,
  implicit val ec: ExecutionContext
) {


  lazy val payApiBaseUrl: String = servicesConfig.baseUrl("pay-api")

  lazy val returnUrl: String = configuration.getOptional[String]("feedback-frontend.host").getOrElse("") + "/feedback/passengers"

  lazy val returnUrlFailed: String = configuration.getOptional[String]("bc-passengers-stride-frontend.host").getOrElse("") + routes.CalculateDeclareController.showCalculation()
  lazy val returnUrlCancelled: String = returnUrlFailed

  lazy val backUrl: String = configuration.getOptional[String]("bc-passengers-stride-frontend.host").getOrElse("") + routes.CalculateDeclareController.enterYourDetails()

  def requestPaymentUrl(chargeReference: ChargeReference, userInformation: UserInformation, calculatorResponse: CalculatorResponse, amountPence: Int)(implicit hc: HeaderCarrier, messages: Messages): Future[PayApiServiceResponse] = {

    def getPlaceOfArrival(userInfo: UserInformation) = {
      if(userInfo.selectPlaceOfArrival.isEmpty) userInfo.enterPlaceOfArrival else userInfo.selectPlaceOfArrival
    }

    val requestBody: JsObject = Json.obj(
      "chargeReference" -> chargeReference.value,
      "taxToPayInPence" -> amountPence,
      "dateOfArrival" -> userInformation.dateOfArrival.toDateTime(userInformation.timeOfArrival).toString("yyyy-MM-dd'T'HH:mm:ss"),
      "passengerName" -> s"${userInformation.firstName} ${userInformation.lastName}",
      "placeOfArrival" -> getPlaceOfArrival(userInformation),
      "returnUrl" -> returnUrl,
      "returnUrlFailed" -> returnUrlFailed,
      "returnUrlCancelled" -> returnUrlCancelled,
      "backUrl" -> backUrl,
      "items" -> JsArray(calculatorResponse.getItemsWithTaxToPay.map { item =>
        Json.obj(
          "name" -> item.metadata.description,
          "costInGbp" -> item.calculation.allTax,
          "price" -> s"${item.metadata.cost} ${messages(item.metadata.currency.displayName)}",
          "purchaseLocation" -> messages(item.metadata.country.countryName)
        )
      }),
      "taxBreakdown" -> Json.obj(
        "customsInGbp" -> calculatorResponse.calculation.customs,
        "exciseInGbp"-> calculatorResponse.calculation.excise,
        "vatInGbp"-> calculatorResponse.calculation.vat
      )
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
