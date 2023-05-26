package tradex.domain
package service

import zio.Task
import model.instrument.*

trait InstrumentService:
  def query(isin: ISINCode): Task[Option[Instrument]]
