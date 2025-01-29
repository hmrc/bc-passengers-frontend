/*
 * Copyright 2025 HM Revenue & Customs
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

import models.*
import play.api.data.Form
import play.api.test.FakeRequest
import util.BaseSpec

class OtherGoodsInputControllerFormSpec extends BaseSpec {

  "OtherGoodsInputController" when {
    ".addCostForm" should {
      "fail on an invalid country code" in {
        val form: Form[OtherGoodsDto] = injected[OtherGoodsInputController].addCostForm
          .bind(
            Map(
              "searchTerm" -> "label.other-goods.antiques",
              "country"    -> "Hey",
              "currency"   -> "EUR",
              "cost"       -> "4,444.00"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                  shouldBe 1
        form.error("country").get.message shouldBe "error.country.invalid"
      }
    }

    ".continueForm" should {
      val otherDto: OtherGoodsDto             = OtherGoodsDto(
        searchTerm = Some(
          OtherGoodsSearchItem(
            name = "label.other-goods.antiques",
            path = ProductPath(path = "other-goods/antiques")
          )
        ),
        country = "FR",
        originCountry = None,
        currency = "EUR",
        cost = 4444.00,
        isVatPaid = None,
        isUccRelief = None,
        isCustomPaid = None,
        hasEvidence = None
      )
      implicit val localContext: LocalContext = LocalContext(
        request = FakeRequest(),
        sessionId = "sessionId",
        journeyData = Some(JourneyData())
      )
      val form: Form[OtherGoodsDto]           = injected[OtherGoodsInputController].continueForm
        .bind(
          Map(
            "searchTerm" -> "label.other-goods.antiques",
            "country"    -> "FR",
            "currency"   -> "EUR",
            "cost"       -> "4,444.00"
          )
        )

      "return the correct result when filled" in {
        form.fill(otherDto).value.get shouldBe form.value.get
      }
    }
  }
}
