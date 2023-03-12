package tradex.domain
package model

import java.time.LocalDateTime
import squants.market.*
import zio.prelude.Validation
import utils.Newtype

object newAccount {
  final case class AccountBase(
      no: AccountNo.Type,
      name: AccountName.Type,
      dateOfOpen: LocalDateTime,
      dateOfClose: Option[LocalDateTime],
      baseCurrency: Currency
  )

  sealed trait AccountType

  sealed trait Trading extends AccountType:
    def tradingCurrency: Currency

  sealed trait Settlement extends AccountType:
    def settlementCurrency: Currency

  trait Account[C <: AccountType]:
    private[newAccount] def base: AccountBase
    private[newAccount] def accountType: C

  final case class TradingAccount private (
      private[newAccount] base: AccountBase,
      private[newAccount] accountType: Trading
  ) extends Account[Trading] {
    val no              = base.no
    val name            = base.name
    val dateOfOpen      = base.dateOfOpen
    val dateOfClose     = base.dateOfClose
    val baseCurrency    = base.baseCurrency
    val tradingCurrency = accountType.tradingCurrency
  }

  final case class SettlementAccount private (
      private[newAccount] base: AccountBase,
      private[newAccount] accountType: Settlement
  ) extends Account[Settlement] {
    val no                 = base.no
    val name               = base.name
    val dateOfOpen         = base.dateOfOpen
    val dateOfClose        = base.dateOfClose
    val baseCurrency       = base.baseCurrency
    val settlementCurrency = accountType.settlementCurrency
  }

  final case class TradingAndSettlementAccount private (base: AccountBase, accountType: Trading & Settlement)
      extends Account[Trading & Settlement] {
    val no                 = base.no
    val name               = base.name
    val dateOfOpen         = base.dateOfOpen
    val dateOfClose        = base.dateOfClose
    val baseCurrency       = base.baseCurrency
    val tradingCurrency    = accountType.tradingCurrency
    val settlementCurrency = accountType.settlementCurrency
  }

  type ClientAccount = TradingAccount | SettlementAccount | TradingAndSettlementAccount

  // newtypes for AccountNo
  type AccountNo = String
  object AccountNo extends Newtype[String]
  extension (ano: AccountNo.Type)
    def validateNo: Validation[String, AccountNo.Type] =
      if (ano.value.size > 12 || ano.value.size < 5)
        Validation.fail(s"AccountNo cannot be more than 12 characters or less than 5 characters long")
      else Validation.succeed(ano)

  // newtypes for AccountName
  type AccountName = String
  object AccountName extends Newtype[String]
  extension (aname: AccountName.Type)
    def validateName: Validation[String, AccountName.Type] =
      if (aname.value.isEmpty || aname.value.isBlank)
        Validation.fail(s"Account Name cannot be empty")
      else Validation.succeed(aname)

  object TradingAccount:
    def tradingAccount(
        no: AccountNo.Type,
        name: AccountName.Type,
        dateOfOpen: Option[LocalDateTime],
        dateOfClose: Option[LocalDateTime],
        baseCurrency: Currency,
        tradingCcy: Currency
    ): Validation[String, TradingAccount] =
      Validation.validateWith(
        no.validateNo,
        name.validateName,
        validateOpenCloseDate(dateOfOpen.getOrElse(today), dateOfClose)
      ) { (n, nm, d) =>
        TradingAccount(
          base = AccountBase(no, name, d._1, d._2, baseCurrency),
          accountType = new Trading:
            def tradingCurrency = tradingCcy
        )
      }

  object SettlementAccount:
    def settlementAccount(
        no: AccountNo.Type,
        name: AccountName.Type,
        dateOfOpen: Option[LocalDateTime],
        dateOfClose: Option[LocalDateTime],
        baseCurrency: Currency,
        settlementCcy: Currency
    ): Validation[String, SettlementAccount] =
      Validation.validateWith(
        no.validateNo,
        name.validateName,
        validateOpenCloseDate(dateOfOpen.getOrElse(today), dateOfClose)
      ) { (n, nm, d) =>
        SettlementAccount(
          base = AccountBase(no, name, d._1, d._2, baseCurrency),
          accountType = new Settlement:
            def settlementCurrency = settlementCcy
        )
      }

  object TradingAndSettlementAccount:
    private abstract class Both() extends Trading, Settlement
    def tradingAndSettlementAccount(
        no: AccountNo.Type,
        name: AccountName.Type,
        dateOfOpen: Option[LocalDateTime],
        dateOfClose: Option[LocalDateTime],
        baseCurrency: Currency,
        tradingCcy: Currency,
        settlementCcy: Currency
    ): Validation[String, TradingAndSettlementAccount] =
      Validation.validateWith(
        no.validateNo,
        name.validateName,
        validateOpenCloseDate(dateOfOpen.getOrElse(today), dateOfClose)
      ) { (n, nm, d) =>
        TradingAndSettlementAccount(
          base = AccountBase(no, name, d._1, d._2, baseCurrency),
          accountType = new Both:
            def tradingCurrency    = tradingCcy
            def settlementCurrency = settlementCcy
        )
      }

  private def validateOpenCloseDate(
      od: LocalDateTime,
      cd: Option[LocalDateTime]
  ): Validation[String, (LocalDateTime, Option[LocalDateTime])] =
    cd.map { c =>
      if (c isBefore od)
        Validation.fail(s"Close date [$c] cannot be earlier than open date [$od]")
      else Validation.succeed((od, cd))
    }.getOrElse(Validation.succeed((od, cd)))

  private def validateAccountAlreadyClosed(
      a: AccountBase
  ): Validation[String, AccountBase] = {
    if (a.dateOfClose.isDefined)
      Validation.fail(s"Account ${a.no} is already closed")
    else Validation.succeed(a)
  }

  private def validateCloseDate(
      a: AccountBase,
      cd: LocalDateTime
  ): Validation[String, LocalDateTime] =
    if (cd isBefore a.dateOfOpen)
      Validation.fail(s"Close date [$cd] cannot be earlier than open date [${a.dateOfOpen}]")
    else Validation.succeed(cd)

  def close(
      a: AccountBase,
      closeDate: LocalDateTime
  ): Validation[String, AccountBase] =
    Validation
      .validateWith(validateAccountAlreadyClosed(a), validateCloseDate(a, closeDate)) { (acc, _) =>
        acc.copy(dateOfClose = Some(closeDate))
      }
}

object Main {
  import newAccount._
  val ta = TradingAccount.tradingAccount(
    no = AccountNo("a-123456"),
    name = AccountName("debasish ghosh"),
    baseCurrency = USD,
    tradingCcy = USD,
    dateOfOpen = None,
    dateOfClose = None
  )
}
