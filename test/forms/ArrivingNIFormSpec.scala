/*
 * Copyright 2020 HM Revenue & Customs
 *
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
