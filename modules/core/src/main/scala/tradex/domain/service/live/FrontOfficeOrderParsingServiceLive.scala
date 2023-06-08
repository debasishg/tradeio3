package tradex.domain
package service
package live

import java.io.Reader
import java.time.{ Instant, LocalDateTime, ZoneOffset }
import zio.prelude.NonEmptyList
import zio.{ Clock, NonEmptyChunk, Task, UIO, ZIO, ZLayer }
import kantan.csv.rfc
import csv.CSV
import model.frontOfficeOrder.FrontOfficeOrder
import transport.frontOfficeOrderT.{ *, given }
import zio.stream.{ ZPipeline, ZStream }
import model.order.*
import model.account.*
import model.instrument.*
import repository.OrderRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

final case class FrontOfficeOrderParsingServiceLive(
    orderRepo: OrderRepository
) extends FrontOfficeOrderParsingService:

  def parse(data: Reader): Task[Unit] =
    parseAllRows(data)
      .via(convertToOrder)
      .runForeachChunk(orders =>
        ZIO.when(orders.nonEmpty)(orderRepo.store(NonEmptyList(orders.head, orders.tail.toList: _*)))
      )

  private def parseAllRows(data: Reader): ZStream[Any, Throwable, FrontOfficeOrder] =
    CSV.decode[FrontOfficeOrder](data, rfc.withHeader)

  private def convertToOrder: ZPipeline[Any, Nothing, FrontOfficeOrder, Order] =
    ZPipeline
      .groupAdjacentBy[FrontOfficeOrder, AccountNo](_.accountNo)
      .mapZIO:
        case (ano, fos) => makeOrder(ano, fos)

  private def makeOrderNo(accountNo: String, date: LocalDate): String =
    s"$accountNo-${DateTimeFormatter.ISO_LOCAL_DATE.format(date)}"

  private def makeOrder(ano: AccountNo, fos: NonEmptyChunk[FrontOfficeOrder]): UIO[Order] =
    Clock.instant
      .map(_.atOffset(ZoneOffset.UTC))
      .flatMap: now =>
        val odate = LocalDate.ofInstant(fos.head.date, ZoneOffset.UTC)
        val ono   = makeOrderNo(AccountNo.unwrap(ano), odate)
        val lineItems = fos.map: fo =>
          LineItem.make(
            OrderNo(ono),
            fo.isin,
            fo.qty,
            fo.unitPrice,
            fo.buySell
          )
        ZIO.succeed(
          Order.make(
            OrderNo(ono),
            LocalDateTime
              .ofInstant(fos.head.date, ZoneOffset.UTC),
            ano,
            NonEmptyList(lineItems.head, lineItems.tail.toList: _*)
          )
        )

object FrontOfficeOrderParsingServiceLive:
  val layer = ZLayer.fromFunction(FrontOfficeOrderParsingServiceLive.apply _)
