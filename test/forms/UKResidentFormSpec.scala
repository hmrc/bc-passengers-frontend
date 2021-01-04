/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package forms

import util.BaseSpec

class UKResidentFormSpec extends  BaseSpec{

  "uKResidentForm" should {
    "return true if the user selects Yes" in {
      UKResidentForm.form.bind(Map("isUKResident"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      UKResidentForm.form.bind(Map("isUKResident"-> "false")).value shouldBe Some(false)
    }
    "return error if the user selects invalid value" in {
      UKResidentForm.form.bind(Map("isUKResident"-> "invalid"))
        .errors.exists(_.message == "error.is_uk_resident") shouldBe true
    }
  }

}
