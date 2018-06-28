package util

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.test.FakeRequest
import play.filters.csrf.CSRF.Token
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}


trait BaseSpec extends WordSpecLike with Matchers with GuiceOneAppPerSuite with BeforeAndAfterEach {

  implicit lazy val hc = HeaderCarrier(sessionId = Some(SessionId("fakesessionid")))

  private def addToken[T](fakeRequest: FakeRequest[T])(implicit app: Application) = {
    val csrfConfig     = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter     = app.injector.instanceOf[CSRFFilter]
    val token          = csrfFilter.tokenProvider.generateToken

    fakeRequest.copyFakeRequest(tags = fakeRequest.tags ++ Map(
      Token.NameRequestTag  -> csrfConfig.tokenName,
      Token.RequestTag      -> token
    )).withHeaders((csrfConfig.headerName, token)).withSession(SessionKeys.sessionId -> "fakesessionid")


  }

  def EnhancedFakeRequest(method: String, uri: String)(implicit app: Application) = addToken(FakeRequest(method, uri))
}
