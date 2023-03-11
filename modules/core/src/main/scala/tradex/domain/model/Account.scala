package tradex.domain
package model

import java.time.LocalDateTime
import squants.market.*
import cats.data.ValidatedNec
import cats.syntax.all.*

import utils.Newtype

// top level definition
def today = LocalDateTime.now

object account:

  // with scala 3.1.0, the above import works
  // but significant indentation doesn't work
  def foo(xs: List[Int]) =
    xs.map(x =>
      val y = x - 1
      y * y
    )

  enum AccountType(val entryName: String):
    case Trading extends AccountType("Trading")
    case Settlement extends AccountType("Settlement")
    case Both extends AccountType("Both")

  object AccountType:
    def apply(s: String): Option[AccountType] =
      if (s == "Trading") Some(Trading)
      else if (s == "Settlement") Some(Settlement)
      else if (s == "Both") Some(Both)
      else None

  final case class Account(
      no: AccountNo.Type,
      name: AccountName.Type,
      dateOfOpen: LocalDateTime,
      dateOfClose: Option[LocalDateTime],
      accountType: AccountType,
      baseCurrency: Currency,
      tradingCurrency: Option[Currency],
      settlementCurrency: Option[Currency]
  )

  // newtypes for AccountNo
  type AccountNo = String
  object AccountNo extends Newtype[String]
  extension (ano: AccountNo.Type)
    def validateNo: ValidatedNec[String, AccountNo.Type] =
      if (ano.value.size > 12 || ano.value.size < 5)
        s"AccountNo cannot be more than 12 characters or less than 5 characters long".invalidNec
      else ano.validNec

  // newtypes for AccountName
  type AccountName = String
  object AccountName extends Newtype[String]
  extension (aname: AccountName.Type)
    def validateName: ValidatedNec[String, AccountName.Type] =
      if (aname.value.isEmpty || aname.value.isBlank)
        s"Account Name cannot be empty".invalidNec
      else aname.validNec

  object Account:
    // smart constructors
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
        name.validateName,
        validateOpenCloseDate(openDate.getOrElse(today), closeDate)
      ).mapN { (n, nm, d) =>
        Account(
          n,
          nm,
          d._1,
          d._2,
          AccountType.Trading,
          baseCcy,
          tradingCcy.some,
          None
        )
      }
    }

    def settlementAccount(
        no: AccountNo.Type,
        name: AccountName.Type,
        openDate: Option[LocalDateTime],
        closeDate: Option[LocalDateTime],
        baseCcy: Currency,
        settlementCcy: Currency
    ): ValidatedNec[String, Account] = {
      (
        no.validateNo,
        name.validateName,
        validateOpenCloseDate(openDate.getOrElse(today), closeDate)
      ).mapN { (n, nm, d) =>
        Account(
          n,
          nm,
          d._1,
          d._2,
          AccountType.Settlement,
          baseCcy,
          None,
          settlementCcy.some
        )
      }
    }

    def tradingAndSettlementAccount(
        no: AccountNo.Type,
        name: AccountName.Type,
        openDate: Option[LocalDateTime],
        closeDate: Option[LocalDateTime],
        baseCcy: Currency,
        tradingCcy: Currency,
        settlementCcy: Currency
    ): ValidatedNec[String, Account] = {
      (
        no.validateNo,
        name.validateName,
        validateOpenCloseDate(openDate.getOrElse(today), closeDate)
      ).mapN { (n, nm, d) =>
        Account(
          n,
          nm,
          d._1,
          d._2,
          AccountType.Both,
          baseCcy,
          tradingCcy.some,
          settlementCcy.some
        )
      }
    }

    private def validateOpenCloseDate(
        od: LocalDateTime,
        cd: Option[LocalDateTime]
    ): ValidatedNec[String, (LocalDateTime, Option[LocalDateTime])] =
      cd.map { c =>
        if (c isBefore od)
          s"Close date [$c] cannot be earlier than open date [$od]".invalidNec
        else (od, cd).validNec
      }.getOrElse { (od, cd).validNec }

    private def validateAccountAlreadyClosed(
        a: Account
    ): ValidatedNec[String, Account] = {
      if (a.dateOfClose.isDefined)
        s"Account ${a.no} is already closed".invalidNec
      else a.validNec
    }

    private def validateCloseDate(
        a: Account,
        cd: LocalDateTime
    ): ValidatedNec[String, LocalDateTime] = {
      if (cd isBefore a.dateOfOpen)
        s"Close date [$cd] cannot be earlier than open date [${a.dateOfOpen}]".invalidNec
      else cd.validNec
    }

    def close(
        a: Account,
        closeDate: LocalDateTime
    ): ValidatedNec[String, Account] = {
      (validateAccountAlreadyClosed(a), validateCloseDate(a, closeDate))
        .mapN { (acc, _) =>
          acc.copy(dateOfClose = Some(closeDate))
        }
    }
