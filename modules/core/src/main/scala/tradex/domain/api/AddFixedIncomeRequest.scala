package tradex.domain
package api

import model.instrument.*
import java.time.LocalDateTime
import zio.json.*
import transport.instrumentT.{ *, given }
import sttp.tapir.Schema
import sttp.tapir.generic.auto.*
import squants.market.Money

final case class AddFixedIncomeRequest(
    fiData: AddFixedIncomeData
)

object AddFixedIncomeRequest:
  given JsonCodec[AddFixedIncomeRequest] = DeriveJsonCodec.gen[AddFixedIncomeRequest]
  given Schema[AddFixedIncomeRequest]    = Schema.derived[AddFixedIncomeRequest]

final case class AddFixedIncomeData(
    isin: ISINCode,
    name: InstrumentName,
    lotSize: LotSize,
    issueDate: LocalDateTime,
    maturityDate: Option[LocalDateTime],
    couponRate: Money,
    couponFrequency: CouponFrequency
)

object AddFixedIncomeData:
  given JsonCodec[AddFixedIncomeData] = DeriveJsonCodec.gen[AddFixedIncomeData]
