package tradex.domain
package transport

import zio.prelude.{ BicovariantOps, Validation }
import kantan.csv.{ CellDecoder, CellEncoder, DecodeError, RowDecoder }
import kantan.csv.java8.*
import model.account.AccountNo
import model.instrument.{ ISINCode, UnitPrice }
import model.order.{ BuySell, OrderNo, Quantity }
import model.market.Market

object cellCodecs:
  given CellDecoder[AccountNo] = CellDecoder.from[AccountNo](v =>
    AccountNo(v).validateNo.toEitherAssociative.leftMap(err => DecodeError.TypeError(argOrEmpty("account no", v, err)))
  )

  given CellEncoder[AccountNo] = CellEncoder.from(AccountNo.unwrap(_))

  given CellDecoder[OrderNo] = CellDecoder.from[OrderNo](v =>
    OrderNo(v).validateNo.toEitherAssociative.leftMap(err => DecodeError.TypeError(argOrEmpty("order no", v, err)))
  )
  given CellEncoder[OrderNo] = CellEncoder.from(OrderNo.unwrap(_))

  given CellDecoder[ISINCode] = CellDecoder.from[ISINCode](v =>
    ISINCode
      .make(v)
      .toEitherAssociative
      .leftMap(err => DecodeError.TypeError(argOrEmpty("isin code", v, err)))
  )

  given CellEncoder[ISINCode] = CellEncoder.from(ISINCode.unwrap(_))

  given CellDecoder[UnitPrice] = CellDecoder.from[UnitPrice](v =>
    UnitPrice
      .make(BigDecimal(v))
      .toEitherAssociative
      .leftMap(err => DecodeError.TypeError(argOrEmpty("unit price", v, err)))
  )

  given CellEncoder[UnitPrice] = CellEncoder.from(_.toString)

  given CellDecoder[Quantity] = CellDecoder.from[Quantity](v =>
    Quantity
      .make(BigDecimal(v))
      .toEitherAssociative
      .leftMap(err => DecodeError.TypeError(argOrEmpty("quantity", v, err)))
  )

  given CellEncoder[Quantity] = CellEncoder.from(_.toString)

  given CellDecoder[BuySell] = CellDecoder.from[BuySell](v =>
    BuySell
      .withValue(v)
      .toEitherAssociative
      .leftMap(err => DecodeError.TypeError(argOrEmpty("buy sell", v, err)))
  )

  given CellEncoder[BuySell] = CellEncoder.from(_.entryName)

  given CellDecoder[Market] = CellDecoder.from[Market](v =>
    Market
      .withValue(v)
      .toEitherAssociative
      .leftMap(err => DecodeError.TypeError(argOrEmpty("market", v, err)))
  )

  given CellEncoder[Market] = CellEncoder.from(_.entryName)

  private def argOrEmpty(column: String, cell: String, error: String): String =
    if (cell.trim.isEmpty) s"Empty $column" else error
