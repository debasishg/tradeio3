package tradex.domain
package resources

import zio.{ Task, ZIO }
import skunk.{ Session, SessionPool }
import skunk.util.Typer
import skunk.codec.text._
import skunk.implicits._
import natchez.Trace.Implicits.noop // needed for skunk
import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._
import cats.effect.kernel.{ Resource, Temporal }
import fs2.io.net.Network
import zio.interop.catz.*
import tradex.domain.config.AppConfig
import config.AppConfig.PostgreSQLConfig

sealed abstract class AppResources private (
    val postgres: Resource[Task, Session[Task]]
)

object AppResources {

  def make(
      cfg: AppConfig
  )(using Temporal[Task], natchez.Trace[Task], Network[Task], Console[Task]): Resource[Task, AppResources] = {

    def checkPostgresConnection(
        postgres: Resource[Task, Session[Task]]
    ): Task[Unit] =
      postgres.use { session =>
        session.unique(sql"select version();".query(text)).flatMap { v =>
          ZIO.logInfo(s"Connected to Postgres $v")
        }
      }

    def mkPostgreSqlResource(c: PostgreSQLConfig): SessionPool[Task] =
      Session
        .pooled[Task](
          host = c.host,
          port = c.port,
          user = c.user,
          password = Some(c.password.toString()),
          database = c.database,
          max = c.max,
          strategy = Typer.Strategy.SearchPath
        )
        .evalTap(checkPostgresConnection)

    mkPostgreSqlResource(cfg.postgreSQL).map(r => new AppResources(r) {})
  }
}
