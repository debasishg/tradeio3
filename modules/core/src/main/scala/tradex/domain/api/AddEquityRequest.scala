package tradex.domain
package api

import model.instrument.*
import java.time.LocalDateTime
import zio.json.*
import transport.instrumentT.{ given, * }
import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

final case class AddEquityRequest(
    equityData: AddEquityData
)

object AddEquityRequest:
  given JsonCodec[AddEquityRequest] = DeriveJsonCodec.gen[AddEquityRequest]
  given Schema[AddEquityRequest]    = Schema.derived[AddEquityRequest]

final case class AddEquityData(
    isin: ISINCode,
    name: InstrumentName,
    lotSize: LotSize,
    issueDate: LocalDateTime,
    unitPrice: UnitPrice
)

object AddEquityData:
  given JsonCodec[AddEquityData] = DeriveJsonCodec.gen[AddEquityData]
