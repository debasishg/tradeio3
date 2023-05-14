package tradex.domain
package service

import zio.test.* 
import zio.test.Assertion.*
import zio.{ZIO, Scope}
import zio.stream.{ZStream, ZPipeline}
import java.nio.charset.StandardCharsets
import csv.CSV
import model.frontOfficeOrder.FrontOfficeOrder
import transport.frontOfficeOrderT.{given, *}
import service.live.FrontOfficeOrderParsingServiceLive
import repository.live.OrderRepositoryLive
import Fixture.appResourcesL
import service.generators.frontOfficeOrderGen
import tradex.domain.repository.OrderRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object FrontOfficeOrderParsingServiceSpec extends ZIOSpecDefault:
  val now = Instant.now
  override def spec: Spec[TestEnvironment & Scope, Any] = suite("FrontOfficeOrderParsingServiceSpec")(
    test("parse front office orders and create orders")(check(Gen.listOfN(10)(frontOfficeOrderGen(now))) { frontOfficeOrders =>
      for
        service <- ZIO.service[FrontOfficeOrderParsingService]
        reader <- ZStream
          .fromIterable(frontOfficeOrders)
          .via(CSV.encode[FrontOfficeOrder])
          .via(ZPipeline.decodeCharsWith(StandardCharsets.UTF_8))
          .toReader
        _ <- service.parse(reader)
        inserted <- ZIO.serviceWithZIO[OrderRepository](_.queryByOrderDate(LocalDate.ofInstant(now, ZoneOffset.UTC)))
      yield assertTrue(inserted.nonEmpty)
    }
  ))
  .provideSome[Scope](
    FrontOfficeOrderParsingServiceLive.layer,
    OrderRepositoryLive.layer,
    config.appConfig,
    appResourcesL.project(_.postgres),
    Sized.default,
    TestRandom.deterministic
  )
