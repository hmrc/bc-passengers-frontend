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
import models._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.mongo.MongoComponent
import util.BaseSpec

import scala.concurrent.Future

class PreviousDeclarationServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[DeclarationService].toInstance(mock(classOf[DeclarationService])))
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .build()

  override def beforeEach(): Unit =
    reset(app.injector.instanceOf[Cache])

  trait LocalSetup {

    val dummyPpi: List[PurchasedProductInstance]  = List(
      PurchasedProductInstance(
        ProductPath("path"),
        "iid",
        Some(1.0),
        Some(100),
        Some(Country("AU", "Australia", "A2", isEu = false, isCountry = true, Nil)),
        None,
        Some("AUD"),
        Some(100.25)
      )
    )
    val dummySelectedProducts: List[List[String]] = List(List("some product"), List("some other product"))

    lazy val previousDeclarationService: PreviousDeclarationService = {
      val service = app.injector.instanceOf[PreviousDeclarationService]
      val mock    = service.cache
      when(mock.store(any())(any())) thenReturn Future.successful(JourneyData())
      when(mock.storeJourneyData(any())(any())) thenReturn Future.successful(Some(JourneyData()))
      service
    }

    lazy val cacheMock: Cache = previousDeclarationService.cache

  }

  "Calling storePrevDeclarationDetails" should {

    val previousDeclarationRequest = PreviousDeclarationRequest("Potter", "someReference")

    "store previousDeclarationRequest when no journey data there currently" in new LocalSetup {

      await(previousDeclarationService.storePrevDeclarationDetails(None)(previousDeclarationRequest))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(prevDeclaration = Some(true), previousDeclarationRequest = Some(previousDeclarationRequest)))
      )(any())
    }

    "store previousDeclarationRequest when DeclarationServiceFailureResponse received" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some(JourneyData(prevDeclaration = Some(true)))

      when(injected[DeclarationService].retrieveDeclaration(any())(any()))
        .thenReturn(Future.successful(DeclarationServiceFailureResponse))

      await(previousDeclarationService.storePrevDeclarationDetails(journeyData)(previousDeclarationRequest))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(prevDeclaration = Some(true), previousDeclarationRequest = Some(previousDeclarationRequest)))
      )(any())
    }

    "store previousDeclarationRequest when DeclarationServiceSuccessResponse received" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some(JourneyData(prevDeclaration = Some(true)))

      when(injected[DeclarationService].retrieveDeclaration(any())(any()))
        .thenReturn(Future.successful(DeclarationServiceSuccessResponse))

      await(previousDeclarationService.storePrevDeclarationDetails(journeyData)(previousDeclarationRequest))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(prevDeclaration = Some(true), previousDeclarationRequest = Some(previousDeclarationRequest)))
      )(any())
    }

    "store retrieved journeyData when success response" in new LocalSetup {

      val calculation: Calculation = Calculation("160.45", "25012.50", "15134.59", "40307.54")

      val productPath: ProductPath = ProductPath("other-goods/adult/adult-footwear")

      val otherGoodsSearchItem: OtherGoodsSearchItem =
        OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))

      val country: Country = Country("IN", "title.india", "IN", isEu = false, isCountry = true, List())

      val liabilityDetails: LiabilityDetails = LiabilityDetails("32.0", "0.0", "126.4", "158.40")

      val editablePurchasedProductInstances: List[PurchasedProductInstance] = List(
        PurchasedProductInstance(
          productPath,
          "UnOGll",
          None,
          None,
          Some(country),
          None,
          Some("GBP"),
          Some(500),
          Some(otherGoodsSearchItem),
          Some(false),
          Some(false),
          None,
          Some(false),
          None,
          Some(true)
        )
      )

      val declarationResponse: DeclarationResponse =
        DeclarationResponse(calculation, liabilityDetails, editablePurchasedProductInstances)

      val retrievedJourneyData: JourneyData = JourneyData(
        prevDeclaration = Some(true),
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        ageOver17 = Some(true),
        isUKResident = Some(false),
        privateCraft = Some(true),
        previousDeclarationRequest = Some(previousDeclarationRequest),
        declarationResponse = Some(declarationResponse)
      )

      val nonEditablePurchasedProductInstances: List[PurchasedProductInstance] = List(
        PurchasedProductInstance(
          productPath,
          "UnOGll",
          None,
          None,
          Some(country),
          None,
          Some("GBP"),
          Some(500),
          Some(otherGoodsSearchItem),
          Some(false),
          Some(false),
          None,
          Some(false),
          None,
          Some(false)
        )
      )

      val expectedJourneyData: JourneyData =
        retrievedJourneyData.copy(declarationResponse = retrievedJourneyData.declarationResponse.map { ds =>
          ds.copy(oldPurchaseProductInstances = nonEditablePurchasedProductInstances)
        })

      when(injected[DeclarationService].retrieveDeclaration(any())(any()))
        .thenReturn(Future.successful(DeclarationServiceRetrieveSuccessResponse(retrievedJourneyData)))

      await(
        previousDeclarationService.storePrevDeclarationDetails(Some(retrievedJourneyData))(previousDeclarationRequest)
      )

      verify(cacheMock, times(1)).storeJourneyData(meq(expectedJourneyData))(any())
    }

  }

  "Calling storePrevDeclaration" should {

    "store storePrevDeclaration when no journey data there currently" in new LocalSetup {

      await(previousDeclarationService.storePrevDeclaration(None)(prevDeclaration = false))

      verify(cacheMock, times(1)).storeJourneyData(meq(JourneyData(prevDeclaration = Some(false))))(any())
    }

    "not update the journey data if the answer has not changed" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some(JourneyData(prevDeclaration = Some(true)))

      await(previousDeclarationService.storePrevDeclaration(journeyData)(prevDeclaration = true))

      verify(cacheMock, times(0)).storeJourneyData(any())(any())
    }

    "store prevDeclaration when journey data does exist, reset existing journey data if the prevDeclaration has changed" in new LocalSetup {

      val ppi: PurchasedProductInstance    =
        PurchasedProductInstance(iid = "someId", path = ProductPath("alcohol/beer"), isVatPaid = Some(true))
      val journeyData: Option[JourneyData] = Some(
        JourneyData(
          prevDeclaration = Some(false),
          isUKVatExcisePaid = Some(true),
          euCountryCheck = Some("greatBritain"),
          arrivingNICheck = Some(true),
          isUKResident = Some(false),
          purchasedProductInstances = List(ppi),
          bringingOverAllowance = Some(true)
        )
      )

      await(previousDeclarationService.storePrevDeclaration(journeyData)(prevDeclaration = true))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(prevDeclaration = Some(true), None, None, None, None, None, None))
      )(any())
    }
  }
}
