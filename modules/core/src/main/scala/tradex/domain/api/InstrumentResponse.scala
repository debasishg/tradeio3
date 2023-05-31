package tradex.domain
package api

import zio.json.*
import model.instrument.*
import transport.instrumentT.{ given, * }
import sttp.tapir.Schema

final case class InstrumentResponse(
    instrument: Instrument
)

object InstrumentResponse:
  given JsonCodec[InstrumentResponse] = DeriveJsonCodec.gen[InstrumentResponse]
  given Schema[InstrumentResponse]    = Schema.derived[InstrumentResponse]
