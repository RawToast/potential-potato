package prices.services

import scala.util.control.NoStackTrace

import prices.data._
import prices.routes.protocol.InstancePriceResponse

trait InstancePriceService[F[_]] {
  def getInstancePricing(instanceKind: InstanceKind): F[Either[InstancePriceService.Exception, Option[InstancePriceResponse]]]
}

object InstancePriceService {

  sealed trait Exception extends NoStackTrace
  object Exception {
    case class APICallFailure(message: String) extends Exception
    case object RateLimitExceeded extends Exception
    case object UnauthorizedCall extends Exception
  }

}
