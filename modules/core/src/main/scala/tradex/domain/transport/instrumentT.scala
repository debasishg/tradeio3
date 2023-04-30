package tradex.domain
package transport

import zio.json.*
import cats.syntax.all.*
import model.instrument.*

object instrumentT {
  given JsonDecoder[ISINCode] =
    JsonDecoder[String].mapOrFail(ISINCode.make(_).toEither.leftMap(_.head))
  given JsonEncoder[ISINCode] = JsonEncoder[String].contramap(ISINCode.unwrap(_))

  given JsonDecoder[UnitPrice] =
    JsonDecoder[BigDecimal].mapOrFail(UnitPrice.make(_).toEither.leftMap(_.head))
  given JsonEncoder[UnitPrice] =
    JsonEncoder[BigDecimal].contramap(UnitPrice.unwrap(_))

  given JsonDecoder[LotSize] =
    JsonDecoder[Int].mapOrFail(LotSize.make(_).toEither.leftMap(_.head))
  given JsonEncoder[LotSize] =
    JsonEncoder[Int].contramap(LotSize.unwrap(_))

  given JsonCodec[InstrumentName]  = DeriveJsonCodec.gen[InstrumentName]
  given JsonCodec[CouponFrequency] = DeriveJsonCodec.gen[CouponFrequency]
  given JsonCodec[InstrumentBase]  = DeriveJsonCodec.gen[InstrumentBase]
  given JsonCodec[Equity]          = DeriveJsonCodec.gen[Equity]
  given JsonCodec[FixedIncome]     = DeriveJsonCodec.gen[FixedIncome]
  given JsonCodec[Ccy]             = DeriveJsonCodec.gen[Ccy]
}
