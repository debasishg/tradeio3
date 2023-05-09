package tradex.domain
package service

import zio.Task
import java.io.Reader

trait ExchangeExecutionParsingService:
  def parse(data: Reader): Task[Unit]
