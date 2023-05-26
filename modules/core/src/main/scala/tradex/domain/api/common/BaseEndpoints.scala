package tradex.domain
package api
package common

import zio.ZLayer
import sttp.tapir.{ EndpointOutput, PublicEndpoint }
import sttp.tapir.ztapir.*
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.generic.auto.*

case class BaseEndpoints():

  val publicEndpoint: PublicEndpoint[Unit, ErrorInfo, Unit, Any] = endpoint
    .errorOut(BaseEndpoints.defaultErrorOutputs)

object BaseEndpoints:
  val live: ZLayer[Any, Nothing, BaseEndpoints] =
    ZLayer.fromFunction(BaseEndpoints.apply _)

  val defaultErrorOutputs: EndpointOutput.OneOf[ErrorInfo, ErrorInfo] = oneOf[ErrorInfo](
    oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
    oneOfVariant(statusCode(StatusCode.Forbidden).and(jsonBody[Forbidden])),
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
    oneOfVariant(statusCode(StatusCode.Conflict).and(jsonBody[Conflict])),
    oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized])),
    oneOfVariant(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[ValidationFailed])),
    oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[InternalServerError]))
  )
