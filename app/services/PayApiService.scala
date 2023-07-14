/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import controllers.routes
import javax.inject.{Inject, Singleton}
import models.{CalculatorResponse, ChargeReference, Country, UserInformation}
import play.api.Configuration
import play.api.i18n.Messages
import play.api.libs.json._
import play.mvc.Http.Status._
import services.http.WsAllMethods
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PayApiService @Inject() (
  val wsAllMethods: WsAllMethods,
  configuration: Configuration,
  servicesConfig: ServicesConfig,
  val countriesService: CountriesService,
  implicit val ec: ExecutionContext
) {

  private lazy val payApiBaseUrl: String = servicesConfig.baseUrl("pay-api")

  lazy val returnUrl: String =
    configuration.getOptional[String]("feedback-frontend.host").getOrElse("") + "/feedback/passengers"

  lazy val returnUrlFailed: String    = configuration
    .getOptional[String]("bc-passengers-frontend.host")
    .getOrElse("") + routes.CalculateDeclareController.showCalculation
  lazy val returnUrlCancelled: String = returnUrlFailed

  private lazy val backUrlDeclaration: String    = configuration
    .getOptional[String]("bc-passengers-frontend.host")
    .getOrElse("") + routes.CalculateDeclareController.enterYourDetails
  private lazy val backUrlAmendment: String      = configuration
    .getOptional[String]("bc-passengers-frontend.host")
    .getOrElse("") + routes.CalculateDeclareController.declareYourGoods
  private lazy val backUrlPendingPayment: String = configuration
    .getOptional[String]("bc-passengers-frontend.host")
    .getOrElse("") + routes.PendingPaymentController.loadPendingPaymentPage

  def requestPaymentUrl(
    chargeReference: ChargeReference,
    userInformation: UserInformation,
    calculatorResponse: CalculatorResponse,
    amountPence: Int,
    isAmendment: Boolean,
    amountPaidPreviously: Option[String],
    amendState: Option[String] = None
  )(implicit hc: HeaderCarrier, messages: Messages): Future[PayApiServiceResponse] = {

    def getPlaceOfArrival(userInfo: UserInformation): String =
      if (userInfo.selectPlaceOfArrival.isEmpty) userInfo.enterPlaceOfArrival else userInfo.selectPlaceOfArrival

    def formatYesNo(customValue: Boolean, country: Option[Country])(implicit messages: Messages): String =
      if (countriesService.isInEu(country.map(_.code).getOrElse(""))) {
        if (customValue) {
          messages("label.yes")
        } else {
          messages("label.no")
        }
      } else {
        messages("label.not_required")
      }

    def geBackURL(isAmendment: Boolean, amendState: String): String =
      if (amendState.equals("pending-payment")) { backUrlPendingPayment }
      else if (isAmendment) { backUrlAmendment }
      else { backUrlDeclaration }

    def previouslyPaidAmount: String = amountPaidPreviously.getOrElse("0.00")

    val requestBody: JsObject = Json.obj(
      "chargeReference"    -> chargeReference.value,
      "taxToPayInPence"    -> amountPence,
      "dateOfArrival"      -> userInformation.dateOfArrival
        .toDateTime(userInformation.timeOfArrival)
        .toString("yyyy-MM-dd'T'HH:mm:ss"),
      "passengerName"      -> s"${userInformation.firstName} ${userInformation.lastName}",
      "placeOfArrival"     -> getPlaceOfArrival(userInformation),
      "returnUrl"          -> returnUrl,
      "returnUrlFailed"    -> returnUrlFailed,
      "returnUrlCancelled" -> returnUrlCancelled,
      "backUrl"            -> geBackURL(isAmendment, amendState.getOrElse("")),
      "items"              -> JsArray(calculatorResponse.getItemsWithTaxToPay.map { item =>
        Json.obj(
          "name"             -> item.metadata.description,
          "costInGbp"        -> item.calculation.allTax,
          "price"            -> s"${item.metadata.cost} ${messages(item.metadata.currency.displayName)}",
          "purchaseLocation" -> messages(item.metadata.country.countryName),
          "producedIn"       -> messages(item.metadata.originCountry.map(_.countryName).getOrElse(messages("label.na"))),
          "evidenceOfOrigin" -> formatYesNo(item.isCustomPaid.getOrElse(false), item.metadata.originCountry)
        )
      }),
      "taxBreakdown"       -> Json.obj(
        "customsInGbp" -> calculatorResponse.calculation.customs,
        "exciseInGbp"  -> calculatorResponse.calculation.excise,
        "vatInGbp"     -> calculatorResponse.calculation.vat
      )
    ) ++ (if (isAmendment) {
            Json.obj(
              "totalPaidNow"         -> BigDecimal(amountPence.toDouble / 100).setScale(2).toString,
              "amountPaidPreviously" -> previouslyPaidAmount
            )
          } else {
            Json.obj()
          })

    wsAllMethods.POST[JsValue, HttpResponse](payApiBaseUrl + "/pay-api/pngr/pngr/journey/start", requestBody) map { r =>
      r.status match {
        case CREATED => PayApiServiceSuccessResponse((r.json \ "nextUrl").as[JsString].value)
        case _       => PayApiServiceFailureResponse
      }
    }

  }
}

trait PayApiServiceResponse
case object PayApiServiceFailureResponse extends PayApiServiceResponse
case class PayApiServiceSuccessResponse(url: String) extends PayApiServiceResponse
