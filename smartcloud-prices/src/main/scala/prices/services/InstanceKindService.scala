package prices.services

import scala.util.control.NoStackTrace

import prices.routes.protocol.InstanceKindResponse

trait InstanceKindService[F[_]] {
  def getAll(): F[Either[InstanceKindService.Exception, InstanceKindResponse]]
}

object InstanceKindService {

  sealed trait Exception extends NoStackTrace
  object Exception {
    case class APICallFailure(message: String) extends Exception
    case object Unauthorized extends Exception
  }

}
