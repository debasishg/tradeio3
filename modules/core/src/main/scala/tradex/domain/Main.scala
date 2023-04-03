package tradex.domain

import zio.{ Scope, Task, UIO, ZIO, ZIOAppDefault, ZLayer }
import zio.interop.catz.*
import cats.effect.std.Console
import cats.effect.Resource
import natchez.Trace.Implicits.noop
import tradex.domain.repository.AccountRepository
import tradex.domain.repository.live.AccountRepositoryLive
import tradex.domain.resources.AppResources
import tradex.domain.config.AppConfig
import service.AccountService

object Main extends ZIOAppDefault:

  case class AccountServiceDummy(repo: AccountRepository):
    def invoke: UIO[Unit] = ZIO.log("Sample account service")

  object AccountServiceDummy:
    val layer = ZLayer.fromFunction(AccountServiceDummy.apply _)

  given Console[Task] = Console.make[Task]

  val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
    for
      config <- ZIO.service[AppConfig]
      res    <- AppResources.make(config).toScopedZIO
    yield res
  )

  val live: ZLayer[Any, Throwable, AccountServiceDummy] = ZLayer.make[AccountServiceDummy](
    AccountServiceDummy.layer,
    AccountRepositoryLive.layer,
    appResourcesL.project(_.postgres),
    config.live
  )

  /** Another pattern for stream based computation */
  def streamUsecase: ZIO[AppResources, Throwable, Long] =
    ZIO
      .serviceWith[AppResources](_.postgres)
      .flatMap(_.use(session => AccountRepositoryLive.streamAccountsAndDoStuff(session)))

  /** Pattern for invoking service APIs that return streams */
  def service: ZIO[AppResources, Throwable, Long] =
    ZIO
      .serviceWith[AppResources](_.postgres)
      .flatMap(res =>
        res
          .evalMap(AccountService.fromSession(_))
          .use(_.allAccounts.runCount)
      )

  override def run =
    streamUsecase.provide(config.live >>> appResourcesL) *>
      ZIO.serviceWithZIO[AccountServiceDummy](_.invoke).provide(live)
