package filters

import javax.inject.{Inject, Singleton}
import play.api.http.HttpFilters
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

@Singleton
class LocalFilters @Inject() (frontendFilters: FrontendFilters, validateAccessCodeFilter: ValidateAccessCodeFilter) extends HttpFilters {

  override val filters = frontendFilters.filters ++ Seq(validateAccessCodeFilter)

}
