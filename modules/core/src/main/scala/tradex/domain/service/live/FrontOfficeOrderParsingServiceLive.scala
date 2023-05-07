package tradex.domain
package service
package live

import zio.Task
import java.io.Reader
import csv.CSV
import model.frontOfficeOrder.FrontOfficeOrder
import transport.frontOfficeOrderT.{ given, * }
import kantan.csv.rfc
import zio.stream.ZStream
import zio.stream.ZPipeline
import tradex.domain.model.order.Order
import tradex.domain.model.account.AccountNo

final case class FrontOfficeOrderParsingServiceLive() extends FrontOfficeOrderParsingService:
  def parse(data: Reader): Task[Unit] = ???

  private def parseAllRows(data: Reader): ZStream[Any, Throwable, FrontOfficeOrder] =
    CSV.decode[FrontOfficeOrder](data, rfc.withHeader)

/*
  private def convertToOrder: ZPipeline[Any, Nothing, FrontOfficeOrder, Order] =
    ZPipeline
      .groupAdjacentBy[FrontOfficeOrder, AccountNo](_.accountNo)
      .map { case (ano, fos) =>
      }
 */
