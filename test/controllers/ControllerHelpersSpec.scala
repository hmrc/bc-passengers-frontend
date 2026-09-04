/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import config.AppConfig
import connectors.Cache
import models.{JourneyData, ProductPath}
import org.mockito.Mockito.*
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import services.{CalculatorService, ProductTreeService}
import util.BaseSpec
import views.html.errorTemplate

import scala.concurrent.Future

class ControllerHelpersSpec extends BaseSpec with ControllerHelpers {

  def cache: Cache                           = mock(classOf[Cache])
  def productTreeService: ProductTreeService = mock(classOf[ProductTreeService])
  def calculatorService: CalculatorService   = mock(classOf[CalculatorService])
  def errorTemplate: errorTemplate           = mock(classOf[errorTemplate])

  given appConfig: AppConfig                                       = mock(classOf[AppConfig])
  protected def controllerComponents: MessagesControllerComponents = mock(classOf[MessagesControllerComponents])

  def exposedMarkReturnToAddedItem(result: Result, editUrl: String, productPath: ProductPath)(implicit
    context: LocalContext
  ): Result =
    markReturnToAddedItem(result, editUrl, productPath)

  def exposedPopReturnToAddedItem(result: Result)(implicit context: LocalContext): Result =
    popReturnToAddedItem(result)

  def exposedClearReturnToAddedItem(result: Result)(implicit context: LocalContext): Result =
    clearReturnToAddedItem(result)

  def exposedClearReturnToAddedItemUnlessCurrentEdit(result: Result, editUrl: String)(implicit
    context: LocalContext
  ): Result =
    clearReturnToAddedItemUnlessCurrentEdit(result, editUrl)

  def exposedBackLinkForAddedItemEdit(defaultBackLink: Option[String], editUrl: String)(implicit
    context: LocalContext
  ): Option[String] =
    backLinkForAddedItemEdit(defaultBackLink, editUrl)

