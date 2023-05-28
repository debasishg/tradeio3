package tradex.domain
package transport

import zio.json.*
import cats.syntax.all.*
import model.order.*
import model.market.*
import instrumentT.{ given, * }
import accountT.{ given, * }
import sttp.tapir.Schema
import sttp.tapir.SchemaType

object orderT {
  given JsonDecoder[OrderNo] =
    JsonDecoder[String].mapOrFail(OrderNo.make(_).toEither.leftMap(_.head))
  given JsonEncoder[OrderNo] = JsonEncoder[String].contramap(OrderNo.unwrap(_))

  given JsonCodec[BuySell] = DeriveJsonCodec.gen[BuySell]
  given JsonCodec[Market]  = DeriveJsonCodec.gen[Market]
  given JsonDecoder[Quantity] =
    JsonDecoder[BigDecimal].mapOrFail(Quantity.make(_).toEither.leftMap(_.head))
  given JsonEncoder[Quantity] =
    JsonEncoder[BigDecimal].contramap(Quantity.unwrap(_))

  given JsonCodec[LineItem] = DeriveJsonCodec.gen[LineItem]
  given JsonCodec[Order]    = DeriveJsonCodec.gen[Order]

  given Schema[Quantity] = Schema(SchemaType.SNumber())
}
