package util

class UtilSpec extends BaseSpec {

  "Validating a cost" should {

    "succeed when passed 11,000.00" in {

      blankOkCostCheckConstraint("cost").apply("11,000.00")
    }

    "succeed when passed 11,000.00 to old constraint" in {

      bigDecimalCostCheckConstraint("cost").apply("11,000.00")
    }

  }


}


