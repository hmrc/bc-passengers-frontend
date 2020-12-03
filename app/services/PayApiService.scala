/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services


import controllers.{LocalContext, routes}
import models.{CalculatorResponse, ChargeReference, UserInformation}
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}
import play.mvc.Http.Status._
import services.http.WsAllMethods
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PayApiService @Inject()(
  val wsAllMethods: WsAllMethods,
  configuration: Configuration,
  servicesConfig: ServicesConfig,
  implicit val ec: ExecutionContext
) {
  lazy val tpsApiBaseUrl: String = servicesConfig.baseUrl("tps-payments-backend")
  lazy val tpsFrontendBaseUrl: String = configuration.getOptional[String]("tps-payments-frontend.host").getOrElse("")
  lazy val backUrl: String = configuration.getOptional[String]("bc-passengers-stride-frontend.host").getOrElse("") + routes.CalculateDeclareController.enterYourDetails()
  lazy val resetUrl: String = configuration.getOptional[String]("bc-passengers-stride-frontend.host").getOrElse("") + routes.TravelDetailsController.checkDeclareGoodsStartPage()
  lazy val finishUrl: String = configuration.getOptional[String]("bc-passengers-stride-frontend.host").getOrElse("") + routes.TravelDetailsController.checkDeclareGoodsStartPage()
  lazy val callbackUrl: String =  servicesConfig.baseUrl("payments-processor") + "/payments/notifications/send-card-payments"

  def requestPaymentUrl(chargeReference: ChargeReference, userInformation: UserInformation, calculatorResponse: CalculatorResponse)(implicit hc: HeaderCarrier, messages: Messages, context: LocalContext
  ): Future[PayApiServiceResponse] = {

    val paymentSpecificData = Json.obj(
      "chargeReference" -> chargeReference.value,
      "vat" -> calculatorResponse.calculation.vat,
      "customs" -> calculatorResponse.calculation.customs,
      "excise" -> calculatorResponse.calculation.excise
    )

    val payment = Json.obj(
      "chargeReference" -> chargeReference.value,
      "customerName" -> s"${userInformation.firstName} ${userInformation.lastName}",
      "amount" -> calculatorResponse.calculation.allTax,
      "taxRegimeDisplay" -> "PNGR",
      "taxType" -> "PNGR",
      "paymentSpecificData" -> paymentSpecificData
    )

    val navigation = Json.obj(
      "back" -> backUrl,
      "reset" -> resetUrl,
      "finish" -> finishUrl,
      "callback" -> callbackUrl
    )
    val json = Json.obj(
      "pid" -> context.request.providerId,
      "payments" -> Json.arr(payment),
      "navigation" -> navigation
    )

    wsAllMethods.POST[JsValue, HttpResponse](s"$tpsApiBaseUrl/tps-payments-backend/tps-payments", json) map { r =>
      Logger.debug(s"""called tps-payments-backend store with status $r.status""")
      r.status match {
        case CREATED =>
          PayApiServiceSuccessResponse(s"$tpsFrontendBaseUrl/tps-payments/make-payment/pngr/" + r.json.as[String])
        case _ => PayApiServiceFailureResponse
      }
    }
  }
}

trait PayApiServiceResponse
case object PayApiServiceFailureResponse extends PayApiServiceResponse
case class PayApiServiceSuccessResponse(url: String) extends PayApiServiceResponse
