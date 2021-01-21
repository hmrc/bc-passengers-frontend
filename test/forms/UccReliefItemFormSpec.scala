/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package forms

import util.BaseSpec

class UccReliefItemFormSpec extends  BaseSpec{

  "uccReliefItemForm" should {
    "return true if the user selects Yes" in {
      UccReliefItemForm.form.bind(Map("isUccRelief"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      UccReliefItemForm.form.bind(Map("isUccRelief"-> "false")).value shouldBe Some(false)
    }
    "return error if the user selects invalid value" in {
      UccReliefItemForm.form.bind(Map("isUccRelief"-> "invalid"))
        .errors.exists(_.message == "error.ucc_item") shouldBe true
    }
  }

}

