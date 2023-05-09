import config.AppConfig
import models.{AgeOver17Dto, AlcoholDto, BringingDutyFreeDto, BringingOverAllowanceDto, ClaimedVatResDto, DeclarationRetrievalDto, EnterYourDetailsDto, EuCountryCheckDto, IrishBorderDto, JourneyData, OtherGoodsDto, OtherGoodsSearchItem, PrivateCraftDto, ProductPath, SelectProductsDto, TobaccoDto, UserInformation}
import org.joda.time.DateTime
import org.scalacheck.Arbitrary
import play.api.Application
import play.api.data.Form
import play.api.data.Forms._
import play.api.inject.guice.GuiceApplicationBuilder
import play.twirl.api.Html
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import views.html._
import views.html.alcohol.alcohol_input
import views.html.amendments._
import views.html.declaration._
import views.html.errors.purchase_price_out_of_bounds
import views.html.other_goods.other_goods_input
import views.html.purchased_products._
import views.html.tobacco.{no_of_sticks_input, no_of_sticks_weight_or_volume_input, tobacco_input, weight_or_volume_input}
import views.html.travel_details._

import scala.util.Try

class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {
  // If you wish to override the GuiceApplicationBuilder to provide additional
  // config for your service, you can do that by overriding fakeApplication
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure()
      .build()

  // Some view template parameters can't be completely arbitrary,
  // but need to have sane values for pages to render properly.
  // eg. if there is validation or conditional logic in the twirl template.
  // These can be provided by calling `fixed()` to wrap an existing concrete value.
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val arbConfig: Arbitrary[AppConfig] = fixed(appConfig)

  val booleanForm: Form[Boolean] = Form("value" -> boolean)
  implicit val arbForm: Arbitrary[Form[Boolean]] = fixed(booleanForm)

  val alcoholForm: Form[AlcoholDto] = Form(
    mapping(
      "weightOrVolume" -> optional(text)
        .transform[BigDecimal](_.flatMap(x => Try(BigDecimal(x)).toOption).getOrElse(0), _ => None),
      "country"        -> ignored(""),
      "originCountry"  -> optional(text),
      "currency"       -> ignored(""),
      "cost"           -> ignored(BigDecimal(0)),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(AlcoholDto.apply)(AlcoholDto.unapply)
  )
  implicit val arbAlcoholInput: Arbitrary[Form[AlcoholDto]] = fixed(alcoholForm)

  implicit val arbDecRetrievalInput: Arbitrary[Form[DeclarationRetrievalDto]] = fixed(DeclarationRetrievalDto.form())

  implicit val arbEnterYourDetailsInput: Arbitrary[Form[EnterYourDetailsDto]] = fixed(EnterYourDetailsDto.form(DateTime.now()))

  val otherGoodsForm: Form[OtherGoodsDto] = Form(
    mapping(
      "searchTerm" -> optional(text)
        .transform[Option[OtherGoodsSearchItem]](
        _.map(_ => OtherGoodsSearchItem("label.other-goods.chocolate", ProductPath("other-goods/other")),
        ),
        _.map(_.name)
      ),
      "country" -> ignored(""),
      "originCountry" -> optional(text),
      "currency" -> ignored(""),
      "cost" -> ignored(BigDecimal(0)),
      "isVatPaid" -> optional(boolean),
      "isUccRelief" -> optional(boolean),
      "isCustomPaid" -> optional(boolean),
      "hasEvidence" -> optional(boolean)
    )(OtherGoodsDto.apply)(OtherGoodsDto.unapply)
  )
  implicit val arbOtherGoodsInput: Arbitrary[Form[OtherGoodsDto]] = fixed(otherGoodsForm)

  implicit val arbJourneyData: Arbitrary[JourneyData] = fixed(JourneyData.apply())

  implicit val arbSelectProductsInput: Arbitrary[Form[SelectProductsDto]] = fixed(SelectProductsDto.form(""))

  implicit val arbPrivateCraftInput: Arbitrary[Form[PrivateCraftDto]] = fixed(PrivateCraftDto.form)

  implicit val arbIrishBorderInput: Arbitrary[Form[IrishBorderDto]] = fixed(IrishBorderDto.form)

  implicit val arbBringingOverAllowanceInput: Arbitrary[Form[BringingOverAllowanceDto]] = fixed(BringingOverAllowanceDto.form)

  implicit val arbEuCountryCheckInput: Arbitrary[Form[EuCountryCheckDto]] = fixed(EuCountryCheckDto.form)

  implicit val arbClaimedVatResInput: Arbitrary[Form[ClaimedVatResDto]] = fixed(ClaimedVatResDto.form)

  implicit val arbConfirmAgeInput: Arbitrary[Form[AgeOver17Dto]] = fixed(AgeOver17Dto.form)

  implicit val arbBringingDutyFreeInput: Arbitrary[Form[BringingDutyFreeDto]] = fixed(BringingDutyFreeDto.form)

  val tobaccoForm: Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks" -> optional(number),
      "weightOrVolume" -> optional(ignored(BigDecimal(0))),
      "country" -> text,
      "originCountry" -> optional(text),
      "currency" -> ignored(""),
      "cost" -> ignored(BigDecimal(0)),
      "isVatPaid" -> optional(boolean),
      "isExcisePaid" -> optional(boolean),
      "isCustomPaid" -> optional(boolean),
      "hasEvidence" -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )
  implicit val arbTobaccoInput: Arbitrary[Form[TobaccoDto]] = fixed(tobaccoForm)
  // Another limitation of the framework is that it can generate Arbitrary[T] but not Arbitrary[T[_]],
  // so any nested types (like a Play `Form[]`) must similarly be provided by wrapping
  // a concrete value using `fixed()`.  Usually, you'll have a value you can use somewhere else
  // in your codebase - either in your production code or another test.
  // Note - these values are declared as `implicit` to simplify calls to `render()` below
  // e.g implicit val arbReportProblemPage: Arbitrary[Form[ReportProblemForm]] = fixed(reportProblemForm)

