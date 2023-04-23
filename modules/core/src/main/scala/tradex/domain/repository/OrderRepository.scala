package tradex.domain
package repository

import zio.Task
import model.order.*
import java.time.LocalDate
import zio.prelude.NonEmptyList
import zio.stream.ZStream

trait OrderRepository:

  /** query by unique key order no, account number and date */
  def query(no: OrderNo): Task[Option[Order]]

  /** query by order date */
  def queryByOrderDate(date: LocalDate): Task[List[Order]]

  /** store */
  def store(ord: Order): Task[Order]

  /** store many orders */
  def store(orders: NonEmptyList[Order]): Task[Unit]

  /** stream all orders for the day */
  def streamOrders(
      executionDate: LocalDate
  ): ZStream[Any, Throwable, Order] = ???
