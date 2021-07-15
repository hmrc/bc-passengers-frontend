/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package utils

trait CurrencyFormatter {
  def currencyFormat(amt: BigDecimal): String = f"&pound;$amt%,1.2f".replace(".00","")
}

object CurrencyFormatter extends CurrencyFormatter