  // This is the package where the page templates are located in your service
  val viewPackageName = "views.html"

  // This is the layout class or classes which are injected into all full pages in your service.
  // This might be `HmrcLayout` or some custom class(es) that your service uses as base page templates.
  val layoutClasses = Seq(classOf[views.html.templates.GovukLayoutWrapper])

  // this partial function wires up the generic render() functions with arbitrary instances of the correct types.
  // Important: there's a known issue with intellij incorrectly displaying warnings here, you should be able to ignore these for now.
  override def renderViewByClass: PartialFunction[Any, Html] = {
    case errorTemplate: errorTemplate => render(errorTemplate)
    case timeOut: timeOut => render(timeOut)
    // alcohol
    case alcohol_input: alcohol_input => render(alcohol_input)
    // amendments
    case declaration_not_found: declaration_not_found => render(declaration_not_found)
    case declaration_retrieval: declaration_retrieval => render(declaration_retrieval)
    case no_further_amendment: no_further_amendment => render(no_further_amendment)
    case pending_payment: pending_payment => render(pending_payment)
    case previous_declaration: previous_declaration => render(previous_declaration)
    // declaration
    case declare_your_goods: declare_your_goods => render(declare_your_goods)
    case enter_your_details: enter_your_details => render(enter_your_details)
    case zero_to_declare_your_goods: zero_to_declare_your_goods => render(zero_to_declare_your_goods)
    // errors
    case purchase_price_out_of_bounds: purchase_price_out_of_bounds => render(purchase_price_out_of_bounds)
    // other_goods
     case other_goods_input: other_goods_input => render(other_goods_input)
    // purchased_products
    case done: done => render(done)
    case limit_exceed: limit_exceed => render(limit_exceed)
    case nothing_to_declare: nothing_to_declare => render(nothing_to_declare)
    case over_ninety_seven_thousand_pounds: over_ninety_seven_thousand_pounds => render(over_ninety_seven_thousand_pounds)
    case select_products: select_products => render(select_products)
    case zero_to_declare: zero_to_declare => render(zero_to_declare)
    // tobacco
    case no_of_sticks_input: no_of_sticks_input => render(no_of_sticks_input)
    case no_of_sticks_weight_or_volume_input: no_of_sticks_weight_or_volume_input => render(no_of_sticks_weight_or_volume_input)
    case weight_or_volume_input: weight_or_volume_input => render(weight_or_volume_input)
    // travel_details
    case arriving_ni: arriving_ni => render(arriving_ni)
    case bringing_duty_free_question: bringing_duty_free_question => render(bringing_duty_free_question)
    case confirm_age: confirm_age => render(confirm_age)
    case did_you_claim_tax_back: did_you_claim_tax_back => render(did_you_claim_tax_back)
    case duty_free_allowance_question_eu: duty_free_allowance_question_eu => render(duty_free_allowance_question_eu)
    case duty_free_allowance_question_mix: duty_free_allowance_question_mix => render(duty_free_allowance_question_mix)
    case eu_country_check: eu_country_check => render(eu_country_check)
    case eu_evidence_item: eu_evidence_item => render(eu_evidence_item)
    case goods_bought_inside_and_outside_eu: goods_bought_inside_and_outside_eu => render(goods_bought_inside_and_outside_eu)
    case goods_bought_inside_eu: goods_bought_inside_eu => render(goods_bought_inside_eu)
    case goods_bought_outside_eu: goods_bought_outside_eu => render(goods_bought_outside_eu)
    case irish_border: irish_border => render(irish_border)
    case no_need_to_use_service: no_need_to_use_service => render(no_need_to_use_service)
    case no_need_to_use_service_gbni: no_need_to_use_service_gbni => render(no_need_to_use_service_gbni)
    case private_travel: private_travel => render(private_travel)
    case ucc_relief_item: ucc_relief_item => render(ucc_relief_item)
    case uk_resident: uk_resident => render(uk_resident)
    case ukexcise_paid: ukexcise_paid => render(ukexcise_paid)
    case ukexcise_paid_item: ukexcise_paid_item => render(ukexcise_paid_item)
    case ukvat_paid_item: ukvat_paid_item => render(ukvat_paid_item)
  }

  runAccessibilityTests()
}
