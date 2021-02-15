/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package forms

import util.BaseSpec

class EUEvidenceItemFormSpec extends  BaseSpec{

  "eUEvidenceItemForm" should {
    "return true if the user selects Yes" in {
      EUEvidenceItemForm.form.bind(Map("eUEvidenceItem"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      EUEvidenceItemForm.form.bind(Map("eUEvidenceItem"-> "false")).value shouldBe Some(false)
    }
    "return error if the user selects invalid value" in {
      EUEvidenceItemForm.form.bind(Map("eUEvidenceItem"-> ""))
        .errors.exists(_.message == "error.evidence_eu_item") shouldBe true
    }
  }

}
