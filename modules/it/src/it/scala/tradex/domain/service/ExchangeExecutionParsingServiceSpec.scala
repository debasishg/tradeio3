package tradex.domain
package service

import zio.test.*
import zio.test.Assertion.*
import zio.{ Scope, ZIO }
import zio.stream.{ ZPipeline, ZStream }
import java.nio.charset.StandardCharsets
import java.time.{ Instant, LocalDate, ZoneOffset }
import csv.CSV
import transport.exchangeExecutionT.{ given, * }
import service.live.ExchangeExecutionParsingServiceLive
import generators.exchangeExecutionGen
import repository.live.ExecutionRepositoryLive
import repository.ExecutionRepository
import Fixture.appResourcesL
import model.exchangeExecution.ExchangeExecution

object ExchangeExecutionParsingServiceSpec extends ZIOSpecDefault:
  val now = Instant.now
  override def spec = suite("ExchangeExecutionParsingServiceSpec")(
    test("parse exchange executions")(check(Gen.listOfN(10)(exchangeExecutionGen(now))) { exchangeExecutions =>
      for
        service <- ZIO.service[ExchangeExecutionParsingService]
        reader <- ZStream
          .fromIterable(exchangeExecutions)
          .via(CSV.encode[ExchangeExecution])
          .via(ZPipeline.decodeCharsWith(StandardCharsets.UTF_8))
          .toReader
        _ <- service.parse(reader)
        inserted <- ZIO.serviceWithZIO[ExecutionRepository](
          _.query(LocalDate.ofInstant(now, ZoneOffset.UTC))
        )
      yield assertTrue(exchangeExecutions.size == inserted.size)
    }) @@ TestAspect.before(clean)
  )
    .provideSome[Scope](
      ExchangeExecutionParsingServiceLive.layer,
      ExecutionRepositoryLive.layer,
      config.appConfig,
      appResourcesL.project(_.postgres),
      Sized.default,
      TestRandom.deterministic
    )

  def clean =
    ZIO.serviceWithZIO[ExecutionRepository](_.cleanAllExecutions)
