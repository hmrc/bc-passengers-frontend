/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import util.BaseSpec

import scala.concurrent.Future

class TravelDetailsServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[Cache])
  }


  trait LocalSetup {

    val dummyPpi = List(PurchasedProductInstance(ProductPath("path"), "iid", Some(1.0), Some(100),
      Some(Country("AU", "Australia", "A2", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(100.25)))
    val dummySelectedProducts = List(List("some product"), List("some other product"))

    lazy val travelDetailsService: TravelDetailsService = {
      val service = app.injector.instanceOf[TravelDetailsService]
      val mock = service.cache
      when(mock.store(any())(any())) thenReturn Future.successful( JourneyData() )
      when(mock.storeJourneyData(any())(any())) thenReturn Future.successful( Some( JourneyData() ) )
      service
    }

    lazy val cacheMock: Cache = travelDetailsService.cache

  }

  "Calling storeEuCountryCheck" should {

    "store the eu country check in keystore when no journey data there currently" in new LocalSetup {

      await(travelDetailsService.storeEuCountryCheck(None)("nonEuOnly"))

      verify(cacheMock, times(1)).storeJourneyData( meq(JourneyData(None,Some("nonEuOnly"),None,None,None,None,None, None, None, None, None, None, None, Nil, Nil)) )(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None, Some("both"),None,None,None,None, None, None,None, None, Some(true), Some(false), None, Nil, dummyPpi) )

      await(travelDetailsService.storeEuCountryCheck(journeyData)("both"))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store the eu country check in keystore when journey data does exist, " +
      "setting subsequent journey data to None if the answer has changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None,Some("both"), Some(true),None,None,None,None, Some(true), Some(true), Some(true), Some(true), Some(false), None, Nil, dummyPpi, defaultCountry = Some("US"), defaultCurrency = Some("USD")) )

      await(travelDetailsService.storeEuCountryCheck(journeyData)("nonEuOnly"))

      verify(cacheMock, times(1)).storeJourneyData( meq(JourneyData(None,Some("nonEuOnly"),Some(true), None,None,None,None,None, None, None, None, None, None, Nil, Nil, defaultCountry = None, defaultCurrency = None)) )(any())
    }

    "store the eu country check in keystore when bringing goods from EU countries" in new LocalSetup {

      await(travelDetailsService.storeEuCountryCheck(None)("euOnly"))

      verify(cacheMock, times(1)).storeJourneyData( meq(JourneyData(None,Some("euOnly"),None,None,None,None,None, None, None, None, None, None, None, Nil, Nil,None,None,None,None,None,None,None,None)) )(any())
    }
  }

  "Calling storeAgeOver17" should {

    "store age confirmation in keystore when no journey data there currently" in new LocalSetup {

      await(travelDetailsService.storeAgeOver17(None)(ageOver17 = true))

      verify(cacheMock, times(1)).storeJourneyData( meq(JourneyData(None,None, None,None,None,None,None,None, None, None, None, Some(true), None, Nil)) )(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None,Some("both"),None,None,None, None, None,None, None, None,None, Some(true), None) )

      await(travelDetailsService.storeAgeOver17(journeyData)(ageOver17 = true))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store age confirmation in keystore when journey data does exist, setting subsequent journey data to None if the age confirmation has changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None,Some("both"), Some(false), None,None,None, None, None,None, None,None, Some(false), None) )

      await(travelDetailsService.storeAgeOver17(journeyData)(ageOver17 = true))

      verify(cacheMock, times(1)).storeJourneyData( meq(JourneyData(None,Some("both"),Some(false), None,None,None,None, None, None,None, None, Some(true), None, Nil)) )(any())
    }
  }

  "Calling storePrivateCraft" should {

    "store private craft and in keystore when no journey data is currently there" in new LocalSetup {

      await(travelDetailsService.storePrivateCraft(None)(privateCraft = false))

      verify(cacheMock, times(1)).storeJourneyData( meq(JourneyData(None,None,None,None,None, None,None, None,None, None, Some(false), None, None, Nil)) )(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None,Some("both"), Some(true), None,None, None,None,None, None,None, Some(false), Some(true), None) )

      await(travelDetailsService.storePrivateCraft(journeyData)(privateCraft = false))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store private craft setting in keystore when journey data does exist, setting subsequent journey data to None if the private craft answer has changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None,Some("both"), None,None,None, None, None,None, None,None, Some(false), None, None) )

      await(travelDetailsService.storePrivateCraft(journeyData)(privateCraft = true))

      verify(cacheMock, times(1)).storeJourneyData( meq(JourneyData(None,Some("both"),None,None,None,None,None, None, None, None, Some(true), None, None, Nil)) )(any())
    }
  }


  "Calling storeBringingOverAllowance" should {

    "store bringingOverAllowance setting in keystore when no journey data is currently there" in new LocalSetup {

      await(travelDetailsService.storeBringingOverAllowance(None)(bringingOverAllowance = false))

      verify(cacheMock, times(1)).storeJourneyData( meq(JourneyData(None,None, None,None,None,None,None, None,None, Some(false), None, None, None, Nil)) )(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None,Some("both"),None,None, None,None,None, None,None, Some(false), Some(true), Some(false), None) )

      await(travelDetailsService.storeBringingOverAllowance(journeyData)(bringingOverAllowance = false))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store bringingOverAllowance setting in keystore when journey data does exist, setting subsequent journey data to None if over allowance answer has changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None, Some("both"), None,None,None,None,None, None,None, Some(false), None, None, None, Nil) )

      await(travelDetailsService.storeBringingOverAllowance(journeyData)(bringingOverAllowance = true))

      verify(cacheMock, times(1)).storeJourneyData( meq(JourneyData(None, Some("both"), None,None,None,None,None,None, None, Some(true), None, None, None, Nil)) )(any())
    }
  }


  "Calling storeArrivingNI" should {

    "store arrivingNI in keystore when no journey data there currently" in new LocalSetup {

      await(travelDetailsService.storeArrivingNI(None)(arrivingNICheck = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(arrivingNICheck = Some(true))) )(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None, Some("both"), arrivingNICheck = Some(true)) )

      await(travelDetailsService.storeArrivingNI(journeyData)(arrivingNICheck = true))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store arrivingNI in keystore when journey data does exist, keeping existing journey data if the ArrivingNI has changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(None, Some("both"),
        arrivingNICheck = Some(false), isVatResClaimed = Some(true)))

      await(travelDetailsService.storeArrivingNI(journeyData)(arrivingNICheck = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(None, Some("both"), arrivingNICheck = Some(true),
        isVatResClaimed = Some(true)) ))(any())
    }
  }

  "Calling storeUKVatPaid" should {

    "store isUKVatPaid in keystore when no journey data there currently" in new LocalSetup {

      await(travelDetailsService.storeUKVatPaid(None)(isUKVatPaid = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(isUKVatPaid = Some(true))))(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some(JourneyData(None, Some("greatBritain"), arrivingNICheck = Some(true), isUKVatPaid = Some(true)))

      await(travelDetailsService.storeUKVatPaid(journeyData)(isUKVatPaid = true))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store isUKVatPaid when journey data does exist, keeping existing journey data if the isUKVatPaid has changed" in new LocalSetup {
      val journeyData: Option[JourneyData] = Some(JourneyData(None, Some("greatBritain"),
        arrivingNICheck = Some(true), isUKVatPaid = Some(false)))
      await(travelDetailsService.storeUKVatPaid(journeyData)(isUKVatPaid = true))
      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(None, Some("greatBritain"), arrivingNICheck = Some(true),
          isUKVatPaid = Some(true))))(any())
    }
  }


  "Calling storeUKExcisePaid" should {

    "store isUKVatExcisePaid when no journey data there currently" in new LocalSetup {

      await(travelDetailsService.storeUKExcisePaid(None)(isUKVatExcisePaid = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(isUKVatExcisePaid = Some(true))) )(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(isUKVatPaid = Some(true), isUKVatExcisePaid = Some(true)) )

      await(travelDetailsService.storeUKExcisePaid(journeyData)(isUKVatExcisePaid = true))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store isUKVatExcisePaid when journey data does exist, keeping existing journey data if the isUKVatExcisePaid has changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some(JourneyData(isUKVatPaid = Some(true), isUKVatExcisePaid = Some(false)))

      await(travelDetailsService.storeUKExcisePaid(journeyData)(isUKVatExcisePaid = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(isUKVatPaid = Some(true), isUKVatExcisePaid = Some(true)) ))(any())
    }
  }

  "Calling storeUKResident" should {

    "store isUKResident when no journey data there currently" in new LocalSetup {

      await(travelDetailsService.storeUKResident(None)(isUKResident = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(isUKResident = Some(true))) )(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(isUKVatExcisePaid = Some(true), isUKResident = Some(true)))

      await(travelDetailsService.storeUKResident(journeyData)(isUKResident = true))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store isUKResident when journey data does exist, reset existing journey data if the isUKResident has changed" in new LocalSetup {

      val ppi = PurchasedProductInstance(iid = "someId", path = ProductPath("alcohol/beer"), isVatPaid = Some(true))
      val journeyData: Option[JourneyData] = Some(JourneyData(isUKVatExcisePaid = Some(true), euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(false), purchasedProductInstances = List(ppi), bringingOverAllowance = Some(true)))

      await(travelDetailsService.storeUKResident(journeyData)(isUKResident = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(true))))(any())
    }
  }

  "Calling storeUccRelief" should {

    "store isUccRelief when no journey data there currently" in new LocalSetup {

      await(travelDetailsService.storeUccRelief(None)(isUccRelief = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(isUccRelief = Some(true))) )(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some( JourneyData(isUKResident = Some(false), isUccRelief = Some(true)))

      await(travelDetailsService.storeUccRelief(journeyData)(isUccRelief = true))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store isUccRelief when journey data does exist, keeping existing journey data if the isUccRelief has changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some(JourneyData(isUKResident = Some(false), isUccRelief = Some(false)))

      await(travelDetailsService.storeUccRelief(journeyData)(isUccRelief = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(isUKResident = Some(false), isUccRelief = Some(true))))(any())
    }
  }

}
