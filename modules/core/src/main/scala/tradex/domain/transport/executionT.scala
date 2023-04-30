package tradex.domain
package transport

import zio.json.*
import cats.syntax.all._
import model.execution.*
import accountT.{ given, * }
import orderT.{ given, * }
import instrumentT.{ given, * }
import java.util.UUID

object executionT {
  given JsonDecoder[ExecutionRefNo] =
    JsonDecoder[UUID].mapOrFail(ExecutionRefNo.make(_).toEither.leftMap(_.head))
  given JsonEncoder[ExecutionRefNo] = JsonEncoder[UUID].contramap(ExecutionRefNo.unwrap(_))

  given JsonDecoder[Execution] = DeriveJsonDecoder.gen[Execution]
  given JsonEncoder[Execution] = DeriveJsonEncoder.gen[Execution]
}
