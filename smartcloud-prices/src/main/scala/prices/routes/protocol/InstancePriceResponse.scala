package prices.routes.protocol

import io.circe.{ Encoder, Json }

import prices.data.{ InstanceKind, InstancePrice }

case class InstancePriceResponse(
    kind: InstanceKind,
    amount: InstancePrice
)
object InstancePriceResponse {
  implicit val instanceKindPricingDecoder: Encoder[InstancePriceResponse] = new Encoder[InstancePriceResponse] {
    final def apply(i: InstancePriceResponse) = Json.obj(
      ("kind", Json.fromString(i.kind.getString)),
      ("amount", Json.fromDoubleOrNull(i.amount.value))
    )
  }
}
