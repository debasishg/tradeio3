package tradex.domain
package model

import zio.prelude.*
import zio.prelude.Assertion.*
import cats.syntax.all.*
import java.time.LocalDateTime
import squants.market.Money
import java.time.LocalDateTime

object instrument:
  object ISINCode extends Newtype[String]:
    override inline def assertion: Assertion[String] = hasLength(equalTo(12)) &&
      matches("([A-Z]{2})((?![A-Z]{10}\b)[A-Z0-9]{10})")

  type ISINCode = ISINCode.Type

  case class InstrumentName(value: NonEmptyString)

  object LotSize extends Subtype[Int]:
    override inline def assertion: Assertion[Int] = greaterThan(0)

  type LotSize = LotSize.Type

  enum InstrumentType(val entryName: String):
    case CCY         extends InstrumentType("Ccy")
    case Equity      extends InstrumentType("Equity")
    case FixedIncome extends InstrumentType("Fixed Income")

  object UnitPrice extends Subtype[BigDecimal]:
    override inline def assertion = Assertion.greaterThan(BigDecimal(0))
  type UnitPrice = UnitPrice.Type

  enum CouponFrequency(val entryName: NonEmptyString):
    case Annual     extends CouponFrequency(NonEmptyString("annual"))
    case SemiAnnual extends CouponFrequency(NonEmptyString("semi-annual"))

  final case class InstrumentBase(
      isinCode: ISINCode,
      name: InstrumentName,
      lotSize: LotSize
  )

  sealed trait Instrument:
    private[instrument] def base: InstrumentBase
    def instrumentType: InstrumentType
    val isinCode = base.isinCode
    val name     = base.name
    val lotSize  = base.lotSize

  final case class Ccy(
      private[instrument] val base: InstrumentBase
  ) extends Instrument:
    val instrumentType = InstrumentType.CCY

  object Ccy:
    def ccy(isin: ISINCode, name: InstrumentName) =
      Ccy(
        base = InstrumentBase(isin, name, LotSize(1))
      )

  final case class Equity(
      private[instrument] val base: InstrumentBase,
      dateOfIssue: LocalDateTime,
      unitPrice: UnitPrice
  ) extends Instrument:
    val instrumentType = InstrumentType.Equity

  object Equity:
    def equity(isin: ISINCode, name: InstrumentName, lotSize: LotSize, issueDate: LocalDateTime, unitPrice: UnitPrice) =
      Equity(
        base = InstrumentBase(isin, name, lotSize),
        dateOfIssue = issueDate,
        unitPrice = unitPrice
      )

  final case class FixedIncome(
      private[instrument] val base: InstrumentBase,
      dateOfIssue: LocalDateTime,
      dateOfMaturity: Option[LocalDateTime],
      couponRate: Money,
      couponFrequency: CouponFrequency
  ) extends Instrument:
    val instrumentType = InstrumentType.FixedIncome

  object FixedIncome:
    def fixedIncome(
        isin: ISINCode,
        name: InstrumentName,
        lotSize: LotSize,
        issueDate: LocalDateTime,
        maturityDate: Option[LocalDateTime],
        couponRate: Money,
        couponFrequency: CouponFrequency
    ): Validation[String, FixedIncome] =
      validateIssueAndMaturityDate(issueDate, maturityDate).map: (id, md) =>
        FixedIncome(
          base = InstrumentBase(isin, name, lotSize),
          dateOfIssue = id,
          dateOfMaturity = md,
          couponRate = couponRate,
          couponFrequency = couponFrequency
        )

    private def validateIssueAndMaturityDate(
        id: LocalDateTime,
        md: Option[LocalDateTime]
    ): Validation[String, (LocalDateTime, Option[LocalDateTime])] =
      md.map: c =>
        if (c isBefore id)
          Validation.fail(s"Maturity date [$c] cannot be earlier than issue date [$id]")
        else Validation.succeed((id, md))
      .getOrElse(Validation.succeed((id, md)))
