package tradex.domain
package repository
package live

import java.time.LocalDate

import model.account.*
import model.order.*
import zio.prelude.NonEmptyList
import zio.{ Task, ZIO, ZLayer }
import cats.effect.kernel.Resource
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import codecs.{ given, * }
import zio.prelude.Associative
import zio.interop.catz.*

final case class OrderRepositoryLive(postgres: Resource[Task, Session[Task]]) extends OrderRepository:
  import OrderRepositorySQL._

  implicit val orderConcatAssociative: Associative[Order] =
    new Associative[Order]:
      def combine(x: => Order, y: => Order): Order =
        Order.make(
          no = x.no,
          orderDate = x.date,
          accountNo = x.accountNo,
          items = x.items ++ y.items
        )

  override def store(orders: NonEmptyList[Order]): Task[Unit] =
    postgres.use(session =>
      session.transaction.use(_ => ZIO.foreach(orders.toList)(storeOrderAndLineItems(_, session)).map(_ => ()))
    )

  override def store(ord: Order): Task[Order] =
    postgres.use(session => session.transaction.use(_ => storeOrderAndLineItems(ord, session)))

  private def storeOrderAndLineItems(
      ord: Order,
      session: Session[Task]
  ): Task[Order] =
    val lineItems = ord.items.toList
    session
      .prepare(deleteLineItems)
      .flatMap(_.execute(OrderNo.unwrap(ord.no))) *>
      session
        .prepare(upsertOrder)
        .flatMap(
          _.execute(
            OrderNo.unwrap(ord.no) ~ ord.date ~ AccountNo.unwrap(ord.accountNo)
          )
        ) *>
      session
        .prepare(insertLineItems(ord.no, lineItems))
        .flatMap { cmd =>
          cmd.execute(lineItems)
        }
        .unit
        .map(_ => ord)

  override def query(no: OrderNo): Task[Option[Order]] =
    postgres.use { session =>
      session.prepare(selectByOrderNo).flatMap { ps =>
        ps.stream(no, 1024)
          .compile
          .toList
          .map(_.groupBy(_.no))
          .map {
            _.map { case (_, lis) =>
              lis.reduce(Associative[Order].combine(_, _))
            }.headOption
          }
      }
    }

  override def queryByOrderDate(date: LocalDate): Task[List[Order]] =
    postgres.use { session =>
      session
        .prepare(selectByOrderDate)
        .flatMap(ps =>
          ps.stream(date, 1024)
            .compile
            .toList
            .map(_.groupBy(_.no))
            .map { m =>
              m.map { case (_, lis) =>
                lis.reduce(Associative[Order].combine(_, _))
              }.toList
            }
        )
    }

  override def cleanAllOrders: Task[Unit] =
    postgres.use(session => session.execute(deleteAllLineItems).unit *> session.execute(deleteAllOrders).unit)

private object OrderRepositorySQL:

  val orderLineItemDecoder: Decoder[Order] =
    (timestamp ~ accountNo ~ isinCode ~ quantity ~ unitPrice ~ buySell ~ orderNo)
      .map { case od ~ ano ~ isin ~ qty ~ up ~ bs ~ ono =>
        Order.make(ono, od, ano, NonEmptyList(LineItem.make(ono, isin, qty, up, bs)))
      }

  val orderEncoder: Encoder[Order] =
    (orderNo ~ accountNo ~ timestamp).values
      .contramap((o: Order) => o.no ~ o.accountNo ~ o.date)

  def lineItemEncoder(ordNo: OrderNo) =
    (orderNo ~ isinCode ~ quantity ~ unitPrice ~ buySell).values
      .contramap((li: LineItem) => ordNo ~ li.isin ~ li.quantity ~ li.unitPrice ~ li.buySell)

  val selectByOrderNo: Query[OrderNo, Order] =
    sql"""
        SELECT o.dateOfOrder, o.accountNo, l.isinCode, l.quantity, l.unitPrice, l.buySellFlag, o.no
        FROM orders o, lineItems l
        WHERE o.no = $orderNo
        AND   o.no = l.orderNo
       """.query(orderLineItemDecoder)

  val selectByOrderDate: Query[LocalDate, Order] =
    sql"""
        SELECT o.dateOfOrder, o.accountNo, l.isinCode, l.quantity, l.unitPrice, l.buySellFlag, o.no
        FROM orders o, lineItems l
        WHERE Date(o.dateOfOrder) = $date
        AND   o.no = l.orderNo
       """.query(orderLineItemDecoder)

  val insertOrder: Command[Order] =
    sql"INSERT INTO orders (no, dateOfOrder, accountNo) VALUES $orderEncoder".command

  def insertLineItem(orderNo: OrderNo): Command[LineItem] =
    sql"INSERT INTO lineItems (orderNo, isinCode, quantity, unitPrice, buySellFlag) VALUES ${lineItemEncoder(orderNo)}".command

  def insertLineItems(orderNo: OrderNo, n: Int): Command[List[LineItem]] = {
    val es = lineItemEncoder(orderNo).list(n)
    sql"INSERT INTO lineItems (orderNo, isinCode, quantity, unitPrice, buySellFlag) VALUES $es".command
  }

  def insertLineItems(
      orderNo: OrderNo,
      lineItems: List[LineItem]
  ): Command[lineItems.type] =
    val es = lineItemEncoder(orderNo).list(lineItems)
    sql"INSERT INTO lineItems (orderNo, isinCode, quantity, unitPrice, buySellFlag) VALUES $es".command

  val upsertOrder =
    sql"""
        INSERT INTO orders
        VALUES ($varchar, $timestamp, $varchar)
        ON CONFLICT(no) DO UPDATE SET
          dateOfOrder = EXCLUDED.dateOfOrder,
          accountNo   = EXCLUDED.accountNo
       """.command

  val deleteLineItems: Command[String] =
    sql"DELETE FROM lineItems WHERE orderNo = $varchar".command

  val deleteAllLineItems: Command[Void] =
    sql"DELETE FROM lineItems".command

  val deleteAllOrders: Command[Void] =
    sql"DELETE FROM orders".command

object OrderRepositoryLive:
  val layer = ZLayer.fromFunction(OrderRepositoryLive.apply _)
