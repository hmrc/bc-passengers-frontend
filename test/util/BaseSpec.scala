/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}

import scala.reflect.ClassTag

trait BaseSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite with BeforeAndAfterEach {
  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .build()
  implicit lazy val hc: HeaderCarrier         = HeaderCarrier(sessionId = Some(SessionId("fakesessionid")))

  private def addToken[T](fakeRequest: FakeRequest[T]): FakeRequest[T] =
    fakeRequest.withSession(SessionKeys.sessionId -> "fakesessionid")

  def injected[T](c: Class[T]): T                                      = app.injector.instanceOf(c)
  def injected[T](implicit evidence: ClassTag[T]): T                   = app.injector.instanceOf[T](evidence)

  def enhancedFakeRequest(method: String, uri: String): FakeRequest[AnyContentAsEmpty.type] = addToken(
    FakeRequest(method, uri)
  )

  def getFormErrors(form: Form[_]): Set[(String, String)] = form.errors.map(error => error.key -> error.message).toSet

  def buildExpectedFormErrors(items: (String, String)*): Set[(String, String)] = items.toSet

}
