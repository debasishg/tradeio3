package tradex.domain
package api

import zio.ZIO
import model.instrument.*
import cats.syntax.all.*
import java.time.LocalDateTime
import repository.InstrumentRepository

object RepositoryTestSupport:
  val exampleInstrument = Equity.equity(
    isin = ISINCode
      .make("US30303M1027")
      .toEitherAssociative
      .leftMap(identity)
      .fold(err => throw new Exception(err), identity),
    name = InstrumentName(NonEmptyString("Meta")),
    lotSize = LotSize(100),
    issueDate = LocalDateTime.now(),
    unitPrice = UnitPrice
      .make(100)
      .toEitherAssociative
      .leftMap(identity)
      .fold(err => throw new Exception(err), identity)
  )

  def insertOneEquity: ZIO[InstrumentRepository, Throwable, Instrument] =
    ZIO.serviceWithZIO[InstrumentRepository](_.store(exampleInstrument))
