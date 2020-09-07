/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package forms

import play.api.mvc.Result
import play.api.test.Helpers.{route, status}
import util.BaseSpec

import scala.concurrent.Future

class ArrivingNIFormSpec extends  BaseSpec{

  "arrivingNIForm" should {
    "return true if the user selects Yes" in {
      ArrivingNIForm.form.bind(Map("arrivingNI"-> "true")).value shouldBe Some(true)
    }
    "return false if the user selects No" in {
      ArrivingNIForm.form.bind(Map("arrivingNI"-> "false")).value shouldBe Some(false)
    }
    "return error if the user selects invalid value" in {
      ArrivingNIForm.form.bind(Map("arrivingNI"-> "invalid"))
        .errors.exists(_.message == "error.arriving_ni") shouldBe true
    }
  }

}
