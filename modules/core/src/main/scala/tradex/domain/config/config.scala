package tradex.domain
package config

import zio.config._, typesafe._, magnolia._
import zio.{ Config, TaskLayer, ZLayer }
import zio.Config.Secret
import config.AppConfig._

object config:
  final case class AppConfig(postgreSQL: PostgreSQLConfig, tradingConfig: TradingConfig)

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

  type AllConfig = AppConfig with TradingConfig with PostgreSQLConfig

  final val Root = "tradex"

  private final val Descriptor = deriveConfig[AppConfig]

  val appConfig = ZLayer(TypesafeConfigProvider.fromResourcePath().nested(Root).load(Descriptor))

  val live: TaskLayer[AllConfig] = appConfig >+>
    appConfig.project(_.postgreSQL) >+>
    appConfig.project(_.tradingConfig)
