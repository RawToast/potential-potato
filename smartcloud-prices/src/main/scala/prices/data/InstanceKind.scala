package prices.data

import io.circe.{ Decoder, HCursor }

final case class InstanceKind(getString: String) extends AnyVal

object InstanceKind {
  implicit val instanceKindEntityDecoder: Decoder[InstanceKind] = new Decoder[InstanceKind] {
    final def apply(c: HCursor) = c.as[String].map(value => InstanceKind(value))
  }
}
