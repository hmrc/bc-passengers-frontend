/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package util

import play.api.data.validation

class UtilSpec extends BaseSpec {

  "Validating a cost" should {

    "succeed when passed 11,000.00" in {

      blankOkCostCheckConstraint("cost").apply("11,000.00")
    }

    "succeed when passed 11,000.00 to old constraint" in {

      bigDecimalCostCheckConstraint("cost").apply("11,000.00")
    }

    "restrict negative value like -95 to old constraint" in {

      bigDecimalCostCheckConstraint("cost").apply("-95.00").equals(validation.Valid) should be (false)
    }

    "restrict negative value like -9.50" in {

      blankOkCostCheckConstraint("cost").apply("-9.50").equals(validation.Valid) should be (false)
    }

  }


}


