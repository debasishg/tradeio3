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

final case class TradingServerEndpoints(
    instrumentService: InstrumentService,
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

  val endpoints: List[ZServerEndpoint[Any, Any]] = List(
    getInstrumentEndpoint
  )

object TradingServerEndpoints:
  val live: ZLayer[InstrumentService with TradingEndpoints, Nothing, TradingServerEndpoints] =
    ZLayer.fromFunction(TradingServerEndpoints.apply _)
