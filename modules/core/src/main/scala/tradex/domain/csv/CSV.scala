package tradex.domain
package csv

import java.io.Reader
import zio.ZIO
import zio.stream.ZStream
import kantan.csv.{ CsvConfiguration, ReadError, RowDecoder }
import kantan.csv.engine.ReaderEngine
import kantan.csv.ops.*

object CSV:
  def decode[A: RowDecoder](reader: Reader, conf: CsvConfiguration)(using
      ReaderEngine
  ): ZStream[Any, Throwable, A] =
    ZStream.fromIterator(
      reader
        .asCsvReader[A](conf)
        .collect { case Right(value) => value }
        .iterator
    )

  enum ParsedResult[+A]:
    case Failed(index: Long, error: ReadError) extends ParsedResult[Nothing]
    case Succeed[A](index: Long, value: A) extends ParsedResult[A]

    def toEither: Either[ReadError, A] = this match
      case Failed(_, error)  => Left(error)
      case Succeed(_, value) => Right(value)
