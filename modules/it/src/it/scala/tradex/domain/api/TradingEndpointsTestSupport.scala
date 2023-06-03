package tradex.domain
package api

import zio.*
import sttp.model.Uri
import sttp.client3.*
import sttp.client3.ziojson.*
import api.endpoints.TradingEndpoints
import sttp.tapir.ztapir.ZServerEndpoint
import api.endpoints.TradingServerEndpoints
import TestUtils.*
import zio.json.JsonCodec

object TradingEndpointsTestSupport:
  def callGetInstrumentEndpoint(
      uri: Uri
  ): ZIO[TradingServerEndpoints, Throwable, Either[ResponseException[String, String], InstrumentResponse]] =
    val getInstrumentEndpoint =
      ZIO
        .service[TradingServerEndpoints]
        .map(_.getInstrumentEndpoint)

    val requestWithUri = basicRequest.get(uri)
    executeRequest[InstrumentResponse](requestWithUri, getInstrumentEndpoint)

  def callAddEquityEndpoint(
      uri: Uri,
      equityData: AddEquityData
  ): ZIO[TradingServerEndpoints, Throwable, Either[ResponseException[String, String], InstrumentResponse]] =
    ZIO
      .service[TradingServerEndpoints]
      .map(_.addEquityEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .put(uri)
          .body(AddEquityRequest(equityData))
          .response(asJson[InstrumentResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  private def executeRequest[T: JsonCodec](
      requestWithUri: Request[Either[String, String], Any],
      endpoint: ZIO[TradingServerEndpoints, Nothing, ZServerEndpoint[Any, Any]]
  ) =
    endpoint
      .flatMap { endpoint =>
        requestWithUri
          .response(asJson[T])
          .send(backendStub(endpoint))
          .map(_.body)
      }
