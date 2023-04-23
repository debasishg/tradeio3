package tradex.domain
package model

import zio.prelude.*
import scala.util.control.NoStackTrace
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*
import java.util.UUID

object user {

  object UserId extends Newtype[UUID]:
    given Decoder[UserId] = Decoder[UUID].emap(UserId.make(_).toEither.leftMap(_.head))
    given Encoder[UserId] = Encoder[UUID].contramap(UserId.unwrap(_))
    implicit val UserIdEqual: Equal[UserId] =
      Equal.default

  type UserId = UserId.Type

  object UserName extends Newtype[NonEmptyString]:
    given Decoder[UserName] = Decoder[NonEmptyString].emap(UserName.make(_).toEither.leftMap(_.head))
    given Encoder[UserName] = Encoder[NonEmptyString].contramap(UserName.unwrap(_))
  type UserName = UserName.Type

  object Password extends Newtype[NonEmptyString]:
    given Decoder[Password] = Decoder[NonEmptyString].emap(Password.make(_).toEither.leftMap(_.head))
    given Encoder[Password] = Encoder[NonEmptyString].contramap(Password.unwrap(_))
  type Password = Password.Type

  object EncryptedPassword extends Newtype[NonEmptyString]:
    given Decoder[EncryptedPassword] = Decoder[NonEmptyString].emap(EncryptedPassword.make(_).toEither.leftMap(_.head))
    given Encoder[EncryptedPassword] = Encoder[NonEmptyString].contramap(EncryptedPassword.unwrap(_))
  type EncryptedPassword = EncryptedPassword.Type

  case class UserNotFound(username: UserName)    extends NoStackTrace
  case class UserNameInUse(username: UserName)   extends NoStackTrace
  case class InvalidPassword(username: UserName) extends NoStackTrace
  case object UnsupportedOperation               extends NoStackTrace
  case object TokenNotFound                      extends NoStackTrace

  private[domain] final case class User private[domain] (
      userId: UserId,
      userName: UserName,
      password: EncryptedPassword
  )

  object User:
    given Decoder[User] = deriveDecoder[User]
    given Encoder[User] = deriveEncoder[User]

}
