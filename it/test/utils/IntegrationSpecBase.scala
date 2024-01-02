/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package utils

import org.scalatest.concurrent._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import uk.gov.hmrc.http.HeaderCarrier

trait IntegrationSpecBase
    extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with WireMockHelper
    with GuiceOneServerPerSuite {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.tps-payments-backend.host" -> wireMockHost,
        "microservice.services.tps-payments-backend.port" -> wireMockPort
      )
      .build()
}
