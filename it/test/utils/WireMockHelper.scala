/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest._

trait WireMockHelper extends BeforeAndAfterEach with BeforeAndAfterAll {
  self: Suite =>

  val wireMockPort: Int    = 11111
  val wireMockHost: String = "localhost"

  private val wmConfig: WireMockConfiguration = wireMockConfig().port(wireMockPort)
  private val wireMockServer: WireMockServer  = new WireMockServer(wmConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetAll()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireMockHost, wireMockPort)
    wireMockServer.start()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  def stubPost(url: String, status: Int, requestBody: String, responseBody: String): StubMapping =
    wireMockServer.stubFor(
      post(urlPathEqualTo(url))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(
          aResponse().withStatus(status).withBody(responseBody)
        )
    )
}
