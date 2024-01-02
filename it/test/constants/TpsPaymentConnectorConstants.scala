/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package constants

import models._
import util.{parseLocalDate, parseLocalTime}

object TpsPaymentConnectorConstants {

  val chargeReference: ChargeReference = ChargeReference("XYPRRVWV52PVDI")

  val userInformation: UserInformation =
    UserInformation(
      firstName = "Harry",
      lastName = "Potter",
      identificationType = "passport",
      identificationNumber = "SX12345",
      emailAddress = "abc@gmail.com",
      selectPlaceOfArrival = "LHR",
      enterPlaceOfArrival = "",
      dateOfArrival = parseLocalDate("2018-11-12"),
      timeOfArrival = parseLocalTime("12:20 pm")
    )

  val calculation: Calculation = Calculation("102.54", "534.89", "725.03", "1362.46")

  val tpsPaymentRequestBody: String =
    s"""
       |{
       |    "chargeReference": "${chargeReference.value}",
       |    "customerName": "${userInformation.firstName} ${userInformation.lastName}",
       |    "amount": "${calculation.allTax}",
       |    "finishUrl": "http://localhost:9083/check-tax-on-goods-you-bring-into-the-uk/declaration-complete",
       |    "resetUrl": "http://localhost:9083/check-tax-on-goods-you-bring-into-the-uk",
       |    "backUrl": "http://localhost:9083/check-tax-on-goods-you-bring-into-the-uk/user-information"
       |}
    """.stripMargin

  val tpsPaymentSuccessResponseBody: String =
    """
      |{
      |    "journeyId": "SomeFakeJourneyId",
      |    "nextUrl": "http://localhost:9124/tps-payments/make-payment/pngr/SomeFakeJourneyId"
      |}
    """.stripMargin

  val tpsPaymentFailureResponseBody: String =
    """
      |{
      |    "statusCode": 500,
      |    "message": "Internal Server Error"
      |}
    """.stripMargin
}