  "ControllerHelpers" when {
    ".logAndRedirect" should {
      "redirect to the correct location" in {
        val result: Future[Result] = logAndRedirect(
          logMessage = "Unable to get journeyData! Starting a new session...",
          redirectLocation = routes.TravelDetailsController.newSession
        )

        result.map(_ shouldBe routes.TravelDetailsController.newSession)
      }
    }

    ".requireJourneyData" should {
      "redirect to the correct location" in {
        given localContext: LocalContext = LocalContext(
          request = FakeRequest(),
          sessionId = "sessionId"
        )

        val result: Future[Result] = requireJourneyData(_ => Future.successful(Ok))

        result.map(_ shouldBe routes.TravelDetailsController.newSession)
      }
    }

    ".requireCalculatorResponse" should {
      "redirect to the correct location" in {
        given localContext: LocalContext = LocalContext(
          request = FakeRequest(),
          sessionId = "sessionId",
          journeyData = Some(JourneyData())
        )

        val result: Future[Result] = requireCalculatorResponse(_ => Future.successful(Ok))

        result.map(_ shouldBe routes.DashboardController.showDashboard)
      }
    }

    ".markReturnToAddedItem" should {
      "store the item edit url, select url, dashboard url and product path in session" in {
        val request                      = FakeRequest()
        given localContext: LocalContext = LocalContext(
          request = request,
          sessionId = "sessionId"
        )

        val result  = exposedMarkReturnToAddedItem(Ok, "/edit-item", ProductPath("alcohol/spirits"))
        val session = result.session(request)

        session.get(returnToAddedItemSessionKey)             shouldBe Some("/edit-item")
        session.get(returnToAddedItemSelectUrlSessionKey)    shouldBe Some(
          "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol"
        )
        session.get(returnToAddedItemProductPathKey)         shouldBe Some("alcohol/spirits")
        session.get(returnToAddedItemDashboardUrlSessionKey) shouldBe Some("/edit-item")
        session.get(returnToAddedItemStackSessionKey)        shouldBe Some(
          "/edit-item|/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol|alcohol/spirits|/edit-item"
        )
      }
    }

    ".popReturnToAddedItem" should {
      "move the previous item in the stack into the active backlink session keys" in {
        val request                      = FakeRequest().withSession(
          returnToAddedItemSessionKey             -> "/edit-item-2",
          returnToAddedItemSelectUrlSessionKey    -> "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco",
          returnToAddedItemProductPathKey         -> "tobacco/cigars",
          returnToAddedItemDashboardUrlSessionKey -> "/check-your-item/tobacco/cigars/iid2",
          returnToAddedItemStackSessionKey        ->
            ("/edit-item-1|/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol|alcohol/spirits|/check-your-item/alcohol/spirits/iid1\n" +
              "/edit-item-2|/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco|tobacco/cigars|/check-your-item/tobacco/cigars/iid2")
        )
        given localContext: LocalContext = LocalContext(
          request = request,
          sessionId = "sessionId"
        )

        val result  = exposedPopReturnToAddedItem(Ok)
        val session = result.session(request)

        session.get(returnToAddedItemSessionKey)             shouldBe Some("/edit-item-1")
        session.get(returnToAddedItemSelectUrlSessionKey)    shouldBe Some(
          "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol"
        )
        session.get(returnToAddedItemProductPathKey)         shouldBe Some("alcohol/spirits")
        session.get(returnToAddedItemDashboardUrlSessionKey) shouldBe Some("/check-your-item/alcohol/spirits/iid1")
        session.get(returnToAddedItemStackSessionKey)        shouldBe Some(
          "/edit-item-1|/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol|alcohol/spirits|/check-your-item/alcohol/spirits/iid1"
        )
      }

      "clear the active backlink session keys when there is no previous item in the stack" in {
        val request                      = FakeRequest().withSession(
          returnToAddedItemSessionKey             -> "/edit-item",
          returnToAddedItemSelectUrlSessionKey    -> "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol",
          returnToAddedItemProductPathKey         -> "alcohol/spirits",
          returnToAddedItemDashboardUrlSessionKey -> "/check-your-item/alcohol/spirits/iid0",
          returnToAddedItemStackSessionKey        ->
            "/edit-item|/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol|alcohol/spirits|/check-your-item/alcohol/spirits/iid0"
        )
        given localContext: LocalContext = LocalContext(
          request = request,
          sessionId = "sessionId"
        )

        val result  = exposedPopReturnToAddedItem(Ok)
        val session = result.session(request)

        session.get(returnToAddedItemSessionKey)             shouldBe None
        session.get(returnToAddedItemSelectUrlSessionKey)    shouldBe None
        session.get(returnToAddedItemProductPathKey)         shouldBe None
        session.get(returnToAddedItemDashboardUrlSessionKey) shouldBe None
        session.get(returnToAddedItemStackSessionKey)        shouldBe None
      }
    }

    ".clearReturnToAddedItemUnlessCurrentEdit" should {
      "keep the session when the active item matches the edit url" in {
        val request                      = FakeRequest().withSession(returnToAddedItemSessionKey -> "/edit-item")
        given localContext: LocalContext = LocalContext(
          request = request,
          sessionId = "sessionId"
        )

        val result = exposedClearReturnToAddedItemUnlessCurrentEdit(Ok, "/edit-item")

        result.session(request).get(returnToAddedItemSessionKey) shouldBe Some("/edit-item")
      }

      "clear the session when the active item does not match the edit url" in {
        val request                      = FakeRequest().withSession(returnToAddedItemSessionKey -> "/edit-item")
        given localContext: LocalContext = LocalContext(
          request = request,
          sessionId = "sessionId"
        )

        val result = exposedClearReturnToAddedItemUnlessCurrentEdit(Ok, "/different-item")

        result.session(request).get(returnToAddedItemSessionKey) shouldBe None
      }
    }

    ".backLinkForAddedItemEdit" should {
      "return the select page when the current edit item is active" in {
        given localContext: LocalContext = LocalContext(
          request = FakeRequest().withSession(
            returnToAddedItemSessionKey          -> "/edit-item",
            returnToAddedItemSelectUrlSessionKey -> "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol"
          ),
          sessionId = "sessionId"
        )

        exposedBackLinkForAddedItemEdit(Some("/default"), "/edit-item") shouldBe Some(
          "/check-tax-on-goods-you-bring-into-the-uk/select-goods/alcohol"
        )
      }

      "return the default backlink when the current edit item is not active" in {
        given localContext: LocalContext = LocalContext(
          request = FakeRequest().withSession(returnToAddedItemSessionKey -> "/edit-item"),
          sessionId = "sessionId"
        )

        exposedBackLinkForAddedItemEdit(Some("/default"), "/different-item") shouldBe Some("/default")
      }
    }
  }
}
