package tradex.domain
package model

import zio.prelude.*
import scala.util.control.NoStackTrace
import java.util.UUID

object user:

  object UserId extends Newtype[UUID]:
    given Equal[UserId] = Equal.default

  type UserId = UserId.Type

  object UserName extends Newtype[NonEmptyString]
  type UserName = UserName.Type

  object Password extends Newtype[NonEmptyString]
  type Password = Password.Type

  object EncryptedPassword extends Newtype[NonEmptyString]
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
