package tradex.domain.csv

import zio.Scope
import zio.test.*
import zio.test.Assertion.*
import kantan.csv.{ CsvConfiguration, HeaderCodec, RowDecoder, rfc }
import zio.stream.ZStream
import kantan.csv.CsvConfiguration
import java.io.StringReader

object CSVSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CSV")(
      test("encode / decode") {
        val data = List(Account("ibm", 1), Account("nri", 2), Account("hitachi", 3))

        val encoded =
          ZStream
            .fromIterable(data)
            .via(CSV.encode[Account])
            .runCollect
            .map(bytes => new String(bytes.toArray))

        encoded.map(actual =>
          assertTrue(actual.split("\r\n") sameElements Array("Name,No", "ibm,1", "nri,2", "hitachi,3"))
        )

        encoded.flatMap(e =>
          CSV
            .decode[Account](new StringReader(e), rfc)
            .runCollect
            .map(decoded => assertTrue(decoded sameElements data))
        )
      }
    )
}

case class Account(name: String, no: Int)

object Account:
  given RowDecoder[Account] =
    RowDecoder.decoder(0, 1)(Account.apply)
  given HeaderCodec[Account] =
    HeaderCodec.codec[String, Int, Account]("Name", "No")(Account(_, _))(e => (e.name, e.no))
