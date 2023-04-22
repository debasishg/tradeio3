package tradex.domain
package model

import zio.prelude.Validation
import java.time.LocalDateTime
import squants.market._
import account.*

object balance {
  final case class Balance private[domain] (
      accountNo: AccountNo,
      amount: Money,
      currency: Currency,
      asOf: LocalDateTime
  )

  object Balance {
    def balance(
        accountNo: AccountNo,
        amount: Money,
        currency: Currency,
        asOf: LocalDateTime
    ): Validation[String, Balance] =
      validateAsOfDate(asOf)
        .map(dt => Balance(accountNo, amount, currency, dt))

    private def validateAsOfDate(
        date: LocalDateTime
    ): Validation[String, LocalDateTime] =
      if (date.isAfter(today))
        Validation.fail(s"Balance date [$date] cannot be later than today")
      else Validation.succeed(date)
  }
}
