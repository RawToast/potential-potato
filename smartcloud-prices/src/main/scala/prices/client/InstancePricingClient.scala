package prices.client

import java.time.ZonedDateTime

import io.circe._

import prices.data._

case class InstancePricing(
    kind: InstanceKind,
    price: InstancePrice,
    timestamp: ZonedDateTime
)
object InstancePricing {
  implicit val instanceKindPricingDecoder: Decoder[InstancePricing] = new Decoder[InstancePricing] {
    final def apply(c: HCursor) = for {
      kind <- c.downField("kind").as[InstanceKind]
      price <- c.downField("price").as[InstancePrice]
      timestamp <- c.downField("timestamp").as[ZonedDateTime]
    } yield InstancePricing(kind, price, timestamp)
  }
}

trait InstancePricingClient[F[_]] {
  import InstancePricingClient._
  def getInstancePricing(instanceKind: InstanceKind): F[Either[Error, Option[InstancePricing]]]
}

object InstancePricingClient {

  sealed trait Error
  object Error {
    case object ServerError extends Error
    case object RateLimit extends Error
    case object Unauthorized extends Error
  }

}
