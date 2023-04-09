package tradex.domain
package model

import zio.prelude.*
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*
import market.*
import account.*
import instrument.*
import order.*
import user.*
import squants.market.*
import java.time.Instant

object trade:
  object TradeRefNo extends Newtype[String]:
    given Decoder[TradeRefNo] = Decoder[String].emap(TradeRefNo.make(_).toEither.leftMap(_.head))
    given Encoder[TradeRefNo] = Encoder[String].contramap(TradeRefNo.unwrap(_))
    implicit val TradeRefNoEqual: Equal[TradeRefNo] =
      Equal.default

  type TradeRefNo = TradeRefNo.Type

  extension (rno: TradeRefNo)
    def validateNo: Validation[String, TradeRefNo] =
      if (TradeRefNo.unwrap(rno).size > 12 || TradeRefNo.unwrap(rno).size < 5)
        Validation.fail(s"TradeRefNo cannot be more than 12 characters or less than 5 characters long")
      else Validation.succeed(rno)

  enum TaxFeeId(val entryName: NonEmptyString):
    case TradeTax extends TaxFeeId(NonEmptyString("Trade Tax"))
    case Commission extends TaxFeeId(NonEmptyString("Commission"))
    case VAT extends TaxFeeId(NonEmptyString("VAT"))
    case Surcharge extends TaxFeeId(NonEmptyString("Surcharge"))

  object TaxFeeId:
    implicit val taxFeeIdEncoder: Encoder[TaxFeeId] =
      Encoder[String].contramap(_.entryName)

    implicit val taxFeeIdDecoder: Decoder[TaxFeeId] =
      Decoder[String].map(TaxFeeId.valueOf(_))

  import TaxFeeId.*

  // rates of tax/fees expressed as fractions of the principal of the trade
  final val rates: Map[TaxFeeId, BigDecimal] =
    Map(TradeTax -> 0.2, Commission -> 0.15, VAT -> 0.1)

  // tax and fees applicable for each market
  // Other signifies the general rule applicable for all markets
  final val taxFeeForMarket: Map[Market, List[TaxFeeId]] =
    Map(
      Market.Other     -> List(TradeTax, Commission),
      Market.Singapore -> List(TradeTax, Commission, VAT)
    )

  // get the list of tax/fees applicable for this trade
  // depending on the market
  final val forTrade: Trade => Option[List[TaxFeeId]] = { trade =>
    taxFeeForMarket.get(trade.market).orElse(taxFeeForMarket.get(Market.Other))
  }

  final def principal(trade: Trade): Money =
    Money(UnitPrice.unwrap(trade.unitPrice) * Quantity.unwrap(trade.quantity))

  // combinator to value a tax/fee for a specific trade
  private def valueAs(trade: Trade, taxFeeId: TaxFeeId): Money =
    ((rates get taxFeeId) map (principal(trade) * _)) getOrElse (Money(0))

  // all tax/fees for a specific trade
  private def taxFeeCalculate(
      trade: Trade,
      taxFeeIds: List[TaxFeeId]
  ): List[TradeTaxFee] =
    taxFeeIds
      .zip(taxFeeIds.map(valueAs(trade, _)))
      .map { case (tid, amt) => TradeTaxFee(tid, amt) }

  private def netAmount(
      trade: Trade,
      taxFeeAmounts: List[TradeTaxFee]
  ): Money =
    principal(trade) + taxFeeAmounts.map(_.amount).foldLeft(Money(0))(_ + _)

  final case class Trade private (
      accountNo: AccountNo,
      isin: ISINCode,
      market: Market,
      buySell: BuySell,
      unitPrice: UnitPrice,
      quantity: Quantity,
      tradeDate: Instant,
      valueDate: Option[Instant] = None,
      userId: Option[UserId] = None,
      taxFees: List[TradeTaxFee] = List.empty,
      netAmount: Option[Money] = None,
      tradeRefNo: Option[TradeRefNo] = None
  )

  private[domain] final case class TradeTaxFee(
      taxFeeId: TaxFeeId,
      amount: Money
  )

  object TradeTaxFee:
    given Decoder[TradeTaxFee] = deriveDecoder[TradeTaxFee]
    given Encoder[TradeTaxFee] = deriveEncoder[TradeTaxFee]

  object Trade:
    given Decoder[Trade] = deriveDecoder[Trade]
    given Encoder[Trade] = deriveEncoder[Trade]

    def trade(
        accountNo: AccountNo,
        isin: ISINCode,
        market: Market,
        buySell: BuySell,
        unitPrice: UnitPrice,
        quantity: Quantity,
        tradeDate: Instant,
        valueDate: Option[Instant] = None,
        userId: Option[UserId] = None
    ): Validation[String, Trade] = {
      validateTradeValueDate(tradeDate, valueDate).map { case (td, maybeVd) =>
        Trade(
          accountNo,
          isin,
          market,
          buySell,
          unitPrice,
          quantity,
          td,
          maybeVd,
          userId
        )
      }
    }

    private def validateTradeValueDate(
        td: Instant,
        vd: Option[Instant]
    ): Validation[String, (Instant, Option[Instant])] =
      vd.map { v =>
        if (v.isBefore(td)) Validation.fail(s"Value date $v cannot be earlier than trade date $td")
        else Validation.succeed((td, vd))
      }.getOrElse(Validation.succeed((td, vd)))

    def withTaxFee(trade: Trade): Trade =
      if (trade.taxFees.isEmpty && !trade.netAmount.isDefined) {
        val taxFees =
          forTrade(trade).map(taxFeeCalculate(trade, _)).getOrElse(List.empty)
        val netAmt = netAmount(trade, taxFees)
        trade.copy(taxFees = taxFees, netAmount = Option(netAmt))
      } else trade