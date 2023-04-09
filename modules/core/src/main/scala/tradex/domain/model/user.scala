package tradex.domain
package model

import zio.prelude.*
import scala.util.control.NoStackTrace
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*

object user {

  object UserId extends Newtype[String]:
    given Decoder[UserId] = Decoder[String].emap(UserId.make(_).toEither.leftMap(_.head))
    given Encoder[UserId] = Encoder[String].contramap(UserId.unwrap(_))
    implicit val AccountNoEqual: Equal[UserId] =
      Equal.default

  type UserId = UserId.Type
  extension (uid: UserId)
    def validateNo: Validation[String, UserId] =
      if (UserId.unwrap(uid).size > 12 || UserId.unwrap(uid).size < 5)
        Validation.fail(s"UserId cannot be more than 12 characters or less than 5 characters long")
      else Validation.succeed(uid)

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

  private[domain] final case class User private (
      userId: UserId,
      userName: UserName,
      password: EncryptedPassword
  )

  object User:
    given Decoder[User] = deriveDecoder[User]
    given Encoder[User] = deriveEncoder[User]

}
