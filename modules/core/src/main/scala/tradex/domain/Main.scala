package tradex.domain

import zio.{ Scope, Task, UIO, ZIO, ZIOAppDefault, ZLayer }
import zio.interop.catz.*
import cats.effect.std.Console
import natchez.Trace.Implicits.noop
import tradex.domain.repository.AccountRepository
import tradex.domain.repository.live.AccountRepositoryLive
import tradex.domain.resources.AppResources
import tradex.domain.config.AppConfig

object Main extends ZIOAppDefault:

  case class AccountService(repo: AccountRepository):
    def invoke: UIO[Unit] = ZIO.log("Sample account service")

  object AccountService:
    val layer = ZLayer.fromFunction(AccountService.apply _)

  given Console[Task] = Console.make[Task]

  val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
    for {
      config <- ZIO.service[AppConfig]
      res    <- AppResources.make(config).toScopedZIO
    } yield res
  )

  val live: ZLayer[Any, Throwable, AccountService] = ZLayer.make[AccountService](
    AccountService.layer,
    AccountRepositoryLive.layer,
    appResourcesL.project(_.postgres),
    config.live
  )

  def streamUsecase =
    ZIO
      .serviceWith[AppResources](_.postgres)
      .flatMap { res =>
        res.use { session =>
          AccountRepositoryLive.streamAccountsAndDoStuff(session)
        }
      }

  override def run =
    streamUsecase.provide(config.live >>> appResourcesL) *>
      ZIO.serviceWithZIO[AccountService](_.invoke).provide(live)
