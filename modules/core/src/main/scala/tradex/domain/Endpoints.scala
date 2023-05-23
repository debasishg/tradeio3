package tradex.domain

import zio.{ Task, ZLayer }
import api.endpoints.TradingServerEndpoints
import sttp.tapir.ztapir.ZServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

final case class Endpoints(
    tradingServerEndpoints: TradingServerEndpoints
):
  val endpoints: List[ZServerEndpoint[Any, Any]] = {
    val api  = tradingServerEndpoints.endpoints
    val docs = docsEndpoints(api)
    api ++ docs
  }

  private def docsEndpoints(apiEndpoints: List[ZServerEndpoint[Any, Any]]): List[ZServerEndpoint[Any, Any]] =
    SwaggerInterpreter()
      .fromServerEndpoints[Task](apiEndpoints, "trading-back-office", "0.1.0")

object Endpoints:
  val live: ZLayer[
    TradingServerEndpoints,
    Nothing,
    Endpoints
  ] =
    ZLayer.fromFunction(Endpoints.apply _)
