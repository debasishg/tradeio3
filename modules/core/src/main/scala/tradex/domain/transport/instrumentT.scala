package tradex.domain
package transport

import zio.json.*
import cats.syntax.all.*
import model.instrument.*
import sttp.tapir.Schema
import sttp.tapir.generic.auto.*
import zio.config.magnolia.examples.P.S
import sttp.tapir.SchemaType

object instrumentT:
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
  given JsonCodec[Instrument]      = DeriveJsonCodec.gen[Instrument]

  given Schema[ISINCode]        = Schema.string
  given Schema[LotSize]         = Schema(SchemaType.SInteger())
  given Schema[UnitPrice]       = Schema(SchemaType.SNumber())
  given Schema[InstrumentName]  = Schema.derivedSchema
  given Schema[CouponFrequency] = Schema.derivedSchema
  given Schema[InstrumentBase]  = Schema.derivedSchema
  given Schema[Equity]          = Schema.derivedSchema
  given Schema[FixedIncome]     = Schema.derivedSchema
  given Schema[Ccy]             = Schema.derivedSchema
  given Schema[Instrument]      = Schema.derivedSchema
