/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import org.scalatest.matchers.must.Matchers.mustBe
import play.api.libs.json
import play.api.libs.json.{JsError, JsSuccess, Json}
import util.{BaseSpec, parseLocalDate, parseLocalTime}

import java.time.{LocalDate, LocalTime}

class JourneyDataSpec extends BaseSpec {

  private val countryEgypt = Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)

  "Calling JourneyData.getOrCreatePurchasedProductInstance" should {
    "return the specified product from the journey data if it exists" in {
      val journeyData = JourneyData(purchasedProductInstances =
        List(
          PurchasedProductInstance(
            ProductPath("alcohol/beer"),
            "iid4",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("alcohol/cider"),
            "iid3",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("tobacco/cigars"),
            "iid3",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("tobacco/cigarettes"),
            "iid3",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("other-goods/childrens"),
            "iid0",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("other-goods/childrens"),
            "iid1",
            Some(1.23456),
            None,
            Some(countryEgypt),
            None,
            Some("USD"),
            Some(BigDecimal(10.567))
          )
        )
      )

      journeyData.getOrCreatePurchasedProductInstance(
        ProductPath("other-goods/childrens"),
        "iid1"
      ) shouldEqual PurchasedProductInstance(
        ProductPath("other-goods/childrens"),
        "iid1",
        Some(1.23456),
        None,
        Some(countryEgypt),
        None,
        Some("USD"),
        Some(BigDecimal(10.567))
      )
    }

    "return a new PurchasedProductInstance if the specified one does not exist" in {
      val journeyData = JourneyData(purchasedProductInstances =
        List(
          PurchasedProductInstance(
            ProductPath("alcohol/beer"),
            "iid4",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("alcohol/cider"),
            "iid3",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("tobacco/cigars"),
            "iid3",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("tobacco/cigarettes"),
            "iid3",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("other-goods/childrens"),
            "iid0",
            Some(1.54332),
            None,
            Some(countryEgypt),
            None,
            Some("AUD"),
            Some(BigDecimal(10.234))
          ),
          PurchasedProductInstance(
            ProductPath("other-goods/childrens"),
            "iid1",
            Some(1.23456),
            None,
            Some(countryEgypt),
            None,
            Some("USD"),
            Some(BigDecimal(10.567))
          )
        )
      )

      journeyData.getOrCreatePurchasedProductInstance(
        ProductPath("alcohol/sparkling"),
        "iid5"
      ) shouldEqual PurchasedProductInstance(ProductPath("alcohol/sparkling"), "iid5", None, None, None, None)
    }
  }

  "Calling JourneyData.revertPurchasedProductInstance" should {
    val purchasedProductInstances: List[PurchasedProductInstance] = List(
      PurchasedProductInstance(
        path = ProductPath(path = "tobacco/chewing-tobacco"),
        iid = "iid0"
      )
    )

    val workingInstance: PurchasedProductInstance = PurchasedProductInstance(
      path = ProductPath(path = "tobacco/rolling-tobacco"),
      iid = "iid0"
    )

    val journeyData: JourneyData = JourneyData(
      purchasedProductInstances = purchasedProductInstances,
      workingInstance = Some(workingInstance)
    )

    "return the correct journey data model" when {
      "both iid values are the same" in {
        val result: JourneyData = journeyData.revertPurchasedProductInstance()

        result shouldBe journeyData.copy(purchasedProductInstances = List(workingInstance))
      }

      "iid values are different" in {
        val result: JourneyData = journeyData
          .copy(
            workingInstance = Some(
              workingInstance.copy(iid = "iid1")
            )
          )
          .revertPurchasedProductInstance()

        result shouldBe journeyData.copy(
          workingInstance = Some(workingInstance.copy(iid = "iid1")),
          purchasedProductInstances = purchasedProductInstances
        )
      }

      "workingInstance is not specified" in {
        val result: JourneyData = journeyData.copy(workingInstance = None).revertPurchasedProductInstance()

        result shouldBe journeyData.copy(workingInstance = None)
      }
    }
  }

  val testUserInfo: PreUserInformation = PreUserInformation(
    nameForm = WhatIsYourNameForm(
      firstName = "firstName",
      lastName = "lastName"
    ),
    identification = Some(
      IdentificationForm(
        identificationType = "identificationType",
        identificationNumber = Some("identificationNumber")
      )
    ),
    emailAddress = Some("emailAddress"),
    arrivalForm = Some(
      ArrivalForm(
        selectPlaceOfArrival = "LHR",
        enterPlaceOfArrival = "enterPlaceOfArrival",
        dateOfArrival = parseLocalDate("2018-11-12"),
        timeOfArrival = parseLocalTime("12:20 pm")
      )
    )
  )

  val declarationResponse: DeclarationResponse = DeclarationResponse(
    Calculation("0.00", "0.00", "0.00", "0.00"),
    LiabilityDetails("32.0", "0.0", "126.4", "158.40"),
    List(
      PurchasedProductInstance(
        ProductPath("other-goods/adult/adult-footwear"),
        "UnOGll",
        None,
        None,
        None,
        None,
        Some("GBP"),
        Some(500),
        Some(
          OtherGoodsSearchItem(
            "label.other-goods.mans_shoes",
            ProductPath("other-goods/adult/adult-footwear")
          )
        ),
        Some(false),
        Some(false),
        None,
        Some(false),
        None,
        Some(false)
      )
    )
  )

  val previousDeclarationRequest: PreviousDeclarationRequest = PreviousDeclarationRequest(
    lastName = "lastName",
    referenceNumber = "referenceNumber"
  )

  "PurchasedProductInstance" should {

    val purchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
      path = ProductPath("alcohol/beer"),
      iid = "iid0",
      weightOrVolume = Some(20.0),
      noOfSticks = Some(1),
      country = Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
      originCountry =
        Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
      currency = Some("EUR"),
      cost = Some(BigDecimal(12.99)),
      isCustomPaid = Some(false),
      isExcisePaid = Some(false),
      isUccRelief = Some(false),
      hasEvidence = Some(false),
      isEditable = Some(true)
    )

    val json = Json.obj(
      "path"           -> ProductPath("alcohol/beer"),
      "iid"            -> "iid0",
      "weightOrVolume" -> Some(20.0),
      "noOfSticks"     -> Some(1),
      "country"        ->
        Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
      "originCountry"  ->
        Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
      "currency"       -> Some("EUR"),
      "cost"           -> Some(BigDecimal(12.99)),
      "isCustomPaid"   -> Some(false),
      "isExcisePaid"   -> Some(false),
      "isUccRelief"    -> Some(false),
      "hasEvidence"    -> Some(false),
      "isEditable"     -> Some(true)
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(purchasedProductInstance) shouldBe json
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        json.validate[PurchasedProductInstance] shouldBe JsSuccess(purchasedProductInstance)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[PurchasedProductInstance] shouldBe a[JsError]
      }
    }

    "when optional fields are missing" in {
      val incorrectJson = Json.obj(
        "path" -> ProductPath("alcohol/beer"),
        "iid"  -> "iid0"
      )

      incorrectJson.validate[PurchasedProductInstance] shouldBe JsSuccess(
        PurchasedProductInstance(
          ProductPath("alcohol/beer"),
          "iid0",
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          Some(true)
        )
      )
    }
  }

  "UserInformation" should {
    "return the PreUserInformation as UserInformation when calling .build" in {
      val userInformation = UserInformation(
        testUserInfo.nameForm.firstName,
        testUserInfo.nameForm.lastName,
        testUserInfo.identification.map(_.identificationType).getOrElse(""),
        testUserInfo.identification.flatMap(_.identificationNumber).getOrElse(""),
        testUserInfo.emailAddress.getOrElse(""),
        testUserInfo.arrivalForm.map(_.selectPlaceOfArrival).getOrElse(""),
        testUserInfo.arrivalForm.map(_.enterPlaceOfArrival).getOrElse(""),
        testUserInfo.arrivalForm
          .map(arrival => parseLocalDate(arrival.dateOfArrival.toString))
          .getOrElse(LocalDate.now()),
        testUserInfo.arrivalForm
          .map(arrival => parseLocalTime(arrival.timeOfArrival.toString))
          .getOrElse(LocalTime.now())
      )

      UserInformation.build(testUserInfo) shouldEqual userInformation
    }
  }

  "UserInformation" should {

    val userInformation: UserInformation = UserInformation(
      firstName = "firstName",
      lastName = "LastName",
      identificationType = "identificationType",
      identificationNumber = "identificationNumber",
      emailAddress = "email",
      selectPlaceOfArrival = "LHR",
      enterPlaceOfArrival = "enterPlaceOfArrival",
      dateOfArrival = parseLocalDate("2018-11-12"),
      timeOfArrival = parseLocalTime("12:20 pm")
    )

    val json = Json.obj(
      "firstName"            -> "firstName",
      "lastName"             -> "LastName",
      "identificationType"   -> "identificationType",
      "identificationNumber" -> "identificationNumber",
      "emailAddress"         -> "email",
      "selectPlaceOfArrival" -> "LHR",
      "enterPlaceOfArrival"  -> "enterPlaceOfArrival",
      "dateOfArrival"        -> parseLocalDate("2018-11-12"),
      "timeOfArrival"        -> parseLocalTime("12:20 pm")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(userInformation) shouldBe json
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        json.validate[UserInformation] shouldBe JsSuccess(userInformation)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[UserInformation] shouldBe a[JsError]
      }
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[UserInformation].isError mustBe true
    }
  }

  "PreUserInformation" should {
    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(testUserInfo) shouldBe Json.obj(
          "nameForm"       -> Json.obj(
            "firstName" -> "firstName",
            "lastName"  -> "lastName"
          ),
          "identification" -> Json.obj(
            "identificationType"   -> "identificationType",
            "identificationNumber" -> "identificationNumber"
          ),
          "emailAddress"   -> "emailAddress",
          "arrivalForm"    -> Json.obj(
            "selectPlaceOfArrival" -> "LHR",
            "enterPlaceOfArrival"  -> "enterPlaceOfArrival",
            "dateOfArrival"        -> parseLocalDate("2018-11-12"),
            "timeOfArrival"        -> parseLocalTime("12:20 pm")
          )
        )
      }

      "an optional field is blank" in {
        val userInformationWithNoPlaceOfArrival: PreUserInformation = testUserInfo.copy(arrivalForm = None)

        Json.toJson(userInformationWithNoPlaceOfArrival) shouldBe Json.obj(
          "nameForm"       -> Json.obj(
            "firstName" -> "firstName",
            "lastName"  -> "lastName"
          ),
          "identification" -> Json.obj(
            "identificationType"   -> "identificationType",
            "identificationNumber" -> "identificationNumber"
          ),
          "emailAddress"   -> "emailAddress"
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "nameForm"       -> Json.obj(
            "firstName" -> "firstName",
            "lastName"  -> "lastName"
          ),
          "identification" -> Json.obj(
            "identificationType"   -> "identificationType",
            "identificationNumber" -> "identificationNumber"
          ),
          "emailAddress"   -> "emailAddress",
          "arrivalForm"    -> Json.obj(
            "selectPlaceOfArrival" -> "LHR",
            "enterPlaceOfArrival"  -> "enterPlaceOfArrival",
            "dateOfArrival"        -> parseLocalDate("2018-11-12"),
            "timeOfArrival"        -> parseLocalTime("12:20 pm")
          )
        )

        json.validate[PreUserInformation] shouldBe JsSuccess(testUserInfo)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[PreviousDeclarationRequest] shouldBe a[JsError]
      }
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[PreviousDeclarationRequest].isError mustBe true
    }

    "return the PreUserInformation from WhatIsYourNameDto when calling .fromWhatIsYourNameDto" in {
      val whatIsYourNameDto  = WhatIsYourNameDto("firstName", "lastName")
      val preUserInformation = PreUserInformation(
        nameForm = WhatIsYourNameForm(
          whatIsYourNameDto.firstName,
          whatIsYourNameDto.lastName
        )
      )

      PreUserInformation.fromWhatIsYourNameDto(whatIsYourNameDto) shouldEqual preUserInformation
    }

    "return the UserInformation when calling .getBasicUserInfo" in {
      val preUserInformation = Some(
        testUserInfo
      )

      PreUserInformation.getBasicUserInfo(preUserInformation) shouldEqual UserInformation(
        firstName = testUserInfo.nameForm.firstName,
        lastName = testUserInfo.nameForm.lastName,
        identificationType = testUserInfo.identification.map(_.identificationType).getOrElse(""),
        identificationNumber = testUserInfo.identification.flatMap(_.identificationNumber).getOrElse(""),
        emailAddress = testUserInfo.emailAddress.getOrElse(""),
        selectPlaceOfArrival = testUserInfo.arrivalForm.map(_.selectPlaceOfArrival).getOrElse(""),
        enterPlaceOfArrival = testUserInfo.arrivalForm.map(_.enterPlaceOfArrival).getOrElse(""),
        dateOfArrival = testUserInfo.arrivalForm.map(_.dateOfArrival).getOrElse(LocalDate.now()),
        timeOfArrival = testUserInfo.arrivalForm.map(_.timeOfArrival).getOrElse(LocalTime.now())
      )
    }
  }

  "PreviousDeclarationRequest" should {
    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(previousDeclarationRequest) shouldBe Json.obj(
          "lastName"        -> "lastName",
          "referenceNumber" -> "referenceNumber"
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "lastName"        -> "lastName",
          "referenceNumber" -> "referenceNumber"
        )
        json.validate[PreviousDeclarationRequest] shouldBe JsSuccess(previousDeclarationRequest)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[PreviousDeclarationRequest] shouldBe a[JsError]
      }
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[PreviousDeclarationRequest].isError mustBe true
    }
  }

  "ProductAlias" should {

    val productAlias: ProductAlias = ProductAlias(
      term = "term",
      productPath = ProductPath("product/path")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(productAlias) shouldBe Json.obj(
          "term"        -> "term",
          "productPath" -> ProductPath("product/path")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "term"        -> "term",
          "productPath" -> ProductPath("product/path")
        )
        json.validate[ProductAlias] shouldBe JsSuccess(productAlias)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[ProductAlias] shouldBe a[JsError]
      }
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[ProductAlias].isError mustBe true
    }
  }

  "WhatIsYourNameForm" should {

    val whatIsYourNameForm: WhatIsYourNameForm = WhatIsYourNameForm(
      firstName = "firstName",
      lastName = "lastName"
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(whatIsYourNameForm) shouldBe Json.obj(
          "firstName" -> "firstName",
          "lastName"  -> "lastName"
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "firstName" -> "firstName",
          "lastName"  -> "lastName"
        )
        json.validate[WhatIsYourNameForm] shouldBe JsSuccess(whatIsYourNameForm)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[WhatIsYourNameForm] shouldBe a[JsError]
      }
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[WhatIsYourNameForm].isError mustBe true
    }
  }

  "ArrivalForm" should {

    val arrivalForm: ArrivalForm = ArrivalForm(
      selectPlaceOfArrival = "LHR",
      enterPlaceOfArrival = "enterPlaceOfArrival",
      dateOfArrival = parseLocalDate("2018-11-12"),
      timeOfArrival = parseLocalTime("12:20 pm")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(arrivalForm) shouldBe Json.obj(
          "selectPlaceOfArrival" -> "LHR",
          "enterPlaceOfArrival"  -> "enterPlaceOfArrival",
          "dateOfArrival"        -> parseLocalDate("2018-11-12"),
          "timeOfArrival"        -> parseLocalTime("12:20 pm")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "selectPlaceOfArrival" -> "LHR",
          "enterPlaceOfArrival"  -> "enterPlaceOfArrival",
          "dateOfArrival"        -> parseLocalDate("2018-11-12"),
          "timeOfArrival"        -> parseLocalTime("12:20 pm")
        )
        json.validate[ArrivalForm] shouldBe JsSuccess(arrivalForm)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[ArrivalForm] shouldBe a[JsError]
      }

      "invalid valid types" in {
        val json = Json.obj(
          "selectPlaceOfArrival" -> 0,
          "enterPlaceOfArrival"  -> "enterPlaceOfArrival",
          "dateOfArrival"        -> parseLocalDate("2018-11-12"),
          "timeOfArrival"        -> parseLocalTime("12:20 pm")
        )
        json.validate[ArrivalForm] shouldBe a[JsError]
      }
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[ArrivalForm].isError mustBe true
    }
  }

  "IdentificationForm" should {

    val identificationForm: IdentificationForm = IdentificationForm(
      identificationType = "identificationType",
      identificationNumber = Some("1000000")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(identificationForm) shouldBe Json.obj(
          "identificationType"   -> "identificationType",
          "identificationNumber" -> Some("1000000")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "identificationType"   -> "identificationType",
          "identificationNumber" -> Some("1000000")
        )
        json.validate[IdentificationForm] shouldBe JsSuccess(identificationForm)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[IdentificationForm] shouldBe a[JsError]
      }
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[IdentificationForm].isError mustBe true
    }
  }

  "PaymentNotification" should {

    val paymentNotification: PaymentNotification = PaymentNotification(
      status = "status",
      reference = "reference"
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(paymentNotification) shouldBe Json.obj(
          "status"    -> "status",
          "reference" -> "reference"
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "status"    -> "status",
          "reference" -> "reference"
        )
        json.validate[PaymentNotification] shouldBe JsSuccess(paymentNotification)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[PaymentNotification] shouldBe a[JsError]
      }
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[PaymentNotification].isError mustBe true
    }
  }

  "JourneyData" should {

    val calculatorResponse: CalculatorResponse = CalculatorResponse(
      Some(
        Alcohol(
          List(
            Band(
              "B",
              List(
                Item(
                  "ALC/A1/CIDER",
                  "1.00",
                  None,
                  Some(5),
                  Calculation("1.00", "1.00", "1.00", "300.00"),
                  Metadata(
                    "5 litres cider",
                    "Cider",
                    "1.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("GBP", "Great British Pounds (GBP)", Some("GBP"), Nil),
                    Country("UK", "UK", "UK", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("1.00", "1.00", "1.00", "300.00")
            )
          ),
          Calculation("1.00", "1.00", "1.00", "300.00")
        )
      ),
      Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
      Calculation("1.00", "1.00", "1.00", "300.00"),
      withinFreeAllowance = false,
      limits = Map.empty,
      isAnyItemOverAllowance = false
    )

    val journeyData: JourneyData = JourneyData(
      prevDeclaration = Some(false),
      euCountryCheck = Some("euOnly"),
      arrivingNICheck = Some(true),
      isUKVatPaid = Some(false),
      isUKVatExcisePaid = Some(true),
      isUKResident = Some(true),
      isUccRelief = Some(false),
      isVatResClaimed = Some(false),
      isBringingDutyFree = Some(true),
      bringingOverAllowance = Some(true),
      privateCraft = Some(false),
      ageOver17 = Some(true),
      irishBorder = Some(false),
      selectedAliases = List(ProductAlias("Book", ProductPath("other-goods/books"))),
      purchasedProductInstances = List(
        PurchasedProductInstance(
          path = ProductPath("alcohol/beer"),
          iid = "iid0",
          weightOrVolume = Some(20.0),
          noOfSticks = Some(1),
          country =
            Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
          originCountry =
            Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
          currency = Some("EUR"),
          cost = Some(BigDecimal(12.99)),
          isCustomPaid = Some(false),
          isExcisePaid = Some(false),
          isUccRelief = Some(false),
          hasEvidence = Some(false),
          isEditable = Some(true)
        )
      ),
      workingInstance = Some(PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"))),
      preUserInformation = Some(testUserInfo),
      calculatorResponse = Some(calculatorResponse),
      chargeReference = Some("chargeReference"),
      defaultCountry = Some("FR"),
      defaultOriginCountry = Some("FR"),
      defaultCurrency = Some("EUR"),
      previousDeclarationRequest = Some(previousDeclarationRequest),
      declarationResponse = Some(declarationResponse),
      deltaCalculation = Some(Calculation("0.00", "0.00", "0.00", "0.00")),
      amendmentCount = Some(0),
      pendingPayment = Some(false),
      amendState = Some("amendState")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(journeyData) shouldBe Json.obj(
          "prevDeclaration"            -> Some(false),
          "euCountryCheck"             -> Some("euOnly"),
          "arrivingNICheck"            -> Some(true),
          "isUKVatPaid"                -> Some(false),
          "isUKVatExcisePaid"          -> Some(true),
          "isUKResident"               -> Some(true),
          "isUccRelief"                -> Some(false),
          "isVatResClaimed"            -> Some(false),
          "isBringingDutyFree"         -> Some(true),
          "bringingOverAllowance"      -> Some(true),
          "privateCraft"               -> Some(false),
          "ageOver17"                  -> Some(true),
          "irishBorder"                -> Some(false),
          "selectedAliases"            -> List(ProductAlias("Book", ProductPath("other-goods/books"))),
          "purchasedProductInstances"  -> List(
            PurchasedProductInstance(
              path = ProductPath("alcohol/beer"),
              iid = "iid0",
              weightOrVolume = Some(20.0),
              noOfSticks = Some(1),
              country =
                Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
              originCountry =
                Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
              currency = Some("EUR"),
              cost = Some(BigDecimal(12.99)),
              isCustomPaid = Some(false),
              isExcisePaid = Some(false),
              isUccRelief = Some(false),
              hasEvidence = Some(false),
              isEditable = Some(true)
            )
          ),
          "workingInstance"            -> Some(PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"))),
          "preUserInformation"         -> Some(testUserInfo),
          "calculatorResponse"         -> Some(calculatorResponse),
          "chargeReference"            -> Some("chargeReference"),
          "defaultCountry"             -> Some("FR"),
          "defaultOriginCountry"       -> Some("FR"),
          "defaultCurrency"            -> Some("EUR"),
          "previousDeclarationRequest" -> Some(previousDeclarationRequest),
          "declarationResponse"        -> Some(declarationResponse),
          "deltaCalculation"           -> Some(Calculation("0.00", "0.00", "0.00", "0.00")),
          "amendmentCount"             -> Some(0),
          "pendingPayment"             -> Some(false),
          "amendState"                 -> Some("amendState")
        )
      }

      "optional fields are missing" in {
        val journeyDataWithMissingFields: JourneyData = JourneyData(
          selectedAliases = List(ProductAlias("Book", ProductPath("other-goods/books"))),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              path = ProductPath("alcohol/beer"),
              iid = "iid0",
              weightOrVolume = Some(20.0),
              noOfSticks = Some(1),
              country =
                Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
              originCountry =
                Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
              currency = Some("EUR"),
              cost = Some(BigDecimal(12.99)),
              isCustomPaid = Some(false),
              isExcisePaid = Some(false),
              isUccRelief = Some(false),
              hasEvidence = Some(false),
              isEditable = Some(true)
            )
          )
        )

        val json = Json.obj(
          "selectedAliases"           -> List(ProductAlias("Book", ProductPath("other-goods/books"))),
          "purchasedProductInstances" -> List(
            PurchasedProductInstance(
              path = ProductPath("alcohol/beer"),
              iid = "iid0",
              weightOrVolume = Some(20.0),
              noOfSticks = Some(1),
              country =
                Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
              originCountry =
                Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
              currency = Some("EUR"),
              cost = Some(BigDecimal(12.99)),
              isCustomPaid = Some(false),
              isExcisePaid = Some(false),
              isUccRelief = Some(false),
              hasEvidence = Some(false),
              isEditable = Some(true)
            )
          )
        )

        Json.toJson(journeyDataWithMissingFields) shouldBe json
      }

      "all fields are empty" in {
        val journeyDataWithEmptyFields: JourneyData = JourneyData()

        Json.toJson(journeyDataWithEmptyFields) shouldBe Json.obj(
          "selectedAliases"           -> List.empty[ProductAlias],
          "purchasedProductInstances" -> List.empty[PurchasedProductInstance]
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "prevDeclaration"            -> Some(false),
          "euCountryCheck"             -> Some("euOnly"),
          "arrivingNICheck"            -> Some(true),
          "isUKVatPaid"                -> Some(false),
          "isUKVatExcisePaid"          -> Some(true),
          "isUKResident"               -> Some(true),
          "isUccRelief"                -> Some(false),
          "isVatResClaimed"            -> Some(false),
          "isBringingDutyFree"         -> Some(true),
          "bringingOverAllowance"      -> Some(true),
          "privateCraft"               -> Some(false),
          "ageOver17"                  -> Some(true),
          "irishBorder"                -> Some(false),
          "selectedAliases"            -> List(ProductAlias("Book", ProductPath("other-goods/books"))),
          "purchasedProductInstances"  -> List(
            PurchasedProductInstance(
              path = ProductPath("alcohol/beer"),
              iid = "iid0",
              weightOrVolume = Some(20.0),
              noOfSticks = Some(1),
              country =
                Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
              originCountry =
                Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
              currency = Some("EUR"),
              cost = Some(BigDecimal(12.99)),
              isCustomPaid = Some(false),
              isExcisePaid = Some(false),
              isUccRelief = Some(false),
              hasEvidence = Some(false),
              isEditable = Some(true)
            )
          ),
          "workingInstance"            -> Some(PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"))),
          "preUserInformation"         -> Some(testUserInfo),
          "calculatorResponse"         -> Some(calculatorResponse),
          "chargeReference"            -> Some("chargeReference"),
          "defaultCountry"             -> Some("FR"),
          "defaultOriginCountry"       -> Some("FR"),
          "defaultCurrency"            -> Some("EUR"),
          "previousDeclarationRequest" -> Some(previousDeclarationRequest),
          "declarationResponse"        -> Some(declarationResponse),
          "deltaCalculation"           -> Some(Calculation("0.00", "0.00", "0.00", "0.00")),
          "amendmentCount"             -> Some(0),
          "pendingPayment"             -> Some(false),
          "amendState"                 -> Some("amendState")
        )
        json.validate[JourneyData] shouldBe JsSuccess(journeyData)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[JourneyData] shouldBe JsSuccess[JourneyData](
          JourneyData(
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            List(),
            List(),
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None
          )
        )
      }
    }
  }

  "JourneyDataDownstream" should {

    val calculatorResponse: CalculatorResponse = CalculatorResponse(
      Some(
        Alcohol(
          List(
            Band(
              "B",
              List(
                Item(
                  "ALC/A1/CIDER",
                  "1.00",
                  None,
                  Some(5),
                  Calculation("1.00", "1.00", "1.00", "300.00"),
                  Metadata(
                    "5 litres cider",
                    "Cider",
                    "1.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("GBP", "Great British Pounds (GBP)", Some("GBP"), Nil),
                    Country("UK", "UK", "UK", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("1.00", "1.00", "1.00", "300.00")
            )
          ),
          Calculation("1.00", "1.00", "1.00", "300.00")
        )
      ),
      Some(Tobacco(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(OtherGoods(Nil, Calculation("0.00", "0.00", "0.00", "0.00"))),
      Calculation("1.00", "1.00", "1.00", "300.00"),
      withinFreeAllowance = false,
      limits = Map.empty,
      isAnyItemOverAllowance = false
    )

    val journeyDataDownstream: JourneyDataDownstream = JourneyDataDownstream(
      prevDeclaration = Some(false),
      euCountryCheck = Some("euOnly"),
      arrivingNICheck = Some(true),
      isUKVatPaid = Some(false),
      isUKVatExcisePaid = Some(true),
      isUKResident = Some(true),
      isUccRelief = Some(false),
      isVatResClaimed = Some(false),
      isBringingDutyFree = Some(true),
      bringingOverAllowance = Some(true),
      privateCraft = Some(false),
      ageOver17 = Some(true),
      irishBorder = Some(false),
      selectedAliases = List(ProductAlias("Book", ProductPath("other-goods/books"))),
      purchasedProductInstances = List(
        PurchasedProductInstance(
          path = ProductPath("alcohol/beer"),
          iid = "iid0",
          weightOrVolume = Some(20.0),
          noOfSticks = Some(1),
          country =
            Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
          originCountry =
            Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
          currency = Some("EUR"),
          cost = Some(BigDecimal(12.99)),
          isCustomPaid = Some(false),
          isExcisePaid = Some(false),
          isUccRelief = Some(false),
          hasEvidence = Some(false),
          isEditable = Some(true)
        )
      ),
      workingInstance = Some(PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"))),
      calculatorResponse = Some(calculatorResponse),
      chargeReference = Some("chargeReference"),
      defaultCountry = Some("FR"),
      defaultOriginCountry = Some("FR"),
      defaultCurrency = Some("EUR"),
      previousDeclarationRequest = Some(previousDeclarationRequest),
      declarationResponse = Some(declarationResponse),
      deltaCalculation = Some(Calculation("0.00", "0.00", "0.00", "0.00")),
      amendmentCount = Some(0),
      pendingPayment = Some(false),
      amendState = Some("amendState")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(journeyDataDownstream) shouldBe Json.obj(
          "prevDeclaration"            -> Some(false),
          "euCountryCheck"             -> Some("euOnly"),
          "arrivingNICheck"            -> Some(true),
          "isUKVatPaid"                -> Some(false),
          "isUKVatExcisePaid"          -> Some(true),
          "isUKResident"               -> Some(true),
          "isUccRelief"                -> Some(false),
          "isVatResClaimed"            -> Some(false),
          "isBringingDutyFree"         -> Some(true),
          "bringingOverAllowance"      -> Some(true),
          "privateCraft"               -> Some(false),
          "ageOver17"                  -> Some(true),
          "irishBorder"                -> Some(false),
          "selectedAliases"            -> List(ProductAlias("Book", ProductPath("other-goods/books"))),
          "purchasedProductInstances"  -> List(
            PurchasedProductInstance(
              path = ProductPath("alcohol/beer"),
              iid = "iid0",
              weightOrVolume = Some(20.0),
              noOfSticks = Some(1),
              country =
                Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
              originCountry =
                Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
              currency = Some("EUR"),
              cost = Some(BigDecimal(12.99)),
              isCustomPaid = Some(false),
              isExcisePaid = Some(false),
              isUccRelief = Some(false),
              hasEvidence = Some(false),
              isEditable = Some(true)
            )
          ),
          "workingInstance"            -> Some(PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"))),
          "calculatorResponse"         -> Some(calculatorResponse),
          "chargeReference"            -> Some("chargeReference"),
          "defaultCountry"             -> Some("FR"),
          "defaultOriginCountry"       -> Some("FR"),
          "defaultCurrency"            -> Some("EUR"),
          "previousDeclarationRequest" -> Some(previousDeclarationRequest),
          "declarationResponse"        -> Some(declarationResponse),
          "deltaCalculation"           -> Some(Calculation("0.00", "0.00", "0.00", "0.00")),
          "amendmentCount"             -> Some(0),
          "pendingPayment"             -> Some(false),
          "amendState"                 -> Some("amendState")
        )
      }

      "optional fields are missing" in {
        val journeyDataDownstreamWithMissingFields: JourneyDataDownstream = JourneyDataDownstream(
          selectedAliases = List(ProductAlias("Book", ProductPath("other-goods/books"))),
          purchasedProductInstances = List(
            PurchasedProductInstance(
              path = ProductPath("alcohol/beer"),
              iid = "iid0",
              weightOrVolume = Some(20.0),
              noOfSticks = Some(1),
              country =
                Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
              originCountry =
                Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
              currency = Some("EUR"),
              cost = Some(BigDecimal(12.99)),
              isCustomPaid = Some(false),
              isExcisePaid = Some(false),
              isUccRelief = Some(false),
              hasEvidence = Some(false),
              isEditable = Some(true)
            )
          )
        )

        val json = Json.obj(
          "selectedAliases"           -> List(ProductAlias("Book", ProductPath("other-goods/books"))),
          "purchasedProductInstances" -> List(
            PurchasedProductInstance(
              path = ProductPath("alcohol/beer"),
              iid = "iid0",
              weightOrVolume = Some(20.0),
              noOfSticks = Some(1),
              country =
                Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
              originCountry =
                Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
              currency = Some("EUR"),
              cost = Some(BigDecimal(12.99)),
              isCustomPaid = Some(false),
              isExcisePaid = Some(false),
              isUccRelief = Some(false),
              hasEvidence = Some(false),
              isEditable = Some(true)
            )
          )
        )

        Json.toJson(journeyDataDownstreamWithMissingFields) shouldBe json
      }

      "all fields are empty" in {
        val journeyDataWithEmptyFields: JourneyDataDownstream = JourneyDataDownstream()

        Json.toJson(journeyDataWithEmptyFields) shouldBe Json.obj(
          "selectedAliases"           -> List.empty[ProductAlias],
          "purchasedProductInstances" -> List.empty[PurchasedProductInstance]
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "prevDeclaration"            -> Some(false),
          "euCountryCheck"             -> Some("euOnly"),
          "arrivingNICheck"            -> Some(true),
          "isUKVatPaid"                -> Some(false),
          "isUKVatExcisePaid"          -> Some(true),
          "isUKResident"               -> Some(true),
          "isUccRelief"                -> Some(false),
          "isVatResClaimed"            -> Some(false),
          "isBringingDutyFree"         -> Some(true),
          "bringingOverAllowance"      -> Some(true),
          "privateCraft"               -> Some(false),
          "ageOver17"                  -> Some(true),
          "irishBorder"                -> Some(false),
          "selectedAliases"            -> List(ProductAlias("Book", ProductPath("other-goods/books"))),
          "purchasedProductInstances"  -> List(
            PurchasedProductInstance(
              path = ProductPath("alcohol/beer"),
              iid = "iid0",
              weightOrVolume = Some(20.0),
              noOfSticks = Some(1),
              country =
                Some(Country("FR", "title.france", "FR", isEu = true, isCountry = true, List("USA", "US", "American"))),
              originCountry =
                Some(Country("IT", "title.italy", "IT", isEu = true, isCountry = true, List("USA", "US", "American"))),
              currency = Some("EUR"),
              cost = Some(BigDecimal(12.99)),
              isCustomPaid = Some(false),
              isExcisePaid = Some(false),
              isUccRelief = Some(false),
              hasEvidence = Some(false),
              isEditable = Some(true)
            )
          ),
          "workingInstance"            -> Some(PurchasedProductInstance(iid = "iid", path = ProductPath("alcohol"))),
          "preUserInformation"         -> Some(testUserInfo),
          "calculatorResponse"         -> Some(calculatorResponse),
          "chargeReference"            -> Some("chargeReference"),
          "defaultCountry"             -> Some("FR"),
          "defaultOriginCountry"       -> Some("FR"),
          "defaultCurrency"            -> Some("EUR"),
          "previousDeclarationRequest" -> Some(previousDeclarationRequest),
          "declarationResponse"        -> Some(declarationResponse),
          "deltaCalculation"           -> Some(Calculation("0.00", "0.00", "0.00", "0.00")),
          "amendmentCount"             -> Some(0),
          "pendingPayment"             -> Some(false),
          "amendState"                 -> Some("amendState")
        )
        json.validate[JourneyDataDownstream] shouldBe JsSuccess(journeyDataDownstream)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[JourneyDataDownstream] shouldBe JsSuccess[JourneyDataDownstream](
          JourneyDataDownstream(
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            List(),
            List(),
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None
          )
        )
      }
    }
  }
}
