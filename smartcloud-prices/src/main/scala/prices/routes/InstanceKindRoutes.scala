package prices.routes

import cats.effect._
import cats.implicits._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ EntityEncoder, HttpRoutes }

import prices.routes.protocol._
import prices.services.InstanceKindService

final case class InstanceKindRoutes[F[_]: Sync](instanceKindService: InstanceKindService[F]) extends Http4sDsl[F] {

  val prefix = "/instance-kinds"

  implicit val instanceKindResponseEncoder: EntityEncoder[F, List[InstanceKindResponse]] = jsonEncoderOf

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root =>
      instanceKindService.getAll().flatMap(kinds => Ok(kinds.map(k => InstanceKindResponse(k))))
  }

  def routes: HttpRoutes[F] =
    Router(
      prefix -> get
    )

}
