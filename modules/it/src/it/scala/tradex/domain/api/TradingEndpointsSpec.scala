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

object TradingEndpointsSpec extends ZIOSpecDefault:
  given Console[Task] = Console.make[Task]
  val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
    for
      config <- ZIO.service[AppConfig]
      res    <- AppResources.make(config).toScopedZIO
    yield res
  )

  override def spec = suite("trading endpoints tests")(
    suite("getInstrumentEndpoint")(
      test("should return instrument")(
        for
          _ <- RepositoryTestSupport.insertOneEquity
          instrument <- TradingEndpointsTestSupport.callGetInstrumentEndpoint(
            uri"http://test.com/api/instrument/US30303M1027"
          )
        yield assert(instrument)(isRight(anything))
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
    )
  )
