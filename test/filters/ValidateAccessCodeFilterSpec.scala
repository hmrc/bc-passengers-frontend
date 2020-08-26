package filters

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.stream.Materializer
import config.AppConfig
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HttpEntity
import play.api.{Application, Configuration, mvc}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.RequestHeader
import services.DateTimeProviderService
import util.BaseSpec
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository

import scala.concurrent.{ExecutionContext, Future}

class ValidateAccessCodeFilterSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .configure("access.cutoffdate" -> "2019-01-25")
    .configure("access.intervaldays" -> "7")
    .configure("access.logkeys" -> "false")
    .configure("access.token" -> "changeme")
    .overrides(bind[DateTimeProviderService].toInstance(new DateTimeProviderService {
      override def javaNow: LocalDate = LocalDate.parse("2018-12-28")
    }))
    .build()

  trait LocalSetup {

    def enabled: String

    def vacf: ValidateAccessCodeFilter = new ValidateAccessCodeFilter(injected[DateTimeProviderService],
      injected[Configuration] ++ Configuration("access.enabled" -> enabled),
      injected[Materializer], injected[ExecutionContext])

    val testDate = LocalDate.parse("2018-12-28", DateTimeFormatter.ISO_LOCAL_DATE)
  }

  "Calling generateAccessCode" should {

    "generate a code that validates in future against the same date" in new LocalSetup {

      override val enabled: String = "true"

      val code1: String = vacf.generateAccessCodeFor(testDate)
      val code2: String = vacf.generateAccessCodeFor(testDate)

      vacf.isValidAccessCodeForDate(code1, testDate) shouldBe true
      vacf.isValidAccessCodeForDate(code2, testDate) shouldBe true

      vacf.isValidAccessCodeForDate(code1, testDate.minusDays(1)) shouldBe false
      vacf.isValidAccessCodeForDate(code2, testDate.plusDays(1)) shouldBe false
    }
  }

  "Calling validExpiryDatesOn" should {

    "Return a list of potential expiry dates from supplied date to the cutoff date" in new LocalSetup {

      override val enabled: String = "true"

      vacf.validExpiryDatesOnOrAfter(LocalDate.parse("2018-12-28")) shouldBe
        List("2019-01-25", "2019-01-18", "2019-01-11", "2019-01-04", "2018-12-28").map(LocalDate.parse)

      vacf.validExpiryDatesOnOrAfter(LocalDate.parse("2018-12-29")) shouldBe
        List("2019-01-25", "2019-01-18", "2019-01-11", "2019-01-04").map(LocalDate.parse)
    }
  }


  "Calling codeIsValidOn" should {

    "Fail when a totally invalid code is supplied" in new LocalSetup {

      override val enabled: String = "true"

      vacf.codeIsValidOnOrAfter(LocalDate.parse("2018-12-28"))("MADE-UP-CODE") shouldBe false
    }

    "Fail when an expired but otherwise valid code is supplied" in new LocalSetup {

      override val enabled: String = "true"

      vacf.codeIsValidOnOrAfter(LocalDate.parse("2018-12-29"))("JDJhJDEwJEFaSjBNdDZFU25LME8ub3kzTWIxUHVQNHhBaVYuVG9VSlA4TDcxMzNxUUE4QXh1S1hVSFA2") shouldBe false
    }

    "Fail when date is after the cutoff" in new LocalSetup {

      override val enabled: String = "true"

      vacf.codeIsValidOnOrAfter(LocalDate.parse("2019-01-26"))("JDJhJDEwJEFaSjBNdDZFU25LME8ub3kzTWIxUHVQNHhBaVYuVG9VSlA4TDcxMzNxUUE4QXh1S1hVSFA2") shouldBe false
    }

    "Pass when a valid code is supplied" in new LocalSetup {

      override val enabled: String = "true"

      vacf.codeIsValidOnOrAfter(LocalDate.parse("2018-12-28"))("JDJhJDEwJEFaSjBNdDZFU25LME8ub3kzTWIxUHVQNHhBaVYuVG9VSlA4TDcxMzNxUUE4QXh1S1hVSFA2") shouldBe true
    }
  }

  "Calling apply" should {

    "Return 200 if neither ac querystring is valid or bcpaccess is true but enabled is false" in new LocalSetup {

      override val enabled: String = "false"

      val mockResult =  Result(ResponseHeader(OK), HttpEntity.NoEntity)
      val nextFilter: RequestHeader => Future[Result] = (_: RequestHeader) => Future.successful(mockResult)

      val r = vacf.apply(nextFilter)(FakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk"))
      status(r) shouldBe OK
    }

    "Return 200 if neither ac querystring is valid or bcpaccess is true, enabled is true, but path does not begin with /check-tax-on-goods-you-bring-into-the-uk" in new LocalSetup {

      override val enabled: String = "true"

      val mockResult =  Result(ResponseHeader(OK), HttpEntity.NoEntity)
      val nextFilter: RequestHeader => Future[Result] = (_: RequestHeader) => Future.successful(mockResult)

      val r = vacf.apply(nextFilter)(FakeRequest("GET", "/ping/ping"))
      status(r) shouldBe OK
    }

    "Return 401 if neither ac querystring is valid or bcpaccess is true" in new LocalSetup {

      override val enabled: String = "true"

      val nextFilter = MockitoSugar.mock[Function[RequestHeader, Future[mvc.Results.Status]]]
      val r = vacf.apply(nextFilter)(FakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk"))
      status(r) shouldBe UNAUTHORIZED
    }

    "Return 200 if bcpaccess session var is 'true'" in new LocalSetup {

      override val enabled: String = "true"

      val mockResult =  Result(ResponseHeader(OK), HttpEntity.NoEntity)
      val nextFilter: RequestHeader => Future[Result] = (_: RequestHeader) => Future.successful(mockResult)

      val r = vacf.apply(nextFilter)(FakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk").withSession("bcpaccess" -> "true"))
      status(r) shouldBe OK
    }

    "Return 200 and set bcpaccess session var to 'true' if ac querystring is valid" in new LocalSetup {

      override val enabled: String = "true"

      val mockResult =  Result(ResponseHeader(OK), HttpEntity.NoEntity)
      val nextFilter: RequestHeader => Future[Result] = (_: RequestHeader) => Future.successful(mockResult)

      val r = vacf.apply(nextFilter)(FakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk?ac=JDJhJDEwJEFaSjBNdDZFU25LME8ub3kzTWIxUHVQNHhBaVYuVG9VSlA4TDcxMzNxUUE4QXh1S1hVSFA2"))
      status(r) shouldBe OK

      session(r).get("bcpaccess") shouldBe Some("true")
    }

    "Return 401 and not set bcpaccess session var if ac querystring is not valid" in new LocalSetup {

      override val enabled: String = "true"

      val mockResult =  Result(ResponseHeader(OK), HttpEntity.NoEntity)
      val nextFilter: RequestHeader => Future[Result] = (_: RequestHeader) => Future.successful(mockResult)

      val r = vacf.apply(nextFilter)(FakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk?ac=MADE-UP-CODE"))
      status(r) shouldBe UNAUTHORIZED

      session(r).get("bcpaccess") shouldBe None
    }
  }
}
