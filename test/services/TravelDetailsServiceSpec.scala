package services



import models.JourneyData
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import util.BaseSpec
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future


class TravelDetailsServiceSpec extends BaseSpec {

  val localSessionCacheMock = MockitoSugar.mock[LocalSessionCache]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[LocalSessionCache].toInstance(localSessionCacheMock))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[LocalSessionCache])
  }

  lazy val travelDetailsService = app.injector.instanceOf[TravelDetailsService]

  trait LocalSetup {
    when(localSessionCacheMock.fetchAndGetJourneyData(any())) thenReturn Future.successful( None )
    when(localSessionCacheMock.cacheJourneyData(any())(any())) thenReturn Future.successful( CacheMap("fakeid", Map.empty) )
  }

  "Calling storeCountry" should {

    "store the country in keystore" in new LocalSetup {

      await(travelDetailsService.storeCountry("Egypt"))

      verify(localSessionCacheMock, times(1)).cacheJourneyData(any())(any())
    }
  }

  "Calling storeAgeOver17" should {

    "store age confirmation in keystore" in new LocalSetup {

      await(travelDetailsService.storeAgeOver17(true))

      verify(localSessionCacheMock, times(1)).cacheJourneyData(any())(any())
    }
  }

  "Calling storePrivateCraft" should {

    "store private craft setting in keystore" in new LocalSetup {

      await(travelDetailsService.storePrivateCraft(false))

      verify(localSessionCacheMock, times(1)).cacheJourneyData(any())(any())
    }
  }

  "Calling fetchAndGetJourneyData" should {

    "fetch the journey data from keystore" in new LocalSetup {

      await(travelDetailsService.getUserInputData)

      verify(localSessionCacheMock, times(1)).fetchAndGetJourneyData(any())
    }
  }
}
