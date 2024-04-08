package prices.routes.protocol

import io.circe._
import io.circe.syntax._

import prices.data.InstanceKind

final case class InstanceKindResponse(values: List[InstanceKind]) extends AnyVal

object InstanceKindResponse {

  implicit val encoder: Encoder[InstanceKindResponse] =
    Encoder.instance[InstanceKindResponse] {
      case InstanceKindResponse(k) =>
        k.map(value => Json.obj("kind" -> value.getString.asJson)).asJson
    }
}
