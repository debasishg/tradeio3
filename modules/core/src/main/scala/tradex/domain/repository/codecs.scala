package tradex.domain
package repository

import cats.syntax.all.*
import skunk.*
import skunk.codec.all.*
import squants.market.*
import model.account.*
import model.instrument.*
import model.order.*
import model.execution.*
import model.trade.*
import model.market.*
import model.user.*

object codecs:
  given MoneyContext = defaultMoneyContext

  val accountNo: Codec[AccountNo] =
    varchar.eimap[AccountNo] { s =>
      AccountNo(s).validateNo.toEitherAssociative.leftMap(identity)
    }(AccountNo.unwrap(_))

  val accountName: Codec[AccountName] =
    varchar.eimap[AccountName] { s =>
      AccountName(s).validateName.toEitherAssociative.leftMap(identity)
    }(AccountName.unwrap(_))

  val money: Codec[Money] = numeric.imap[Money](USD(_))(_.amount)

  val currency: Codec[Currency] =
    varchar.eimap[Currency](Currency(_).toEither.leftMap(_.getMessage()))(
      _.code
    )

  val instrumentName: Codec[InstrumentName] =
    varchar.eimap[InstrumentName] { s =>
      NonEmptyString.make(s).map(InstrumentName(_)).toEitherAssociative.leftMap(identity)
    }(_.value.toString)

  val isinCode: Codec[ISINCode] =
    varchar.eimap[ISINCode] { s =>
      ISINCode.make(s).toEitherAssociative.leftMap(identity)
    }(ISINCode.unwrap(_))

  val orderNo: Codec[OrderNo] =
    varchar.eimap[OrderNo] { s =>
      OrderNo(s).validateNo.toEitherAssociative.leftMap(identity)
    }(OrderNo.unwrap(_))

  val unitPrice: Codec[UnitPrice] =
    numeric.eimap[UnitPrice] { s =>
      UnitPrice.make(s).toEitherAssociative.leftMap(identity)
    }(UnitPrice.unwrap(_))

  val quantity: Codec[Quantity] =
    numeric.eimap[Quantity] { s =>
      Quantity.make(s).toEitherAssociative.leftMap(identity)
    }(Quantity.unwrap(_))

  val lotSize: Codec[LotSize] =
    int4.eimap[LotSize] { s =>
      LotSize.make(s).toEitherAssociative.leftMap(identity)
    }(LotSize.unwrap(_))

  val executionRefNo: Codec[ExecutionRefNo] =
    varchar.eimap[ExecutionRefNo] { s =>
      ExecutionRefNo(s).validateNo.toEitherAssociative.leftMap(identity)
    }(ExecutionRefNo.unwrap(_))

  val tradeRefNo: Codec[TradeRefNo] =
    varchar.eimap[TradeRefNo] { s =>
      TradeRefNo(s).validateNo.toEitherAssociative.leftMap(identity)
    }(TradeRefNo.unwrap(_))

  val market: Codec[Market] =
    varchar.imap[Market](Market.valueOf(_))(_.entryName)

  val taxFeeId: Codec[TaxFeeId] =
    varchar.imap[TaxFeeId](TaxFeeId.valueOf(_))(_.entryName)

  val userId: Codec[UserId] =
    varchar.eimap[UserId] { s =>
      UserId(s).validateNo.toEitherAssociative.leftMap(identity)
    }(UserId.unwrap(_))

  val userName: Codec[UserName] =
    varchar.eimap[UserName] { s =>
      NonEmptyString.make(s).map(UserName(_)).toEitherAssociative.leftMap(identity)
    }(UserName.unwrap(_))

  val encPassword: Codec[EncryptedPassword] =
    varchar.eimap[EncryptedPassword] { s =>
      NonEmptyString.make(s).map(EncryptedPassword(_)).toEitherAssociative.leftMap(identity)
    }(EncryptedPassword.unwrap(_))
