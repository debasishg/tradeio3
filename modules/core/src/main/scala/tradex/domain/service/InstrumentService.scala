package tradex.domain
package service

import zio.{ Task, UIO }
import model.instrument.*
import java.time.LocalDateTime
import squants.market.Money

trait InstrumentService:
  def query(isin: ISINCode): Task[Option[Instrument]]

  def addEquity(
      isin: ISINCode,
      name: InstrumentName,
      lotSize: LotSize,
      issueDate: LocalDateTime,
      unitPrice: UnitPrice
  ): UIO[Instrument]

  def addFixedIncome(
      isin: ISINCode,
      name: InstrumentName,
      lotSize: LotSize,
      issueDate: LocalDateTime,
      maturityDate: Option[LocalDateTime],
      couponRate: Money,
      couponFrequency: CouponFrequency
  ): Task[Instrument]
