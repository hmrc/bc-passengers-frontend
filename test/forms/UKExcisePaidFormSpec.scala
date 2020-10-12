/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package forms

import util.BaseSpec

class UKExcisePaidFormSpec extends  BaseSpec{

  "uKExcisePaidForm" should {
    "return true if the user selects Yes" in {
      UKExcisePaidForm.form.bind(Map("isUKExcisePaid"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      UKExcisePaidForm.form.bind(Map("isUKExcisePaid"-> "false")).value shouldBe Some(false)
    }
    "return true if the user selects I am not bringing in any alcohol or tobacco" in {
      UKExcisePaidForm.form.bind(Map("isUKExcisePaid"-> "true")).value shouldBe Some(true)
    }
    "return error if the user selects invalid value" in {
      UKExcisePaidForm.form.bind(Map("isUKExcisePaid"-> ""))
        .errors.exists(_.message == "error.is_uk_excise_paid") shouldBe true
    }
  }

}