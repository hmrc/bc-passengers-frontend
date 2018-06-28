package util

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter

import scala.concurrent.{ExecutionContext, Future}

class FakeCookieCryptoFilter @Inject()(override val mat: Materializer, val ec: ExecutionContext) extends CookieCryptoFilter {

  override protected val encrypter = new Encrypter {
    override def encrypt(plain: PlainContent): Crypted = ???
  }
  override protected val decrypter = new Decrypter {

    override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes = ???
    override def decrypt(reversiblyEncrypted: Crypted): PlainText = ???
  }

  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) =
    next(rh)
}
