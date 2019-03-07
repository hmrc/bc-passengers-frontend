package services

import connectors.Cache
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

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[Cache])
  }


  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val travelDetailsService = {
      val service = app.injector.instanceOf[TravelDetailsService]
      val mock = service.cache
      when(mock.fetch(any())) thenReturn Future.successful( journeyDataInCache )
      when(mock.store(any())(any())) thenReturn Future.successful( CacheMap("fakeid", Map.empty) )
      service
    }

    lazy val cacheMock = travelDetailsService.cache

  }

  "Calling storeEuCountryCheck" should {

    "store the country in keystore when no journey data there currently" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeEuCountryCheck("nonEuOnly"))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("nonEuOnly"), None, None, None, None, Nil)) )(any())
    }

    "store the country in keystore, clearing subsequent journey data when journey data exists" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("nonEuOnly"), None, None, Some(true), Some(false), Nil) )

      await(travelDetailsService.storeEuCountryCheck("both"))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, None, None, Nil)) )(any())
    }
  }

  "Calling storeAgeOver17" should {

    "store age confirmation in keystore when no journey data there currently" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeAgeOver17(ageOver17 = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, None, None, None, Some(true), Nil)) )(any())
    }

    "store age confirmation in keystore maintaining subsequent journey data" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("Australia"), None, None, Some(false), Some(false), Nil) )

      await(travelDetailsService.storeAgeOver17(ageOver17 = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("Australia"), None, None, Some(false), Some(true), Nil)) )(any())
    }
  }

  "Calling storePrivateCraft" should {

    "store private craft setting in keystore when no journey data is currently there" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storePrivateCraft(privateCraft = false))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, None, None, Some(false), None, Nil)) )(any())
    }

    "store private craft setting in keystore when journey data does exist " in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("Australia"), None, None, Some(false), Some(false), List(List("someProduct"))) )

      await(travelDetailsService.storePrivateCraft(privateCraft = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("Australia"), None, None, Some(true), Some(false), List(List("someProduct")))) )(any())
    }
  }


  "Calling fetchAndGetJourneyData" should {

    "fetch the journey data from keystore" in new LocalSetup {

      override val journeyDataInCache = None

      await(cacheMock.fetch)

      verify(cacheMock, times(1)).fetch(any())
    }
  }
}
