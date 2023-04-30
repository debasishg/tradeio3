package tradex.domain
package transport

import model.account.*
import zio.json.*
import cats.syntax.all.*
import squants.market.Currency

object accountT {
  given JsonDecoder[AccountNo] =
    JsonDecoder[String].mapOrFail(AccountNo.make(_).toEither.leftMap(_.head))
  given JsonEncoder[AccountNo] = JsonEncoder[String].contramap(AccountNo.unwrap(_))

  given JsonDecoder[AccountName] =
    JsonDecoder[String].mapOrFail(AccountName.make(_).toEither.leftMap(_.head))
  given JsonEncoder[AccountName] = JsonEncoder[String].contramap(AccountName.unwrap(_))

  given JsonCodec[AccountBase] = DeriveJsonCodec.gen[AccountBase]
  given JsonDecoder[TradingAccount] =
    JsonDecoder[(AccountBase, String, Currency)].mapOrFail {
      case (base, accountType, ccy) if accountType == "Trading" =>
        TradingAccount
          .tradingAccount(
            base.no,
            base.name,
            Some(base.dateOfOpen),
            base.dateOfClose,
            base.baseCurrency,
            ccy
          )
          .toEither
          .leftMap(_.head)
      case (base, accountType, ccy) =>
        s"Invalid account type: $accountType".invalid[TradingAccount].toEither
    }
  given JsonEncoder[TradingAccount] =
    JsonEncoder[(AccountBase, String, Currency)].contramap { account =>
      (account.base, "Trading", account.tradingCurrency)
    }

  given JsonDecoder[SettlementAccount] =
    JsonDecoder[(AccountBase, String, Currency)].mapOrFail {
      case (base, accountType, ccy) if accountType == "Settlement" =>
        SettlementAccount
          .settlementAccount(
            base.no,
            base.name,
            Some(base.dateOfOpen),
            base.dateOfClose,
            base.baseCurrency,
            ccy
          )
          .toEither
          .leftMap(_.head)
      case (base, accountType, ccy) =>
        s"Invalid account type: $accountType".invalid[SettlementAccount].toEither
    }
  given JsonEncoder[SettlementAccount] =
    JsonEncoder[(AccountBase, String, Currency)].contramap { account =>
      (account.base, "Settlement", account.settlementCurrency)
    }

  given JsonDecoder[TradingAndSettlementAccount] =
    JsonDecoder[(AccountBase, String, Currency, Currency)].mapOrFail {
      case (base, accountType, tCcy, sCcy) if accountType == "Trading & Settlement" =>
        TradingAndSettlementAccount
          .tradingAndSettlementAccount(
            base.no,
            base.name,
            Some(base.dateOfOpen),
            base.dateOfClose,
            base.baseCurrency,
            tCcy,
            sCcy
          )
          .toEither
          .leftMap(_.head)
      case (base, accountType, tCcy, sCcy) =>
        s"Invalid account type: $accountType".invalid[TradingAndSettlementAccount].toEither
    }
  given JsonEncoder[TradingAndSettlementAccount] =
    JsonEncoder[(AccountBase, String, Currency, Currency)].contramap { account =>
      (account.base, "Trading & Settlement", account.tradingCurrency, account.settlementCurrency)
    }
}
