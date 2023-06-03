package tradex.domain
package api
package common

import zio.{ IO, Task, ZIO }

object ErrorMapper:

  def defaultErrorsMappings[A](io: Task[A]): ZIO[Any, ErrorInfo, A] = io.mapError {
    case e: Exceptions.AlreadyInUse => Conflict(e.message)
    case e: Exceptions.NotFound     => NotFound(e.message)
    case e: Exceptions.BadRequest   => BadRequest(e.message)
    case e: Exceptions.Unauthorized => Unauthorized(e.message)
    case e                          => InternalServerError(e.getMessage)
  }
