/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package forms

import util.BaseSpec

class UccReliefFormSpec extends  BaseSpec{

  "uccReliefForm" should {
    "return true if the user selects Yes" in {
      UccReliefForm.form.bind(Map("isUccRelief"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      UccReliefForm.form.bind(Map("isUccRelief"-> "false")).value shouldBe Some(false)
    }
    "return error if the user selects invalid value" in {
      UccReliefForm.form.bind(Map("isUccRelief"-> "invalid"))
        .errors.exists(_.message == "error.ucc") shouldBe true
    }
  }

}

