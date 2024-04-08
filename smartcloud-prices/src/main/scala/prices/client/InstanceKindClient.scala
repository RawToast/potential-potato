package prices.client

import prices.data._

trait InstanceKindClient[F[_]] {
  import InstanceKindClient._
  def getAll(): F[Either[Error, List[InstanceKind]]]
}

object InstanceKindClient {
  sealed trait Error
  object Error {
    case object Unauthorized extends Error
    case object ServerError extends Error
  }
}
