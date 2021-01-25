/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package forms

import util.BaseSpec

class UKExcisePaidItemFormSpec extends  BaseSpec{

  "uKExcisePaidItemForm" should {
    "return true if the user selects Yes" in {
      UKExcisePaidItemForm.form.bind(Map("uKExcisePaidItem"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      UKExcisePaidItemForm.form.bind(Map("uKExcisePaidItem"-> "false")).value shouldBe Some(false)
    }
    "return error if the user selects invalid value" in {
      UKExcisePaidItemForm.form.bind(Map("uKExcisePaidItem"-> ""))
        .errors.exists(_.message == "error.is_uk_excise_paid_item") shouldBe true
    }
  }

}
