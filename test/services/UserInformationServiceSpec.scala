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

package services

import connectors.Cache
import models.{JourneyData, UserInformation}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import util.{BaseSpec, parseLocalDate, parseLocalTime}

import scala.concurrent.Future

class UserInformationServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .build()

  override def beforeEach(): Unit =
    reset(app.injector.instanceOf[Cache])

  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val s: UserInformationService = {
      val service = app.injector.instanceOf[UserInformationService]
      val mock    = service.cache
      when(mock.fetch(any())) thenReturn Future.successful(journeyDataInCache)
      when(mock.store(any())(any())) thenReturn Future.successful(JourneyData())
      service
    }
  }

  "Calling UserInformationService.storeUserInformation" should {

    "store a new user information" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(
        s.storeUserInformation(
          JourneyData(),
          UserInformation(
            "Harry",
            "Potter",
            "passport",
            "SX12345",
            "abc@gmail.com",
            "Newcastle Airport",
            "",
            parseLocalDate("2018-08-31"),
            parseLocalTime("12:20 pm")
          )
        )
      )

      verify(s.cache, times(1)).store(
        meq(
          JourneyData(userInformation =
            Some(
              UserInformation(
                "Harry",
                "Potter",
                "passport",
                "SX12345",
                "abc@gmail.com",
                "Newcastle Airport",
                "",
                parseLocalDate("2018-08-31"),
                parseLocalTime("12:20 pm")
              )
            )
          )
        )
      )(any())

    }

  }

}
