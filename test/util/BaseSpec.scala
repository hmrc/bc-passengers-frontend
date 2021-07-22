/*
 * Copyright 2021 HM Revenue & Customs
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

package util

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.reflect.ClassTag


trait BaseSpec extends WordSpecLike with Matchers with GuiceOneAppPerSuite with BeforeAndAfterEach {
  override implicit lazy val app : Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository])).build()
  implicit lazy val hc = HeaderCarrier(sessionId = Some(SessionId("fakesessionid")))

  private def addToken[T](fakeRequest: FakeRequest[T])(implicit app: Application) = {
    val csrfConfig     = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter     = app.injector.instanceOf[CSRFFilter]
    val token          = csrfFilter.tokenProvider.generateToken

    fakeRequest.withSession(SessionKeys.sessionId -> "fakesessionid")

//    (tags = fakeRequest.tags ++ Map(
//      Token.NameRequestTag  -> csrfConfig.tokenName,
//      Token.RequestTag      -> token
//    )).withHeaders((csrfConfig.headerName, token))
  }

  def injected[T](c: Class[T]): T = app.injector.instanceOf(c)
  def injected[T](implicit evidence: ClassTag[T]) = app.injector.instanceOf[T]


  def EnhancedFakeRequest(method: String, uri: String)(implicit app: Application) = addToken(FakeRequest(method, uri))
}
