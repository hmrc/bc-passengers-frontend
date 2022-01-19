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

package forms

import util.BaseSpec

class ArrivingNIFormSpec extends  BaseSpec{

  "arrivingNIForm" should {
    "return true if the user selects Yes" in {
      ArrivingNIForm.validateForm().bind(Map("arrivingNI"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      ArrivingNIForm.validateForm().bind(Map("arrivingNI"-> "false")).value shouldBe Some(false)
    }
    "return true if the user coming from GB and selects Yes value" in {
      ArrivingNIForm.validateForm(Some("greatBritain")).bind(Map("arrivingNI"-> "true")).value shouldBe Some(true)
    }
    "return error if the user selects invalid value" in {
      ArrivingNIForm.validateForm().bind(Map("arrivingNI"-> ""))
        .errors.exists(_.message == "error.arriving_ni") shouldBe true
    }
    "return error if the user coming from GB and selects No value" in {
      ArrivingNIForm.validateForm(Some("greatBritain")).bind(Map("arrivingNI"-> "false"))
        .errors.exists(_.message == "error.arriving_gb") shouldBe true
    }
  }

}
