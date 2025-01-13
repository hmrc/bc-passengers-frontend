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

package controllers

import config.AppConfig
import connectors.Cache
import org.mockito.Mockito.*
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import services.{CalculatorService, ProductTreeService}
import util.BaseSpec
import views.html.errorTemplate

import scala.concurrent.Future

class ControllerHelpersSpec extends BaseSpec with ControllerHelpers {

  def cache: Cache                           = mock(classOf[Cache])
  def productTreeService: ProductTreeService = mock(classOf[ProductTreeService])
  def calculatorService: CalculatorService   = mock(classOf[CalculatorService])
  def errorTemplate: errorTemplate           = mock(classOf[errorTemplate])

  given appConfig: AppConfig                                       = mock(classOf[AppConfig])
  protected def controllerComponents: MessagesControllerComponents = mock(classOf[MessagesControllerComponents])

  "ControllerHelpers" when {
    ".logAndRedirect" should {
      "redirect to the correct location" in {
        val result: Future[Result] = logAndRedirect(
          logMessage = "Unable to get journeyData! Starting a new session...",
          redirectLocation = routes.TravelDetailsController.newSession
        )

        result.map(_ shouldBe routes.TravelDetailsController.newSession)
      }
    }

    ".requireJourneyData" should {
      "redirect to the correct location" in {
        implicit val localContext: LocalContext = LocalContext(
          request = FakeRequest(),
          sessionId = "sessionId"
        )

        val result: Future[Result] = requireJourneyData(_ => Future.successful(Ok))

        result.map(_ shouldBe routes.TravelDetailsController.newSession)
      }
    }
  }
}
