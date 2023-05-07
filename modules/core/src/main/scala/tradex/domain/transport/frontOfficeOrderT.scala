package tradex.domain
package transport

import kantan.csv.CellDecoder
import zio.prelude.{ BicovariantOps, Validation }
import kantan.csv.{ DecodeError, RowDecoder }
import kantan.csv.java8.*
import model.account.AccountNo
import model.instrument.{ ISINCode, UnitPrice }
import model.order.{ BuySell, Quantity }
import model.frontOfficeOrder.FrontOfficeOrder

object frontOfficeOrderT:
  given CellDecoder[AccountNo] = CellDecoder.from[AccountNo](v =>
    AccountNo(v).validateNo.toEitherAssociative.leftMap(err => DecodeError.TypeError(argOrEmpty("account no", v, err)))
  )

  given CellDecoder[ISINCode] = CellDecoder.from[ISINCode](v =>
    ISINCode
      .make(v)
      .toEitherAssociative
      .leftMap(err => DecodeError.TypeError(argOrEmpty("isin code", v, err)))
  )

  given CellDecoder[UnitPrice] = CellDecoder.from[UnitPrice](v =>
    UnitPrice
      .make(BigDecimal(v))
      .toEitherAssociative
      .leftMap(err => DecodeError.TypeError(argOrEmpty("unit price", v, err)))
  )

  given CellDecoder[Quantity] = CellDecoder.from[Quantity](v =>
    Quantity
      .make(BigDecimal(v))
      .toEitherAssociative
      .leftMap(err => DecodeError.TypeError(argOrEmpty("quantity", v, err)))
  )

  given CellDecoder[BuySell] = CellDecoder.from[BuySell](v =>
    BuySell
      .withValue(v)
      .toEitherAssociative
      .leftMap(err => DecodeError.TypeError(argOrEmpty("buy sell", v, err)))
  )

  given RowDecoder[FrontOfficeOrder] = RowDecoder.decoder(0, 1, 2, 3, 4, 5)(FrontOfficeOrder.apply)

  private def argOrEmpty(column: String, cell: String, error: String): String =
    if (cell.trim.isEmpty) s"Empty $column" else error
