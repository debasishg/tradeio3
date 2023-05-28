package tradex.domain
package api
package endpoints

import zio.{ ZIO, ZLayer }
import cats.syntax.all.*
import sttp.tapir.ztapir.*
import service.InstrumentService
import common.*
import common.ErrorMapper.defaultErrorsMappings
import scala.util.chaining.*
import model.instrument.ISINCode
import service.TradingService
import tradex.domain.model.account.AccountNo

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
        .pipe(r =>
          defaultErrorsMappings(r.someOrFail(Exceptions.NotFound(s"Instrument with ISIN $isin not found")))
            .fold(
              err => Left(err),
              ins => Right(ins)
            )
        )
    }

  val queryTradesByDateEndpoint: ZServerEndpoint[Any, Any] = tradingEndpoints.queryTradesByDateEndpoint
    .serverLogic { case (accountNo, tradeDate) =>
      tradingService
        .queryTradesForDate(
          AccountNo(accountNo).validateNo
            .fold(errs => throw new Exception(errs.toString), identity),
          tradeDate
        )
        .pipe(r =>
          defaultErrorsMappings(
            r.collect(Exceptions.NotFound(s"No trades found for $accountNo and $tradeDate")) {
              case trades if trades.nonEmpty => trades
            }
          )
            .fold(
              err => Left(err),
              ins => Right(ins)
            )
        )

    }

  val endpoints: List[ZServerEndpoint[Any, Any]] = List(
    getInstrumentEndpoint ++ queryTradesByDateEndpoint
  )

object TradingServerEndpoints:
  val live = ZLayer.fromFunction(TradingServerEndpoints.apply _)
