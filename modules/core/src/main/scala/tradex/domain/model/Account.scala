package tradex.domain
package model

import java.time.LocalDateTime
import squants.market._
import cats.data.ValidatedNec
import cats.syntax.all._

object account:
  enum AccountType(val entryName: String):
    case Trading extends AccountType("Trading")
    case Settlement extends AccountType("Settlement")
    case Both extends AccountType("Both")

  final case class Account private (
      no: String,
      name: String,
      dateOfOpen: LocalDateTime,
      dateOfClose: Option[LocalDateTime],
      accountType: AccountType,
      baseCurrency: Currency,
      tradingCurrency: Option[Currency],
      settlementCurrency: Option[Currency]
  )
