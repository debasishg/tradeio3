package tradex.domain

import org.flywaydb.core.Flyway
import zio.Task
import config._
import zio.ZIO

object FlywayMigration {
  def migrate(config: AppConfig.PostgreSQLConfig): Task[Unit] =
    ZIO
      .attemptBlocking(
        Flyway
          .configure(this.getClass.getClassLoader)
          .dataSource(
            s"jdbc:postgresql://${config.host}:${config.port}/${config.database}",
            config.user,
            config.password.toString()
          )
          .locations("migrations")
          .connectRetries(Int.MaxValue)
          .load()
          .migrate()
      )
      .unit
}
