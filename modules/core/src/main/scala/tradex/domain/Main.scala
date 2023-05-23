package tradex.domain

import zio.{ Console => ZConsole, * }
import zio.logging.backend.SLF4J
import zio.logging.LogFormat
import zio.http.Server
import zio.interop.catz.*
import sttp.tapir.server.ziohttp.ZioHttpServerOptions
import api.common.{ CustomDecodeFailureHandler, DefectHandler }
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import api.endpoints.{ TradingEndpoints, TradingServerEndpoints }
import service.live.InstrumentServiceLive
import api.common.BaseEndpoints
import repository.live.InstrumentRepositoryLive
import tradex.domain.config.AppConfig
import resources.AppResources
import natchez.Trace.Implicits.noop
import cats.effect.std.Console

object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = SLF4J.slf4j(LogFormat.colored)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
    val options: ZioHttpServerOptions[Any] = ZioHttpServerOptions.customiseInterceptors
      .exceptionHandler(new DefectHandler())
      .decodeFailureHandler(CustomDecodeFailureHandler.create())
      .options

    given Console[Task] = Console.make[Task]
    val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
      for
        config <- ZIO.service[AppConfig]
        res    <- AppResources.make(config).toScopedZIO
      yield res
    )

    (for
      endpoints <- ZIO.service[Endpoints]
      httpApp = ZioHttpInterpreter(options).toHttp(endpoints.endpoints)
      actualPort <- Server.install(httpApp.withDefaultErrorResponse)
      _          <- ZConsole.printLine(s"Trading Application started")
      _          <- ZConsole.printLine(s"Go to http://localhost:$actualPort/docs to open SwaggerUI")
      _          <- ZIO.never
    yield ())
      .provide(
        Endpoints.live,
        TradingServerEndpoints.live,
        TradingEndpoints.live,
        BaseEndpoints.live,
        InstrumentServiceLive.layer,
        InstrumentRepositoryLive.layer,
        appResourcesL.project(_.postgres),
        tradex.domain.config.appConfig,
        Server.defaultWithPort(port)
      )
      .exitCode
