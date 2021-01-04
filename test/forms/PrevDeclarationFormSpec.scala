/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package forms

import util.BaseSpec

class PrevDeclarationFormSpec extends  BaseSpec{

  "prevDeclarationForm" should {
    "return true if the user selects Yes" in {
      PrevDeclarationForm.validateForm().bind(Map("prevDeclaration"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      PrevDeclarationForm.validateForm().bind(Map("prevDeclaration"-> "false")).value shouldBe Some(false)
    }
    "return error if the user selects invalid value" in {
      PrevDeclarationForm.validateForm().bind(Map("prevDeclaration"-> ""))
        .errors.exists(_.message == "error.previous_declaration") shouldBe true
    }
  }
}
