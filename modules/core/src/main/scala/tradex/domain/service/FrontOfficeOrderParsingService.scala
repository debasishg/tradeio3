package tradex.domain
package service

import zio.Task
import java.io.Reader

trait FrontOfficeOrderParsingService {
  def parse(data: Reader): Task[Unit]
}
