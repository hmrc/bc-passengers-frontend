package services

import models.{JourneyData, ProductPath, PurchasedProductInstance, UserInformation}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import util.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserInformationServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[LocalSessionCache].toInstance(MockitoSugar.mock[LocalSessionCache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[LocalSessionCache])
  }


  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val s = {
      val service = app.injector.instanceOf[UserInformationService]
      val mock = service.localSessionCache
      when(mock.fetchAndGetJourneyData(any())) thenReturn Future.successful( journeyDataInCache )
      when(mock.cacheJourneyData(any())(any())) thenReturn Future.successful( CacheMap("fakeid", Map.empty) )
      service
    }
  }

  "Calling UserInformationService.storeUserInformation" should {

    "store a new user information" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(s.storeUserInformation(JourneyData(), UserInformation("Harry", "Potter", "12345678", "Newcastle Airport", LocalDate.parse("2018-08-31"), LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa")))))

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(userInformation = Some(UserInformation("Harry", "Potter", "12345678", "Newcastle Airport", LocalDate.parse("2018-08-31"), LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa"))))))
      )(any())

    }

  }

}
