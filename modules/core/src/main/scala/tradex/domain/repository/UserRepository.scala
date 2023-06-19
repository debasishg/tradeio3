package tradex.domain
package repository

import zio.UIO
import model.user.*

trait UserRepository {

  /** query by username */
  def query(username: UserName): UIO[Option[User]]

  /** store a user * */
  def store(username: UserName, password: EncryptedPassword): UIO[UserId]
}
