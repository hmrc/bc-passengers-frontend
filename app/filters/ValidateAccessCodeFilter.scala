package filters

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64

import akka.stream.Materializer
import javax.inject.Inject
import org.mindrot.jbcrypt.BCrypt
import play.api.{Configuration, Logger}
import play.api.mvc._
import services.DateTimeProviderService

import scala.concurrent.{ExecutionContext, Future}


class ValidateAccessCodeFilter @Inject() (
  dateTimeProviderService: DateTimeProviderService,
  config: Configuration,
  implicit val mat: Materializer,
  implicit val ec: ExecutionContext
) extends Filter {

  lazy val accessToken = config.get[String]("access.token")
  lazy val cutoffDate: LocalDate = LocalDate.parse(config.get[String]("access.cutoffdate"), DateTimeFormatter.ISO_LOCAL_DATE)
  lazy val intervalDays: Int = config.get[String]("access.intervaldays").toInt
  lazy val enabled: Boolean = config.getOptional[Boolean]("access.enabled").getOrElse(false)

  //Log access keys
  if(config.getOptional[Boolean]("access.logkeys").getOrElse(false))
    for(d <- validExpiryDatesOnOrAfter(dateTimeProviderService.javaNow)) Logger.info(d + ": " + generateAccessCodeFor(d))

  def validExpiryDatesOnOrAfter(date: LocalDate): Stream[LocalDate] = Stream
    .iterate(cutoffDate)(_.minusDays(intervalDays))
    .takeWhile(d => d.isAfter(date) || d.isEqual(date))

  def generateAccessCodeFor(date: LocalDate): String = {

    val c = BCrypt.hashpw(accessToken + date.format(DateTimeFormatter.ISO_LOCAL_DATE), BCrypt.gensalt())
    Base64.getEncoder.encode(c.getBytes()).map(_.toChar).mkString
  }

  def isValidAccessCodeForDate(code: String, date: LocalDate): Boolean = {

    val c = Base64.getDecoder.decode(code.getBytes).map(_.toChar).mkString
    BCrypt.checkpw(accessToken + date.format(DateTimeFormatter.ISO_LOCAL_DATE), c)
  }

  def codeIsValidOnOrAfter(date: LocalDate)(code: String): Boolean = {
    try {
      if (date.isAfter(cutoffDate)) false
      else {
        val dates = validExpiryDatesOnOrAfter(date)
        dates.exists(d => isValidAccessCodeForDate(code, d))
      }
    }
    catch {
      case e: IllegalArgumentException =>
        Logger.debug(s"Exception checking codeIsValidOnOrAfter($date)([hidden]), returning false: " + e.getMessage)
        false
    }
  }

  override def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    if ( ! (enabled && requestHeader.uri.startsWith("/check-tax-on-goods-you-bring-into-the-uk")) ) {
      nextFilter(requestHeader)
    }
    else if (requestHeader.session.get("bcpaccess").fold(false)(_=="true")) {
      nextFilter(requestHeader)
    }
    else if(requestHeader.getQueryString("ac").fold(false)(codeIsValidOnOrAfter(dateTimeProviderService.javaNow))) {
      nextFilter(requestHeader).map(_.addingToSession("bcpaccess" -> "true")(requestHeader))
    }
    else {
      Future.successful(Results.Unauthorized("Invalid access token"))
    }
  }
}
