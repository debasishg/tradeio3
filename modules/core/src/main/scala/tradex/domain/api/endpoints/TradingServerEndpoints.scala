package tradex.domain
package api
package endpoints

import zio.{ ZIO, ZLayer }
import cats.syntax.all.*
import sttp.tapir.ztapir.*
import service.{ InstrumentService, TradingService }
import common.*
import common.ErrorMapper.defaultErrorsMappings
import scala.util.chaining.*
import model.instrument.ISINCode
import model.account.AccountNo

final case class TradingServerEndpoints(
    instrumentService: InstrumentService,
    tradingService: TradingService,
    tradingEndpoints: TradingEndpoints
):
  val getInstrumentEndpoint: ZServerEndpoint[Any, Any] = tradingEndpoints.getInstrumentEndpoint
    .serverLogic { isin =>
      instrumentService
        .query(
          ISINCode
            .make(isin)
            .toEitherAssociative
            .leftMap(identity)
            .fold(err => throw new Exception(err), identity)
        )
        .logError
        .pipe(r => defaultErrorsMappings(r.someOrFail(Exceptions.NotFound(s"Instrument with ISIN $isin not found"))))
        .map(InstrumentResponse.apply)
        .either
    }

  val addEquityEndpoint: ZServerEndpoint[Any, Any] = tradingEndpoints.addEquityEndpoint
    .serverLogic(data =>
      instrumentService
        .addEquity(
          data.equityData.isin,
          data.equityData.name,
          data.equityData.lotSize,
          data.equityData.issueDate,
          data.equityData.unitPrice
        )
        .logError
        .pipe(defaultErrorsMappings)
        .map(InstrumentResponse.apply)
        .either
    )

  val addFixedIncomeEndpoint: ZServerEndpoint[Any, Any] = tradingEndpoints.addFixedIncomeEndpoint
    .serverLogic(data =>
      instrumentService
        .addFixedIncome(
          data.fiData.isin,
          data.fiData.name,
          data.fiData.lotSize,
          data.fiData.issueDate,
          data.fiData.maturityDate,
          data.fiData.couponRate,
          data.fiData.couponFrequency
        )
        .logError
        .pipe(defaultErrorsMappings)
        .map(InstrumentResponse.apply)
        .either
    )

  val queryTradesByDateEndpoint: ZServerEndpoint[Any, Any] = tradingEndpoints.queryTradesByDateEndpoint.serverLogic {
    case (accountNo, tradeDate) =>
      tradingService
        .queryTradesForDate(
          AccountNo(accountNo).validateNo
            .fold(errs => throw new Exception(errs.toString), identity),
          tradeDate
        )
        .logError
        .pipe(r =>
          defaultErrorsMappings(
            r.collect(Exceptions.NotFound(s"No trades found for $accountNo and $tradeDate")) {
              case trades if trades.nonEmpty => trades
            }
          ).either
        )
  }

  val endpoints: List[ZServerEndpoint[Any, Any]] = List(
    getInstrumentEndpoint,
    addEquityEndpoint,
    addFixedIncomeEndpoint,
    queryTradesByDateEndpoint
  )

object TradingServerEndpoints:
  val live = ZLayer.fromFunction(TradingServerEndpoints.apply _)
