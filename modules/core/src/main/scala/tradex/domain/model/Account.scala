package tradex.domain
package model

import java.time.LocalDateTime
import squants.market.*
import cats.data.ValidatedNec
import cats.syntax.all.*

import utils.Newtype

object account:
  enum AccountType(val entryName: String):
    case Trading extends AccountType("Trading")
    case Settlement extends AccountType("Settlement")
    case Both extends AccountType("Both")

  final case class Account private (
      no: AccountNo.Type,
      name: AccountName.Type,
      dateOfOpen: LocalDateTime,
      dateOfClose: Option[LocalDateTime],
      accountType: AccountType,
      baseCurrency: Currency,
      tradingCurrency: Option[Currency],
      settlementCurrency: Option[Currency]
  )

  type AccountNo = String
  object AccountNo extends Newtype[String]
  extension (ano: AccountNo.Type)
    def validateNo: ValidatedNec[String, AccountNo.Type] =
      if (ano.value.size > 12 || ano.value.size < 5)
        s"AccountNo cannot be more than 12 characters or less than 5 characters long".invalidNec
      else ano.validNec

  type AccountName = String
  object AccountName extends Newtype[String]
  extension (aname: AccountName.Type)
    def validateName: ValidatedNec[String, AccountName.Type] =
      if (aname.value.isEmpty || aname.value.isBlank)
        s"Account Name cannot be empty".invalidNec
      else aname.validNec

  object Account:
    def tradingAccount(
        no: AccountNo.Type,
        name: AccountName.Type,
        openDate: Option[LocalDateTime],
        closeDate: Option[LocalDateTime],
        baseCcy: Currency,
        tradingCcy: Currency
    ): ValidatedNec[String, Account] = {
      (
        no.validateNo,
        name.validateName
      ).mapN { (n, nm) =>
        Account(
          n,
          nm,
          openDate.get,
          closeDate,
          AccountType.Trading,
          baseCcy,
          tradingCcy.some,
          None
        )
      }
    }
