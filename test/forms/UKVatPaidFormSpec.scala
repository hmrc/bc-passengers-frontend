/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package forms

import util.BaseSpec

class UKVatPaidFormSpec extends  BaseSpec{

  "uKVatPaidForm" should {
    "return true if the user selects Yes" in {
      UKVatPaidForm.form.bind(Map("isUKVatPaid"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      UKVatPaidForm.form.bind(Map("isUKVatPaid"-> "false")).value shouldBe Some(false)
    }
    "return error if the user selects invalid value" in {
      UKVatPaidForm.form.bind(Map("isUKVatPaid"-> ""))
        .errors.exists(_.message == "error.is_uk_vat_paid") shouldBe true
    }
  }

}
