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

  val couponFrequency: Codec[CouponFrequency] =
    varchar.imap[CouponFrequency](CouponFrequency.valueOf(_))(_.entryName)

  val executionRefNo: Codec[ExecutionRefNo] =
    uuid.imap[ExecutionRefNo](ExecutionRefNo(_))(ExecutionRefNo.unwrap(_))

  val tradeRefNo: Codec[TradeRefNo] =
    uuid.imap[TradeRefNo](TradeRefNo(_))(TradeRefNo.unwrap(_))

  val market: Codec[Market] =
    varchar.eimap[Market](Market.withValue(_).toEitherAssociative.leftMap(identity))(_.entryName)

  val instrumentType: Codec[InstrumentType] =
    varchar.imap[InstrumentType](InstrumentType.valueOf(_))(_.entryName)

  val buySell: Codec[BuySell] =
    varchar.eimap[BuySell](BuySell.withValue(_).toEitherAssociative.leftMap(identity))(_.entryName)

  val taxFeeId: Codec[TaxFeeId] =
    varchar.imap[TaxFeeId](TaxFeeId.valueOf(_))(_.entryName)

  val userId: Codec[UserId] =
    uuid.imap[UserId](UserId(_))(UserId.unwrap(_))

  val userName: Codec[UserName] =
    varchar.eimap[UserName] { s =>
      NonEmptyString.make(s).map(UserName(_)).toEitherAssociative.leftMap(identity)
    }(UserName.unwrap(_))

  val encPassword: Codec[EncryptedPassword] =
    varchar.eimap[EncryptedPassword] { s =>
      NonEmptyString.make(s).map(EncryptedPassword(_)).toEitherAssociative.leftMap(identity)
    }(EncryptedPassword.unwrap(_))
