package tradex.domain
package service

import zio.test.*
import zio.test.Assertion.*
import zio.{ Scope, ZIO }
import zio.stream.{ ZPipeline, ZStream }
import java.nio.charset.StandardCharsets
import java.time.{ Instant, LocalDate, ZoneOffset }
import csv.CSV
import model.frontOfficeOrder.FrontOfficeOrder
import transport.frontOfficeOrderT.{ given, * }
import service.live.FrontOfficeOrderParsingServiceLive
import generators.frontOfficeOrderGen
import repository.live.OrderRepositoryLive
import repository.OrderRepository
import Fixture.appResourcesL

object FrontOfficeOrderParsingServiceSpec extends ZIOSpecDefault:
  val now = Instant.now
  override def spec = suite("FrontOfficeOrderParsingServiceSpec")(
    test("parse front office orders and create orders")(check(Gen.listOfN(10)(frontOfficeOrderGen(now))) {
      frontOfficeOrders =>
        for
          service <- ZIO.service[FrontOfficeOrderParsingService]
          reader <- ZStream
            .fromIterable(frontOfficeOrders)
            .via(CSV.encode[FrontOfficeOrder])
            .via(ZPipeline.decodeCharsWith(StandardCharsets.UTF_8))
            .toReader
          _        <- service.parse(reader)
          inserted <- ZIO.serviceWithZIO[OrderRepository](_.queryByOrderDate(LocalDate.ofInstant(now, ZoneOffset.UTC)))
        yield assertTrue(inserted.nonEmpty)
    }) @@ TestAspect.before(clean)
  )
    .provideSome[Scope](
      FrontOfficeOrderParsingServiceLive.layer,
      OrderRepositoryLive.layer,
      config.appConfig,
      appResourcesL.project(_.postgres),
      Sized.default,
      TestRandom.deterministic
    )

  def clean =
    ZIO.serviceWithZIO[OrderRepository](_.cleanAllOrders)
