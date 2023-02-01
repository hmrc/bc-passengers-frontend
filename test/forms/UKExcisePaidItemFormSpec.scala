/*
 * Copyright 2023 HM Revenue & Customs
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

class UKExcisePaidItemFormSpec extends BaseSpec {

  "uKExcisePaidItemForm" should {
    "return true if the user selects Yes" in {
      UKExcisePaidItemForm.form.bind(Map("uKExcisePaidItem" -> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      UKExcisePaidItemForm.form.bind(Map("uKExcisePaidItem" -> "false")).value shouldBe Some(false)
    }
    "return error if the user selects invalid value" in {
      UKExcisePaidItemForm.form
        .bind(Map("uKExcisePaidItem" -> ""))
        .errors
        .exists(_.message == "error.is_uk_excise_paid_item") shouldBe true
    }
  }

}
