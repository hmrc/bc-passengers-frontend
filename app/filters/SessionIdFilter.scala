package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc._

import scala.concurrent.Future


class SessionIdFilter @Inject() (implicit val mat: Materializer)  extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
//
//    val h = requestHeader.headers
//
//
//    h.add()
//
//    requestHeader.copy(headers = )

    nextFilter(requestHeader)
  }
}
