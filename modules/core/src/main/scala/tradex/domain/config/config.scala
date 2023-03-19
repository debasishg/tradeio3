package tradex.domain
package config

import tradex.domain.NonEmptyString
import zio.Config.Secret

object config {
  case class AppConfig(postgreSQL: PostgreSQLConfig)

  case class PostgreSQLConfig(
      host: NonEmptyString,
      port: Int,
      user: NonEmptyString,
      password: Secret,
      database: NonEmptyString,
      max: Int
  )

}
