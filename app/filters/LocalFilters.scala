/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package filters

import javax.inject.{Inject, Singleton}
import play.api.http.HttpFilters
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

@Singleton
class LocalFilters @Inject() (
  frontendFilters: FrontendFilters,
  validateAccessCodeFilter: ValidateAccessCodeFilter,
  disableBrowserCacheFilter: DisableBrowserCacheFilter
) extends HttpFilters {

  override val filters = frontendFilters.filters ++ Seq(validateAccessCodeFilter, disableBrowserCacheFilter)

}
