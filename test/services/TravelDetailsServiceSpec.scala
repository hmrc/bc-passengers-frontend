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


  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[LocalSessionCache].toInstance(MockitoSugar.mock[LocalSessionCache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[LocalSessionCache])
  }


  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val travelDetailsService = {
      val service = app.injector.instanceOf[TravelDetailsService]
      val mock = service.localSessionCache
      when(mock.fetchAndGetJourneyData(any())) thenReturn Future.successful( journeyDataInCache )
      when(mock.cacheJourneyData(any())(any())) thenReturn Future.successful( CacheMap("fakeid", Map.empty) )
      service
    }

    lazy val localSessionCacheMock = travelDetailsService.localSessionCache

  }

  "Calling storeCountry" should {

    "store the country in keystore when no journey data there currently" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeCountry("Egypt"))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(Some("Egypt"), None, None, None)) )(any())
    }

    "store the country in keystore, clearing subsequent journey data when journey data exists" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("Australia"), Some(true), Some(false), None) )

      await(travelDetailsService.storeCountry("Egypt"))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(Some("Egypt"), None, None, None)) )(any())
    }
  }

  "Calling storeAgeOver17" should {

    "store age confirmation in keystore when no journey data there currently" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeAgeOver17(true))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(None, Some(true), None, None)) )(any())
    }

    "store age confirmation in keystore, clearing subsequent journey data when journey data exists" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("Australia"), Some(false), Some(false), None) )

      await(travelDetailsService.storeAgeOver17(true))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(Some("Australia"), Some(true), None, None)) )(any())
    }
  }

  "Calling storePrivateCraft" should {

    "store private craft setting in keystore when no journey data there currently" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storePrivateCraft(false))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(None, None, Some(false), None)) )(any())
    }

    "store private craft setting in keystore when journey data does exist there currently" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("Australia"), Some(false), Some(false)) )

      await(travelDetailsService.storePrivateCraft(true))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(Some("Australia"), Some(false), Some(true), None)) )(any())
    }
  }

  "Calling addSelectedProducts" should {

    "add selected products in keystore when no journey data there currently" in new LocalSetup {

      override val journeyDataInCache = None

      val selectedProducts = List(List("tobacco", "cigarettes"), List("tobacco", "cigars"))

      await(travelDetailsService.addSelectedProducts(selectedProducts))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(None, None, None, Some(List(List("tobacco", "cigarettes"), List("tobacco", "cigars"))) )) )(any())
    }

    "add selected products setting in keystore when journey data does exist there currently" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("Australia"), Some(false), Some(false), None) )

      val selectedProducts = List(List("tobacco", "cigarettes"), List("tobacco", "cigars"))

      await(travelDetailsService.addSelectedProducts(selectedProducts))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(Some("Australia"), Some(false), Some(false), Some(List(List("tobacco", "cigarettes"), List("tobacco", "cigars"))) )) )(any())
    }

    "when products already exist store selected products at the start of a combined list in keystore" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("Australia"), Some(false), Some(false), Some(List(List("tobacco", "cigarettes"), List("tobacco", "cigars"))) ))

      val selectedProducts = List(List("alcohol", "beer"))

      await(travelDetailsService.addSelectedProducts(selectedProducts))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(Some("Australia"), Some(false), Some(false), Some(List(List("alcohol", "beer"), List("tobacco", "cigarettes"), List("tobacco", "cigars"))) )) )(any())
    }

  }

  "Calling removeSelectedProduct" should {

    "remove the first selected product and update keystore" in new LocalSetup {

      override val journeyDataInCache = Some(JourneyData(None, None, None, Some(List(List("tobacco", "cigarettes"), List("tobacco", "cigars")))))

      await(travelDetailsService.removeSelectedProduct())

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(None, None, None, Some(List(List("tobacco", "cigars"))) )) )(any())
    }
  }


  "Calling fetchAndGetJourneyData" should {

    "fetch the journey data from keystore" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.getUserInputData)

      verify(localSessionCacheMock, times(1)).fetchAndGetJourneyData(any())
    }
  }
}