package services

import connectors.Cache
import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
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

    val dummyPpi = List(PurchasedProductInstance(ProductPath("path"), "iid", Some(1.0), Some(100), Some(Country("AU", "Australia", "A2", false, Nil)), Some("AUD"), Some(100.25)))
    val dummySelectedProducts = List(List("some product"), List("some other product"))

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

    "store the eu country check in keystore when no journey data there currently" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeEuCountryCheck("nonEuOnly"))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("nonEuOnly"), None, None, None, None, None, None, Nil, Nil)) )(any())
    }

    "store the eu country check in keystore, keeping subsequent journey data the same if the answer has not changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, None, None, Some(true), Some(false), None, dummySelectedProducts, dummyPpi) )

      await(travelDetailsService.storeEuCountryCheck("both"))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, None, Some(true), Some(false), None, dummySelectedProducts, dummyPpi)) )(any())
    }

    "store the eu country check in keystore when journey data does exist, setting subsequent journey data to None if the answer has changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), Some(true), Some(true), Some(true), Some(true), Some(false), None, dummySelectedProducts, dummyPpi) )

      await(travelDetailsService.storeEuCountryCheck("nonEuOnly"))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("nonEuOnly"), None, None, None, None, None, None, Nil, Nil)) )(any())
    }
  }

  "Calling storeAgeOver17" should {

    "store age confirmation in keystore when no journey data there currently" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeAgeOver17(ageOver17 = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, None, None, None, None, Some(true), None, Nil)) )(any())
    }

    "store age confirmation in keystore maintaining subsequent journey data, keeping subsequent journey data the same if age confirmation answer has not changed " in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, None, None, None, Some(true), None, dummySelectedProducts ) )

      await(travelDetailsService.storeAgeOver17(ageOver17 = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, None, None, Some(true), None, dummySelectedProducts)) )(any())
    }

    "store age confirmation in keystore when journey data does exist, setting subsequent journey data to None if the age confirmation has changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, None, None, None, Some(false), None, dummySelectedProducts) )

      await(travelDetailsService.storeAgeOver17(ageOver17 = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, None, None, Some(true), None, Nil)) )(any())
    }
  }

  "Calling storeIrishBorder" should {

    "store Irish Border in keystore when no journey data there currently" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeIrishBorder(irishBorder = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, None, None, None, None, None, Some(true), Nil)) )(any())
    }

    "store Irish Border in keystore maintaining subsequent journey data, keeping subsequent journey data the same if Irish Border answer has not changed " in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, None, None, None, None, Some(true), dummySelectedProducts ) )

      await(travelDetailsService.storeIrishBorder(irishBorder = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, None, None, None, Some(true), dummySelectedProducts)) )(any())
    }

    "store Irish Border in keystore when journey data does exist, keepng subsequent journey data if the Irish Border has changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, None, None, None, None, Some(false), dummySelectedProducts) )

      await(travelDetailsService.storeIrishBorder(irishBorder = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, None, None, None, Some(true), dummySelectedProducts)) )(any())
    }
  }

  "Calling storePrivateCraft" should {

    "store private craft and in keystore when no journey data is currently there" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storePrivateCraft(privateCraft = false))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, None, None, None, Some(false), None, None, Nil)) )(any())
    }

    "store private craft setting in keystore when journey data does exist, keeping subsequent journey data the same if private craft answer has not changed " in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, None, None, Some(false), Some(true), None, dummySelectedProducts) )

      await(travelDetailsService.storePrivateCraft(privateCraft = false))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, None, Some(false), Some(true), None, dummySelectedProducts)) )(any())
    }

    "store private craft setting in keystore when journey data does exist, setting subsequent journey data to None if the private craft answer has changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, None, None, Some(false), None, None, dummySelectedProducts) )

      await(travelDetailsService.storePrivateCraft(privateCraft = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, None, Some(true), None, None, Nil)) )(any())
    }
  }

  "Calling storeVatResCheck" should {

    "store isVatResclaimed setting in keystore when no journey data is currently there" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeVatResCheck(isVatResClaimed = false))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, Some(false), None, None, None, None, None, Nil)) )(any())
    }

    "store isVatResClaimed setting in keystore when journey data does exist, keeping subsequent journey data the same if vat res answer has not changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), Some(true), Some(true), Some(false), Some(true), Some(false), None, dummySelectedProducts) )

      await(travelDetailsService.storeVatResCheck(isVatResClaimed = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), Some(true), Some(true), Some(false), Some(true), Some(false), None, dummySelectedProducts)) )(any())
    }

    "store isVatResClaimed setting in keystore when journey data does exist, setting subsequent journey data to None if the vat res answer has changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), Some(false), None, Some(false), None, None, None, Nil) )

      await(travelDetailsService.storeVatResCheck(isVatResClaimed = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), Some(true), None, None, None, None, None, Nil)) )(any())
    }
  }

  "Calling storeBringingOverAllowance" should {

    "store bringingOverAllowance setting in keystore when no journey data is currently there" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeBringingOverAllowance(bringingOverAllowance = false))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, None, None, Some(false), None, None, None, Nil)) )(any())
    }

    "store bringingOverAllowance setting in keystore when journey data does exist, keeping subsequent journey data the same if over allowance answer has not changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, None, Some(false), Some(true), Some(false), None, dummySelectedProducts) )

      await(travelDetailsService.storeBringingOverAllowance(bringingOverAllowance = false))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, Some(false), Some(true), Some(false), None, dummySelectedProducts)) )(any())
    }

    "store bringingOverAllowance setting in keystore when journey data does exist, setting subsequent journey data to None if over allowance answer has changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, None, Some(false), None, None, None, Nil) )

      await(travelDetailsService.storeBringingOverAllowance(bringingOverAllowance = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, None, Some(true), None, None, None, Nil)) )(any())
    }
  }

  "Calling storeBringingDutyFree" should {

    "store isBringingDutyFree setting in keystore when no journey data is currently there" in new LocalSetup {

      override val journeyDataInCache = None

      await(travelDetailsService.storeBringingDutyFree(isBringingDutyFree = false))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, None, Some(false), None, None, None, None, Nil)) )(any())
    }

    "store isBringingDutyFree setting in keystore when journey data does exist, keeping subsequent journey data the same if over allowance answer has not changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, Some(true), Some(false), Some(true), Some(false), None, dummySelectedProducts) )

      await(travelDetailsService.storeBringingDutyFree(isBringingDutyFree = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, Some(true), Some(false), Some(true), Some(false), None, dummySelectedProducts)) )(any())
    }

    "store isBringingDutyFree setting in keystore when journey data does exist, setting subsequent journey data to None if over allowance answer has changed" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("both"), None, Some(false), Some(false), None, None, None, Nil) )

      await(travelDetailsService.storeBringingDutyFree(isBringingDutyFree = true))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("both"), None, Some(true), None, None, None, None, Nil)) )(any())
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
