package tradex.domain

import zio.config.*, typesafe.*, magnolia.*
import zio.{ Config, TaskLayer, ZLayer }
import zio.Config.Secret

object config:
  final case class AppConfig(postgreSQL: AppConfig.PostgreSQLConfig, tradingConfig: AppConfig.TradingConfig)

  object AppConfig:
    final case class PostgreSQLConfig(
        host: NonEmptyString,
        port: Int,
        user: NonEmptyString,
        password: Secret,
        database: NonEmptyString,
        max: Int
    )

    final case class TradingConfig(
        maxAccountNoLength: Int,
        minAccountNoLength: Int,
        zeroBalanceAllowed: Boolean
    )

  type AllConfig = AppConfig with AppConfig.TradingConfig with AppConfig.PostgreSQLConfig

  final val Root = "tradex"

  private final val Descriptor = deriveConfig[AppConfig]

  private val appConfig = ZLayer(TypesafeConfigProvider.fromResourcePath().nested(Root).load(Descriptor))

  val live: TaskLayer[AllConfig] = appConfig >+>
    appConfig.project(_.postgreSQL) >+>
    appConfig.project(_.tradingConfig)
