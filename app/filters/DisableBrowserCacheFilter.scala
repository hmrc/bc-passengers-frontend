/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package filters

import akka.stream.Materializer
import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


class DisableBrowserCacheFilter @Inject()(
  implicit val mat: Materializer,
  implicit val ec: ExecutionContext
) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    nextFilter(requestHeader).map { result =>

      result.withHeaders("Cache-Control" -> "no-cache, no-store, must-revalidate, max-age=0",
        "Pragma" -> "no-cache",
        "Expires" -> "0")
    }
  }

}
