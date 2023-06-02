package tradex.domain
package api
package endpoints

import zio.ZLayer
import cats.syntax.all.*
import java.util.UUID
import common.BaseEndpoints
import sttp.tapir.ztapir.ZPartialServerEndpoint
import api.common.ErrorInfo
import sttp.tapir.ztapir.*
import sttp.tapir.json.zio.jsonBody
import model.instrument.*
import model.account.*
import model.order.*
import model.trade.*
import model.market.Market
import model.user.UserId
import squants.market.USD
import transport.instrumentT.{ given, * }
import transport.tradeT.{ given, * }
import java.time.{ LocalDate, LocalDateTime }

final case class TradingEndpoints(
    base: BaseEndpoints
):
  val getInstrumentEndpoint =
    base.publicEndpoint.get
      .in("api" / "instrument" / path[String]("isin"))
      .out(jsonBody[InstrumentResponse].example(Examples.instrumentResponse))

  val addEquityEndpoint =
    base.publicEndpoint.put
      .in("api" / "instrument" / "equity")
      .in(jsonBody[AddEquityRequest].example(Examples.addEquityRequest))
      .out(jsonBody[InstrumentResponse].example(Examples.instrumentResponse))

  val addFixedIncomeEndpoint =
    base.publicEndpoint.put
      .in("api" / "instrument" / "fi")
      .in(jsonBody[AddFixedIncomeRequest]) // .example(Examples.addFixedIncomeRequest))
      .out(jsonBody[InstrumentResponse].example(Examples.instrumentResponse))

  val queryTradesByDateEndpoint =
    base.publicEndpoint.get
      .in("api" / "trade" / path[String]("accountno"))
      .in(query[LocalDate]("tradedate"))
      .out(jsonBody[List[Trade]].example(List(Examples.trade)))

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
  val addEquityRequest = AddEquityRequest(
    AddEquityData(
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
  )
  val instrumentResponse = InstrumentResponse(exampleInstrument)
  val trade =
    Trade(
      TradeRefNo(UUID.randomUUID()),
      AccountNo("ibm-123"),
      ISINCode("US0378331005"),
      Market.NewYork,
      BuySell.Buy,
      UnitPrice.wrap(BigDecimal(12.25)),
      Quantity.wrap(100),
      LocalDateTime.now,
      None,
      Some(UserId(UUID.randomUUID())),
      List(TradeTaxFee(TaxFeeId.TradeTax, USD(245)), TradeTaxFee(TaxFeeId.Commission, USD(183.75))),
      None
    )

object TradingEndpoints:
  val live: ZLayer[BaseEndpoints, Nothing, TradingEndpoints] =
    ZLayer.fromFunction(TradingEndpoints.apply _)
