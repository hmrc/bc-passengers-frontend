/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.LocalContext
import models.UserInformation.getPreUser
import models.YourJourneyDetailsDto.toArrivalForm
import models.{ArrivalForm, DateTimeOfArrival, IdentificationForm, JourneyData, PlaceOfArrival, PreUserInformation, UserInformation, WhatIsYourNameForm, YourJourneyDetailsDto}
import org.mockito.ArgumentMatchers.{eq as meq, *}
import org.mockito.Mockito.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.MongoComponent
import util.{BaseSpec, parseLocalDate, parseLocalTime}

import java.time.{LocalDate, LocalTime}
import scala.concurrent.Future

class PreUserInformationServiceSpec extends BaseSpec {

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .build()

  override def beforeEach(): Unit =
    reset(app.injector.instanceOf[Cache])

  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val s: PreUserInformationService = {
      val service = app.injector.instanceOf[PreUserInformationService]
      val mock    = service.cache
      when(mock.fetch(any())).thenReturn(Future.successful(journeyDataInCache))
      when(mock.store(any())(any())).thenReturn(Future.successful(JourneyData()))
      service
    }
  }

  "Calling PreUserInformationService.storeUserInformation" should {

    "store a new user information" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(
        s.storePreUserInformation(
          JourneyData(),
          Option(
            getPreUser(
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
      )

      verify(s.cache, times(1)).store(
        meq(
          JourneyData(preUserInformation =
            Some(
              getPreUser(
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
        )
      )(any())

    }

  }

  "Calling PreUserInformationService.storeCompleteUserInformation" should {

    "store complete new user information" in new LocalSetup {

      val travelDetailsJourneyData: JourneyData = JourneyData(
        prevDeclaration = Some(false),
        euCountryCheck = Some("nonEuOnly"),
        arrivingNICheck = Some(true),
        isVatResClaimed = None,
        isBringingDutyFree = None,
        bringingOverAllowance = Some(true),
        ageOver17 = Some(true),
        privateCraft = Some(false)
      )

      implicit val localContext: LocalContext = LocalContext(
        request = FakeRequest(),
        sessionId = "sessionId",
        journeyData = Some(
          JourneyData(
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            List(),
            List(),
            None,
            Some(
              PreUserInformation(
                WhatIsYourNameForm("Harry", "Potter"),
                Some(IdentificationForm("passport", Some("SX12345"))),
                Some("abc@gmail.com"),
                Some(
                  ArrivalForm(
                    "Newcastle Airport",
                    "",
                    parseLocalDate("2018-08-31"),
                    parseLocalTime("12:20 pm")
                  )
                )
              )
            )
          )
        )
      )

      override def journeyDataInCache: Option[JourneyData] = None

      private val declarationTime: LocalTime =
        parseLocalTime("09:15 pm")

      private val declarationDate: LocalDate =
        parseLocalDate("2019-05-13")

      val dto: YourJourneyDetailsDto = YourJourneyDetailsDto(
        PlaceOfArrival(Some("UK"), Some("London")),
        DateTimeOfArrival(s"$declarationDate", s"$declarationTime")
      )

      val updatedPreUserInformation: Option[PreUserInformation] =
        travelDetailsJourneyData.preUserInformation.map(_.copy(arrivalForm = Option(toArrivalForm(dto))))

      await(
        s.storeCompleteUserInformation(
          JourneyData(),
          dto
        )
      )

      verify(s.cache, times(1)).store(
        meq(
          JourneyData(preUserInformation =
            Some(
              getPreUser(
                UserInformation(
                  "Harry",
                  "Potter",
                  "passport",
                  "SX12345",
                  "abc@gmail.com",
                  "UK",
                  "London",
                  parseLocalDate("2019-05-13"),
                  parseLocalTime("09:15 pm")
                )
              )
            )
          )
        )
      )(any())
    }
  }

}
