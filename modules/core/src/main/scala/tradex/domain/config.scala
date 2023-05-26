package tradex.domain

import zio.config.*, typesafe.*, magnolia.*
import zio.{ Config, TaskLayer, ZLayer }
import zio.Config.Secret

object config:
  final case class AppConfig(
      postgreSQL: AppConfig.PostgreSQLConfig,
      httpServer: AppConfig.HttpServerConfig,
      tradingConfig: AppConfig.TradingConfig
  )

  object AppConfig:
    final case class PostgreSQLConfig(
        host: NonEmptyString,
        port: Int,
        user: NonEmptyString,
        password: NonEmptyString, // @todo : need to change to Secret
        database: NonEmptyString,
        max: Int
    )
    final case class HttpServerConfig(host: NonEmptyString, port: Int)

    final case class TradingConfig(
        maxAccountNoLength: Int,
        minAccountNoLength: Int,
        zeroBalanceAllowed: Boolean
    )

  final val Root = "tradex"

  private final val Descriptor = deriveConfig[AppConfig]

  val appConfig = ZLayer(TypesafeConfigProvider.fromResourcePath().nested(Root).load(Descriptor))
