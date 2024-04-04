/*
 * Copyright 2024 HM Revenue & Customs
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

import config.AppConfig
import controllers._
import forms._
import models._
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{Lang, MessagesApi}
import play.twirl.api.Html
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import views.html._
import views.html.alcohol.alcohol_input
import views.html.amendments._
import views.html.declaration._
import views.html.errors.purchase_price_out_of_bounds
import views.html.other_goods.other_goods_input
import views.html.purchased_products._
import views.html.templates.GovukLayoutWrapper
import views.html.tobacco.tobacco_input
import views.html.travel_details._

import java.time.LocalDateTime

class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec with AccessibilityConstants {

  private val appConfig: AppConfig                                 = app.injector.instanceOf[AppConfig]
  private val alcoholInputForm: AlcoholInputForm                   = app.injector.instanceOf[AlcoholInputForm]
  private val tobaccoInputForm: TobaccoInputForm                   = app.injector.instanceOf[TobaccoInputForm]
  private val otherGoodsInputController: OtherGoodsInputController = app.injector.instanceOf[OtherGoodsInputController]
  private val messagesApi: MessagesApi                             = app.injector.instanceOf[MessagesApi]

  private val booleanForm: Form[Boolean]          = Form("value" -> boolean)
  private val alcoholForm: Form[AlcoholDto]       = alcoholInputForm.resilientForm
  private val tobaccoForm: Form[TobaccoDto]       = tobaccoInputForm.resilientForm
  private val otherGoodsForm: Form[OtherGoodsDto] = otherGoodsInputController.addCostForm

  override implicit val arbAsciiString: Arbitrary[String]           = fixed("1.20")
  implicit val arbNamesAndTokens: Arbitrary[List[(String, String)]] = fixed(namesAndTokens)
  implicit val arbAppConfig: Arbitrary[AppConfig]                   = fixed(appConfig)
  implicit val arbMessagesApi: Arbitrary[MessagesApi]               = fixed(messagesApi)
  implicit val arbCurrencies: Arbitrary[List[Currency]]             = fixed(currencies)
  implicit val arbCountries: Arbitrary[List[Country]]               = fixed(countries)
  implicit val arbPortsOfArrival: Arbitrary[List[PortsOfArrival]]   = fixed(portsOfArrival)
  implicit val arbUserInformation: Arbitrary[UserInformation]       = fixed(userInformation)
  implicit val arbJourneyData: Arbitrary[JourneyData]               = fixed(JourneyData())
  implicit val arbHtml: Arbitrary[Html]                             = fixed(Html(""))
  implicit val arbLang: Arbitrary[Lang]                             = fixed(Lang("en"))
  implicit val arbPurchasedItems: Arbitrary[List[PurchasedItem]]    = fixed(purchasedItems)

  implicit val arbBooleanForm: Arbitrary[Form[Boolean]]          = fixed(booleanForm)
  implicit val arbAlcoholForm: Arbitrary[Form[AlcoholDto]]       = fixed(alcoholForm)
  implicit val arbTobaccoForm: Arbitrary[Form[TobaccoDto]]       = fixed(tobaccoForm)
  implicit val arbOtherGoodsForm: Arbitrary[Form[OtherGoodsDto]] = fixed(otherGoodsForm)

  implicit val arbPrivateCraftForm: Arbitrary[Form[PrivateCraftDto]]                   = fixed(PrivateCraftDto.form)
  implicit val arbIrishBorderForm: Arbitrary[Form[IrishBorderDto]]                     = fixed(IrishBorderDto.form)
  implicit val arbBringingOverAllowanceForm: Arbitrary[Form[BringingOverAllowanceDto]] = fixed(
    BringingOverAllowanceDto.form
  )
  implicit val arbEuCountryCheckForm: Arbitrary[Form[EuCountryCheckDto]]               = fixed(EuCountryCheckDto.form)
  implicit val arbClaimedVatResForm: Arbitrary[Form[ClaimedVatResDto]]                 = fixed(ClaimedVatResDto.form)
  implicit val arbConfirmAgeForm: Arbitrary[Form[AgeOver17Dto]]                        = fixed(AgeOver17Dto.form)
  implicit val arbBringingDutyFreeForm: Arbitrary[Form[BringingDutyFreeDto]]           = fixed(BringingDutyFreeDto.form)
  implicit val arbConfirmRemoveForm: Arbitrary[Form[ConfirmRemoveDto]]                 = fixed(ConfirmRemoveDto.form)
  implicit val arbDecRetrievalForm: Arbitrary[Form[DeclarationRetrievalDto]]           = fixed(
    DeclarationRetrievalDto.form()
  )
  implicit val arbEnterYourDetailsForm: Arbitrary[Form[EnterYourDetailsDto]]           = fixed(
    EnterYourDetailsDto.form(LocalDateTime.now())
  )
  implicit val arbSelectProductsForm: Arbitrary[Form[SelectProductsDto]]               = fixed(SelectProductsDto.form)

  override def viewPackageName: String = "views.html"

  override def layoutClasses: Seq[Class[GovukLayoutWrapper]] = Seq(classOf[GovukLayoutWrapper])

  override def renderViewByClass: PartialFunction[Any, Html] = {
    case errorTemplate: errorTemplate                                           => render(errorTemplate)
    case timeOut: timeOut                                                       => render(timeOut)
    // alcohol
    case alcohol_input: alcohol_input                                           => render(alcohol_input)
    // amendments
    case declaration_not_found: declaration_not_found                           => render(declaration_not_found)
    case declaration_retrieval: declaration_retrieval                           => render(declaration_retrieval)
    case no_further_amendment: no_further_amendment                             => render(no_further_amendment)
    case pending_payment: pending_payment                                       => render(pending_payment)
    case previous_declaration: previous_declaration                             => render(previous_declaration)
    // declaration
    case declare_your_goods: declare_your_goods                                 => render(declare_your_goods)
    case enter_your_details: enter_your_details                                 => render(enter_your_details)
    case zero_declaration: zero_declaration                                     =>
      implicit val arbCalculatorResponseDto: Arbitrary[CalculatorResponseDto] = fixed(calculatorResponseDto)
      implicit val arbCalculatorResponse: Arbitrary[CalculatorResponse]       = fixed(calculatorResponse)
      render(zero_declaration)
    case zero_to_declare_your_goods: zero_to_declare_your_goods                 => render(zero_to_declare_your_goods)
    // errors
    case purchase_price_out_of_bounds: purchase_price_out_of_bounds             => render(purchase_price_out_of_bounds)
    // other_goods
    case other_goods_input: other_goods_input                                   => render(other_goods_input)
    // purchased_products
    case dashboard: dashboard                                                   => render(dashboard)
    case done: done                                                             => render(done)
    case limit_exceed_add: limit_exceed_add                                     => render(limit_exceed_add)
    case limit_exceed_edit: limit_exceed_edit                                   => render(limit_exceed_edit)
    case nothing_to_declare: nothing_to_declare                                 => render(nothing_to_declare)
    case over_ninety_seven_thousand_pounds: over_ninety_seven_thousand_pounds   =>
      render(over_ninety_seven_thousand_pounds)
    case select_products: select_products                                       => render(select_products)
    case remove: remove                                                         => render(remove)
    case zero_to_declare: zero_to_declare                                       => render(zero_to_declare)
    // tobacco
    case tobacco_input: tobacco_input                                           => render(tobacco_input)
    // travel_details
    case arriving_ni: arriving_ni                                               => render(arriving_ni)
    case bringing_duty_free_question: bringing_duty_free_question               => render(bringing_duty_free_question)
    case confirm_age: confirm_age                                               => render(confirm_age)
    case did_you_claim_tax_back: did_you_claim_tax_back                         => render(did_you_claim_tax_back)
    case duty_free_allowance_question_eu: duty_free_allowance_question_eu       => render(duty_free_allowance_question_eu)
    case duty_free_allowance_question_mix: duty_free_allowance_question_mix     => render(duty_free_allowance_question_mix)
    case eu_country_check: eu_country_check                                     => render(eu_country_check)
    case eu_evidence_item: eu_evidence_item                                     => render(eu_evidence_item)
    case goods_bought_inside_and_outside_eu: goods_bought_inside_and_outside_eu =>
      render(goods_bought_inside_and_outside_eu)
    case goods_bought_inside_eu: goods_bought_inside_eu                         => render(goods_bought_inside_eu)
    case goods_bought_outside_eu: goods_bought_outside_eu                       => render(goods_bought_outside_eu)
    case irish_border: irish_border                                             => render(irish_border)
    case no_need_to_use_service: no_need_to_use_service                         => render(no_need_to_use_service)
    case no_need_to_use_service_gbni: no_need_to_use_service_gbni               => render(no_need_to_use_service_gbni)
    case private_travel: private_travel                                         => render(private_travel)
    case ucc_relief_item: ucc_relief_item                                       => render(ucc_relief_item)
    case uk_resident: uk_resident                                               => render(uk_resident)
    case ukexcise_paid: ukexcise_paid                                           => render(ukexcise_paid)
    case ukexcise_paid_item: ukexcise_paid_item                                 => render(ukexcise_paid_item)
    case ukvat_paid_item: ukvat_paid_item                                       => render(ukvat_paid_item)
  }

  runAccessibilityTests()
}
