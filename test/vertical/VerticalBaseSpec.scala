package vertical

import models.JourneyData
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt}
import services.LocalSessionCache
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future

trait VerticalBaseSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[LocalSessionCache].toInstance(MockitoSugar.mock[LocalSessionCache]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  trait LocalSetup {

    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[LocalSessionCache].fetchAndGetJourneyData(any())) thenReturn {
        Future.successful(cachedJourneyData)
      }

      when(injected[LocalSessionCache].cacheJourneyData(any())(any())) thenReturn {
        Future.successful(CacheMap("fakeid", Map.empty))
      }

      rt(app, req)
    }

  }

  override def beforeEach: Unit = {
    reset(injected[LocalSessionCache])
  }

}