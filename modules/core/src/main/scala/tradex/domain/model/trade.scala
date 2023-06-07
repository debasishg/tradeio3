package tradex.domain
package model

import zio.prelude.*
import market.*
import account.*
import instrument.*
import order.*
import user.*
import squants.market.*
import com.softwaremill.quicklens.*
import zio.{ Random, Task, ZIO }
import java.time.LocalDateTime
import java.util.UUID

object trade:
  object TradeRefNo extends Newtype[UUID]:
    given Equal[TradeRefNo] = Equal.default

  type TradeRefNo = TradeRefNo.Type

  enum TaxFeeId(val entryName: NonEmptyString):
    case TradeTax   extends TaxFeeId(NonEmptyString("TradeTax"))
    case Commission extends TaxFeeId(NonEmptyString("Commission"))
    case VAT        extends TaxFeeId(NonEmptyString("VAT"))
    case Surcharge  extends TaxFeeId(NonEmptyString("Surcharge"))

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

  final case class Trade private[domain] (
      tradeRefNo: TradeRefNo,
      accountNo: AccountNo,
      isin: ISINCode,
      market: Market,
      buySell: BuySell,
      unitPrice: UnitPrice,
      quantity: Quantity,
      tradeDate: LocalDateTime,
      valueDate: Option[LocalDateTime] = None,
      userId: Option[UserId] = None,
      taxFees: List[TradeTaxFee] = List.empty,
      netAmount: Option[Money] = None
  )

  private[domain] final case class TradeTaxFee(
      taxFeeId: TaxFeeId,
      amount: Money
  )

  object Trade:

    def trade(
        accountNo: AccountNo,
        isin: ISINCode,
        market: Market,
        buySell: BuySell,
        unitPrice: UnitPrice,
        quantity: Quantity,
        tradeDate: LocalDateTime,
        valueDate: Option[LocalDateTime] = None,
        userId: Option[UserId] = None
    ): Task[Trade] = (for
      tdvd  <- ZIO.fromEither(validateTradeValueDate(tradeDate, valueDate).toEither)
      refNo <- Random.nextUUID.map(uuid => TradeRefNo.make(uuid).toEither).absolve
    yield Trade(
      refNo,
      accountNo,
      isin,
      market,
      buySell,
      unitPrice,
      quantity,
      tdvd._1,
      tdvd._2,
      userId
    )).mapError(errors => new Throwable(errors.mkString(",")))

    private def validateTradeValueDate(
        td: LocalDateTime,
        vd: Option[LocalDateTime]
    ): Validation[String, (LocalDateTime, Option[LocalDateTime])] =
      vd.map { v =>
        if (v.isBefore(td)) Validation.fail(s"Value date $v cannot be earlier than trade date $td")
        else Validation.succeed((td, vd))
      }.getOrElse(Validation.succeed((td, vd)))

    def withTaxFee(trade: Trade): Trade =
      if (trade.taxFees.isEmpty && !trade.netAmount.isDefined)
        val taxFees = forTrade(trade).map(taxFeeCalculate(trade, _)).getOrElse(List.empty)
        val netAmt  = netAmount(trade, taxFees)
        trade
          .modify(_.taxFees)
          .setTo(taxFees)
          .modify(_.netAmount.each)
          .setTo(netAmt)
      else trade
