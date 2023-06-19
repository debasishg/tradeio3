package tradex.domain
package service
package live

import zio.{ Task, UIO, ZIO, ZLayer }
import repository.InstrumentRepository
import model.instrument.*
import java.time.LocalDateTime
import squants.market.Money
import zio.prelude.ZValidation.{ Failure, Success }

final case class InstrumentServiceLive(
    repository: InstrumentRepository
) extends InstrumentService:
  override def query(isin: ISINCode): Task[Option[Instrument]] =
    repository.query(isin)

  override def addEquity(
      isin: ISINCode,
      name: InstrumentName,
      lotSize: LotSize,
      issueDate: LocalDateTime,
      unitPrice: UnitPrice
  ): UIO[Instrument] =
    repository.store(Equity.equity(isin, name, lotSize, issueDate, unitPrice))

  override def addFixedIncome(
      isin: ISINCode,
      name: InstrumentName,
      lotSize: LotSize,
      issueDate: LocalDateTime,
      maturityDate: Option[LocalDateTime],
      couponRate: Money,
      couponFrequency: CouponFrequency
  ): Task[Instrument] =
    FixedIncome.fixedIncome(isin, name, lotSize, issueDate, maturityDate, couponRate, couponFrequency) match
      case Success(_, fi) => repository.store(fi)
      case Failure(_, errs) =>
        ZIO.fail(new IllegalArgumentException(s"Invalid FixedIncome: ${errs.mkString(",")}"))

object InstrumentServiceLive:
  val layer = ZLayer.fromFunction(InstrumentServiceLive.apply _)
