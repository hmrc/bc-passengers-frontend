/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package connectors

import constants.TpsPaymentConnectorConstants._
import models._
import play.api.http.Status._
import utils.IntegrationSpecBase

class TpsPaymentConnectorISpec extends IntegrationSpecBase {

  private val tpsPaymentBackendUrl: String = "/tps-payments-backend/start-tps-journey/pngr"

  private val tpsPaymentConnector: TpsPaymentConnector = app.injector.instanceOf[TpsPaymentConnector]

  "TpsPaymentConnector" when {
    "calling the POST - /tps-payments-backend/start-tps-journey/pngr endpoint" should {
      "return PayApiSuccessResponse when tps-payments-backend returns 201 CREATED" in {
        stubPost(tpsPaymentBackendUrl, CREATED, tpsPaymentRequestBody, tpsPaymentSuccessResponseBody)

        val result: TpsResponse = tpsPaymentConnector
          .requestPaymentUrl(
            chargeReference = chargeReference,
            userInformation = userInformation,
            calculation = calculation,
            isAmendment = false
          )
          .futureValue

        result mustBe PayApiSuccessResponse(
          "SomeFakeJourneyId",
          "http://localhost:9124/tps-payments/make-payment/pngr/SomeFakeJourneyId"
        )
      }

      "return PayApiFailureResponse when tps-payments-backend returns 500 INTERNAL_SERVER_ERROR" in {
        stubPost(tpsPaymentBackendUrl, INTERNAL_SERVER_ERROR, tpsPaymentRequestBody, tpsPaymentFailureResponseBody)

        val result: TpsResponse = tpsPaymentConnector
          .requestPaymentUrl(
            chargeReference = chargeReference,
            userInformation = userInformation,
            calculation = calculation,
            isAmendment = false
          )
          .futureValue

        result mustBe PayApiFailureResponse
      }
    }
  }
}
