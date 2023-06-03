package tradex.domain
package api

import zio.*
import zio.test.*
import zio.test.Assertion.*
import sttp.client3.UriContext
import api.endpoints.{ TradingEndpoints, TradingServerEndpoints }
import api.common.BaseEndpoints
import resources.AppResources
import tradex.domain.config.AppConfig
import zio.interop.catz.*
import natchez.Trace.Implicits.noop
import cats.effect.std.Console
import service.live.{ InstrumentServiceLive, TradingServiceLive }
import repository.live.{ InstrumentRepositoryLive, OrderRepositoryLive, TradeRepositoryLive }
import Fixture.appResourcesL
import sttp.client3.HttpError

object TradingEndpointsSpec extends ZIOSpecDefault:

  override def spec = suite("trading endpoints tests")(
    suite("getInstrumentEndpoint")(
      test("should return instrument")(
        for
          _ <- RepositoryTestSupport.insertOneEquity
          instrument <- TradingEndpointsTestSupport.callGetInstrumentEndpoint(
            uri"http://test.com/api/instrument/US30303M1027"
          )
        yield assert(instrument)(isRight(anything))
      ),
      test("should return 404")(
        for
          _ <- RepositoryTestSupport.insertOneEquity
          instrument <- TradingEndpointsTestSupport.callGetInstrumentEndpoint(
            uri"http://test.com/api/instrument/US30303M1029"
          )
        yield assert(instrument)(
          isLeft(
            equalTo(
              HttpError(
                body = "{\"error\":\"Instrument with ISIN US30303M1029 not found\"}",
                statusCode = sttp.model.StatusCode(404)
              )
            )
          )
        )
      ),
      test("should fail in validation of ISIN code - internal server error")(
        for
          _ <- RepositoryTestSupport.insertOneEquity
          instrument <- TradingEndpointsTestSupport.callGetInstrumentEndpoint(
            uri"http://test.com/api/instrument/US30303M10"
          )
        yield assert(instrument)( // fails through defect handler
          isLeft(
            equalTo(
              HttpError(
                body = "Internal server error",
                statusCode = sttp.model.StatusCode(500)
              )
            )
          )
        )
      )
    ),
    suite("addEquityEndpoint")(
      test("should add equity")(
        for
          instrument <- TradingEndpointsTestSupport.callAddEquityEndpoint(
            uri"http://test.com/api/instrument/equity",
            RepositoryTestSupport.addEquityData
          )
        yield assert(instrument)(isRight(anything))
      )
    )
  )
    .provide(
      InstrumentRepositoryLive.layer,
      OrderRepositoryLive.layer,
      TradeRepositoryLive.layer,
      InstrumentServiceLive.layer,
      TradingServiceLive.layer,
      TradingServerEndpoints.live,
      TradingEndpoints.live,
      BaseEndpoints.live,
      tradex.domain.config.appConfig,
      appResourcesL.project(_.postgres),
      appResourcesL
    )
