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

package filters

import org.apache.pekko.stream.Materializer
import org.mockito.MockitoSugar
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.BaseSpec
import scala.concurrent.Future

class DisableBrowserCacheFilterSpec extends BaseSpec with MockitoSugar {

  implicit val mockMaterializer: Materializer = mock[Materializer]

  private val disableBrowserCacheFilter: DisableBrowserCacheFilter = new DisableBrowserCacheFilter()

  "DisableBrowserCacheFilter" should {
    "apply a Cache-Control header to request" in {
      val result = disableBrowserCacheFilter.apply(_ => Future.successful(Ok))(FakeRequest())

      headers(result) shouldBe Map(
        "Cache-Control" -> "no-cache, no-store, must-revalidate, max-age=0",
        "Pragma"        -> "no-cache",
        "Expires"       -> "0"
      )
    }
  }
}
