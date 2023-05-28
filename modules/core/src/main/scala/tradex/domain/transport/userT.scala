package tradex.domain
package transport

import model.user.*
import zio.json.*
import java.util.UUID
import cats.syntax.all.*
import sttp.tapir.Schema

object userT:
  given JsonDecoder[UserId] =
    JsonDecoder[UUID].mapOrFail(UserId.make(_).toEither.leftMap(_.head))
  given JsonEncoder[UserId] = JsonEncoder[UUID].contramap(UserId.unwrap(_))

  given JsonDecoder[UserName] =
    JsonDecoder[NonEmptyString].mapOrFail(UserName.make(_).toEither.leftMap(_.head))
  given JsonEncoder[UserName] = JsonEncoder[String].contramap(UserName.unwrap(_))

  given JsonDecoder[EncryptedPassword] =
    JsonDecoder[NonEmptyString].mapOrFail(EncryptedPassword.make(_).toEither.leftMap(_.head))
  given JsonEncoder[EncryptedPassword] = JsonEncoder[String].contramap(EncryptedPassword.unwrap(_))

  given JsonCodec[User] = DeriveJsonCodec.gen[User]

  given Schema[UserId]            = Schema.string
  given Schema[UserName]          = Schema.string
  given Schema[EncryptedPassword] = Schema.string
  given Schema[User]              = Schema.derived[User]
