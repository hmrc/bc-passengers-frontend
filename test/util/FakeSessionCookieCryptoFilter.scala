/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.stream.Materializer
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.{CookieHeaderEncoding, RequestHeader, Result, SessionCookieBaker}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeSessionCookieCryptoFilter @Inject()(val mat: Materializer, val ec: ExecutionContext) extends SessionCookieCryptoFilter {

  override protected def encrypter: Encrypter = mock[Encrypter]
  override protected def decrypter: Decrypter = mock[Decrypter]
  override protected def sessionBaker: SessionCookieBaker = mock[SessionCookieBaker]
  override protected def cookieHeaderEncoding: CookieHeaderEncoding = mock[CookieHeaderEncoding]

  override def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = next(rh)
}
