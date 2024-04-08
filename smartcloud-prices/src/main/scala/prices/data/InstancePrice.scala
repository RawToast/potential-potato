package prices.data

import io.circe.Decoder
import io.circe.HCursor

final case class InstancePrice(value: Double) extends AnyVal

object InstancePrice {
  implicit val instanceKindEntityDecoder: Decoder[InstancePrice] = new Decoder[InstancePrice] {
    final def apply(c: HCursor) = c.value.as[Double].map(p => InstancePrice(p))
  }
}
