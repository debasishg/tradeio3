package tradex.domain
package model

import zio.prelude.*
import zio.prelude.Assertion.*
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*
import cats.syntax.all.*
import java.time.Instant
import squants.market.Money

object instrument {
  object ISINCode extends Newtype[String]:
    override inline def assertion: Assertion[String] = hasLength(equalTo(12)) &&
      matches("([A-Z]{2})((?![A-Z]{10}\b)[A-Z0-9]{10})")
    given Decoder[ISINCode] = Decoder[String].emap(ISINCode.make(_).toEither.leftMap(_.head))
    given Encoder[ISINCode] = Encoder[String].contramap(ISINCode.unwrap(_))

  type ISINCode = ISINCode.Type

  case class InstrumentName(value: NonEmptyString)
  given Decoder[InstrumentName] = deriveDecoder[InstrumentName]
  given Encoder[InstrumentName] = deriveEncoder[InstrumentName]

  object LotSize extends Subtype[Int]:
    override inline def assertion: Assertion[Int] = greaterThan(0)
    given Decoder[LotSize]                        = Decoder[Int].emap(LotSize.make(_).toEither.leftMap(_.head))
    given Encoder[LotSize]                        = Encoder[Int].contramap(LotSize.unwrap(_))

  type LotSize = LotSize.Type

  enum InstrumentType(val entryName: NonEmptyString):
    case CCY extends InstrumentType(NonEmptyString("ccy"))
    case Equity extends InstrumentType(NonEmptyString("equity"))
    case FixedIncome extends InstrumentType(NonEmptyString("fixed income"))

  object InstrumentType:
    given Encoder[InstrumentType] =
      Encoder[String].contramap(_.entryName)

    given Decoder[InstrumentType] =
      Decoder[String].map(InstrumentType.valueOf(_))

  object UnitPrice extends Subtype[BigDecimal] {
    override inline def assertion = Assertion.greaterThan(BigDecimal(0))
    given Decoder[UnitPrice]      = Decoder[BigDecimal].emap(UnitPrice.make(_).toEither.leftMap(_.head))
    given Encoder[UnitPrice]      = Encoder[BigDecimal].contramap(UnitPrice.unwrap(_))
  }
  type UnitPrice = UnitPrice.Type

  enum CouponFrequency(val entryName: NonEmptyString):
    case Annual extends CouponFrequency(NonEmptyString("annual"))
    case SemiAnnual extends CouponFrequency(NonEmptyString("semi-annual"))

  object CouponFrequency:
    given Encoder[CouponFrequency] =
      Encoder[String].contramap(_.entryName)

    given Decoder[CouponFrequency] =
      Decoder[String].map(CouponFrequency.valueOf(_))

  final case class InstrumentBase(
      isinCode: ISINCode,
      name: InstrumentName,
      lotSize: LotSize
  )

  trait Instrument:
    private[instrument] def base: InstrumentBase
    def instrumentType: InstrumentType
    val isinCode = base.isinCode
    val name     = base.name
    val lotSize  = base.lotSize

  final case class Ccy(
      private[instrument] base: InstrumentBase
  ) extends Instrument:
    val instrumentType = InstrumentType.CCY

  object Ccy:
    def ccy(isin: ISINCode, name: InstrumentName) =
      Ccy(
        base = InstrumentBase(isin, name, LotSize(1))
      )

  final case class Equity(
      private[instrument] base: InstrumentBase,
      dateOfIssue: Instant,
      unitPrice: UnitPrice
  ) extends Instrument:
    val instrumentType = InstrumentType.Equity

  object Equity:
    def equity(isin: ISINCode, name: InstrumentName, lotSize: LotSize, issueDate: Instant, unitPrice: UnitPrice) =
      Equity(
        base = InstrumentBase(isin, name, lotSize),
        dateOfIssue = issueDate,
        unitPrice = unitPrice
      )

  final case class FixedIncome(
      private[instrument] base: InstrumentBase,
      dateOfIssue: Instant,
      dateOfMaturity: Option[Instant],
      couponRate: Money,
      couponFrequency: CouponFrequency
  ) extends Instrument:
    val instrumentType = InstrumentType.FixedIncome

  object FixedIncome:
    def fixedIncome(
        isin: ISINCode,
        name: InstrumentName,
        lotSize: LotSize,
        issueDate: Instant,
        maturityDate: Option[Instant],
        couponRate: Money,
        couponFrequency: CouponFrequency
    ): Validation[String, FixedIncome] =
      validateIssueAndMaturityDate(issueDate, maturityDate).map { (id, md) =>
        FixedIncome(
          base = InstrumentBase(isin, name, lotSize),
          dateOfIssue = id,
          dateOfMaturity = md,
          couponRate = couponRate,
          couponFrequency = couponFrequency
        )
      }

    private def validateIssueAndMaturityDate(
        id: Instant,
        md: Option[Instant]
    ): Validation[String, (Instant, Option[Instant])] =
      md.map { c =>
        if (c isBefore id)
          Validation.fail(s"Maturity date [$c] cannot be earlier than issue date [$id]")
        else Validation.succeed((id, md))
      }.getOrElse(Validation.succeed((id, md)))

}
