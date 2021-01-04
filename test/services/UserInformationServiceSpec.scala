/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services

import connectors.Cache
import models.{JourneyData, UserInformation}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{LocalDate, LocalTime}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import util.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserInformationServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[Cache])
  }


  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val s = {
      val service = app.injector.instanceOf[UserInformationService]
      val mock = service.cache
      when(mock.fetch(any())) thenReturn Future.successful( journeyDataInCache )
      when(mock.store(any())(any())) thenReturn Future.successful( JourneyData() )
      service
    }
  }

  "Calling UserInformationService.storeUserInformation" should {

    "store a new user information" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(s.storeUserInformation(JourneyData(), UserInformation("Harry", "Potter","passport", "SX12345","abc@gmail.com", "Newcastle Airport","", LocalDate.parse("2018-08-31"), LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa")))))

      verify(s.cache, times(1)).store(
        meq(JourneyData(userInformation = Some(UserInformation("Harry", "Potter","passport", "SX12345", "abc@gmail.com", "Newcastle Airport", "",  LocalDate.parse("2018-08-31"), LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa"))))))
      )(any())

    }

  }

}
