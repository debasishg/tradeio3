package tradex.domain
package api
package endpoints

import common.BaseEndpoints
import sttp.tapir.ztapir.ZPartialServerEndpoint
import api.common.ErrorInfo
import sttp.tapir.ztapir.*
import sttp.tapir.json.zio.jsonBody
import model.instrument.*
import model.account.*
import model.order.*
import model.trade.*
import transport.instrumentT.{ given, * }
import transport.tradeT.{ given, * }
import java.time.{ LocalDate, LocalDateTime }
import zio.ZLayer
import cats.syntax.all.*
import tradex.domain.model.trade.Trade
import java.util.UUID

final case class TradingEndpoints(
    base: BaseEndpoints
):
  val getInstrumentEndpoint =
    base.publicEndpoint.get
      .in("api" / "instrument" / path[String]("isin"))
      .out(jsonBody[Instrument].example(Examples.exampleInstrument))

  val queryTradesByDateEndpoint =
    base.publicEndpoint.get
      .in("api" / "trade" / path[String]("accountno"))
      .in(query[LocalDate]("tradedate"))
      .out(jsonBody[List[Trade]]) // .example(Examples.exampleInstrument))

private object Examples:
  val exampleInstrument = Equity.equity(
    isin = ISINCode
      .make("US0378331005")
      .toEitherAssociative
      .leftMap(identity)
      .fold(err => throw new Exception(err), identity),
    name = InstrumentName(NonEmptyString("Apple Inc.")),
    lotSize = LotSize(100),
    issueDate = LocalDateTime.now(),
    unitPrice = UnitPrice
      .make(100)
      .toEitherAssociative
      .leftMap(identity)
      .fold(err => throw new Exception(err), identity)
  )

object TradingEndpoints:
  val live: ZLayer[BaseEndpoints, Nothing, TradingEndpoints] =
    ZLayer.fromFunction(TradingEndpoints.apply _)
