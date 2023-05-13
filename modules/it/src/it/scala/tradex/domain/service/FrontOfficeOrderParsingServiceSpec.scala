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

object FrontOfficeOrderParsingServiceSpec extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment & Scope, Any] = suite("FrontOfficeOrderParsingServiceSpec")(
    test("parse order")(check(Gen.listOfN(10)(frontOfficeOrderGen)) { frontOfficeOrders =>
      for
        service <- ZIO.service[FrontOfficeOrderParsingService]
        reader <- ZStream
          .fromIterable(frontOfficeOrders)
          .via(CSV.encode[FrontOfficeOrder])
          .via(ZPipeline.decodeCharsWith(StandardCharsets.UTF_8))
          .toReader
        _ <- service.parse(reader)
      yield assertTrue(true)
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
