package util

import akka.stream.Materializer
import javax.inject.Inject
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter

import scala.concurrent.{ExecutionContext, Future}

class FakeSessionCookieCryptoFilter @Inject()(val mat: Materializer, val ec: ExecutionContext) extends SessionCookieCryptoFilter {

  override protected def encrypter = ???
  override protected def decrypter = ???
  override protected def sessionBaker = ???

  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) = next(rh)
}
