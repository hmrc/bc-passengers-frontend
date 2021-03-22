/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services

import connectors.Cache
import models._
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

class PreviousDeclarationServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[DeclarationService].toInstance(MockitoSugar.mock[DeclarationService]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[Cache])
  }


  trait LocalSetup {

    val dummyPpi = List(PurchasedProductInstance(ProductPath("path"), "iid", Some(1.0), Some(100),
      Some(Country("AU", "Australia", "A2", isEu = false, isCountry = true, Nil)), None, Some("AUD"), Some(100.25)))
    val dummySelectedProducts = List(List("some product"), List("some other product"))

    lazy val previousDeclarationService: PreviousDeclarationService = {
      val service = app.injector.instanceOf[PreviousDeclarationService]
      val mock = service.cache
      when(mock.store(any())(any())) thenReturn Future.successful( JourneyData() )
      when(mock.storeJourneyData(any())(any())) thenReturn Future.successful( Some( JourneyData() ) )
      service
    }

    lazy val cacheMock: Cache = previousDeclarationService.cache

  }


  "Calling storePrevDeclarationDetails" should {

    val previousDeclarationRequest = PreviousDeclarationRequest("Potter", "SX12345", "someReference")

    "store previousDeclarationRequest when no journey data there currently" in new LocalSetup {

      await(previousDeclarationService.storePrevDeclarationDetails(None)(previousDeclarationRequest))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(prevDeclaration = Some(true), previousDeclarationRequest = Some(previousDeclarationRequest))))(any())
    }

    "store previousDeclarationRequest when DeclarationServiceFailureResponse received" in new LocalSetup {

      val journeyData: Option[JourneyData] = Some(JourneyData(prevDeclaration = Some(true)))

      when(injected[DeclarationService].retrieveDeclaration(any())(any())).thenReturn(Future.successful(DeclarationServiceFailureResponse))

      await(previousDeclarationService.storePrevDeclarationDetails(journeyData)(previousDeclarationRequest))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(JourneyData(prevDeclaration = Some(true), previousDeclarationRequest = Some(previousDeclarationRequest))))(any())
    }

    "store retrieved journeyData when success response" in new LocalSetup {

      val calculation = Calculation("160.45","25012.50","15134.59","40307.54")

      val productPath = ProductPath("other-goods/adult/adult-footwear")

      val country = Country("IN","title.india","IN",false,true,List())

      val editablePurchasedProductInstances = List(
        PurchasedProductInstance(productPath,"UnOGll",None,None,Some(country),None,Some("GBP"),Some(500),Some(false),Some(false),None,Some(false),None,Some(true))
      )

      val declarationResponse = DeclarationResponse(calculation, editablePurchasedProductInstances)

      val retrievedJourneyData: JourneyData = JourneyData(prevDeclaration = Some(true),
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        ageOver17 = Some(true),
        isUKResident = Some(false),
        privateCraft = Some(true),
        previousDeclarationRequest = Some(previousDeclarationRequest),
        declarationResponse = Some(declarationResponse)
      )

      val nonEditablePurchasedProductInstances = List(
        PurchasedProductInstance(productPath,"UnOGll",None,None,Some(country),None,Some("GBP"),Some(500),Some(false),Some(false),None,Some(false),None,Some(false))
      )

      val expectedJourneyData = retrievedJourneyData.copy(
        declarationResponse = retrievedJourneyData.declarationResponse.map { ds =>
          ds.copy(oldPurchaseProductInstances = nonEditablePurchasedProductInstances)
        })


      when(injected[DeclarationService].retrieveDeclaration(any())(any())).thenReturn(Future.successful(DeclarationServiceRetrieveSuccessResponse(retrievedJourneyData)))

      await(previousDeclarationService.storePrevDeclarationDetails(Some(retrievedJourneyData))(previousDeclarationRequest))

      verify(cacheMock, times(1)).storeJourneyData(
        meq(expectedJourneyData))(any())
    }

  }
}
