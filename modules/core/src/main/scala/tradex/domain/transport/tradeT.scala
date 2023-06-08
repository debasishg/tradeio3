package tradex.domain
package transport

import zio.json.*
import sttp.tapir.{ Schema, SchemaType }
import sttp.tapir.generic.auto.*
import java.util.UUID
import cats.syntax.all.*
import model.trade.*
import accountT.{ *, given }
import instrumentT.{ *, given }
import orderT.{ *, given }
import userT.{ *, given }

object tradeT:
  given JsonDecoder[TradeRefNo] =
    JsonDecoder[UUID].mapOrFail(TradeRefNo.make(_).toEither.leftMap(_.head))
  given JsonEncoder[TradeRefNo] = JsonEncoder[UUID].contramap(TradeRefNo.unwrap(_))

  given JsonCodec[TaxFeeId]    = DeriveJsonCodec.gen[TaxFeeId]
  given JsonCodec[TradeTaxFee] = DeriveJsonCodec.gen[TradeTaxFee]
  given JsonCodec[Trade]       = DeriveJsonCodec.gen[Trade]

  given Schema[TradeRefNo]  = Schema.string
  given Schema[TradeTaxFee] = Schema.derived[TradeTaxFee]
  given Schema[Trade]       = Schema.derived[Trade]
