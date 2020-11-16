/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.actions

import com.google.inject.Inject
import config.AppConfig
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval}
import uk.gov.hmrc.http.HeaderCarrier
import util.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends BaseSpec with MockitoSugar{

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  "Auth Action" when {

    val fakeRequest = FakeRequest("GET", "/some/resource/path")
    val bodyParsers = app.injector.instanceOf[BodyParsers.Default]
    val appConfig = app.injector.instanceOf[AppConfig]

    "the user is logged in with internalId returned" must {

      "successful cary out request" in {

        val authConnector = mock[AuthConnector]
        implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

        when(authConnector.authorise[Option[Credentials]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Credentials("id", "type"))))

        val authAction = new AuthenticatedIdentifierAction(authConnector, appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK
      }
    }

    "the user is logged in with NO internalId returned" must {

      "redirect to unauthorised" in {

        implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new IncorrectCredentialStrength), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should endWith("unauthorised-user")
      }
    }

    "the user hasn't logged in" must {

      "redirect the user to log in " in {

        implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result).get should startWith("/stride/sign-in")
      }
    }

    "the user's session has expired" must {

      "redirect the user to log in " in {

        implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result).get should startWith("/stride/sign-in")
      }
    }

    "the user doesn't have sufficient enrolments" must {

      "redirect the user to the unauthorised page" in {

        implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new InsufficientEnrolments), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result).get should endWith("unauthorised-user")
      }
    }

    "the user doesn't have sufficient confidence level" must {

      "redirect the user to the unauthorised page" in {

        implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new InsufficientConfidenceLevel), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result).get should endWith("unauthorised-user")
      }
    }

    "the user used an unaccepted auth provider" must {

      "redirect the user to the unauthorised page" in {

        implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedAuthProvider), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result).get should endWith("unauthorised-user")
      }
    }

    "the user has an unsupported affinity group" must {

      "redirect the user to the unauthorised page" in {

        implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedAffinityGroup), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result).get should endWith("unauthorised-user")
      }
    }

    "the user has an unsupported credential role" must {

      "redirect the user to the unauthorised page" in {

        implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result).get should endWith("unauthorised-user")
      }
    }
  }
}

class FakeSuccessAuthConnector[B] @Inject()(response: B) extends AuthConnector {
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.successful(response.asInstanceOf[A])
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
