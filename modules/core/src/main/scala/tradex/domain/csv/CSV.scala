package tradex.domain
package csv

import java.io.Reader
import zio.{ Chunk, ZIO }
import zio.stream.{ ZPipeline, ZStream }
import kantan.csv.{ CsvConfiguration, HeaderEncoder, ReadError, RowDecoder, rfc }
import kantan.csv.engine.ReaderEngine
import kantan.csv.ops.*
import java.nio.charset.CharacterCodingException
import kantan.csv.HeaderEncoder

object CSV:
  def encode[A: HeaderEncoder]: ZPipeline[Any, CharacterCodingException, A, Byte] =
    ZPipeline.suspend {
      ZPipeline.mapChunks((in: Chunk[A]) => Chunk.single(in.asCsv(rfc.withHeader).trim)) >>>
        ZPipeline.intersperse("\r\n") >>>
        ZPipeline.utf8Encode
    }

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
