package tradex.domain
package service
package live

import zio.Task
import repository.InstrumentRepository
import model.instrument.*
import zio.ZLayer

final case class InstrumentServiceLive(
    repository: InstrumentRepository
) extends InstrumentService:
  override def query(isin: ISINCode): Task[Option[Instrument]] =
    repository.query(isin)

object InstrumentServiceLive:
  val layer = ZLayer.fromFunction(InstrumentServiceLive.apply _)
