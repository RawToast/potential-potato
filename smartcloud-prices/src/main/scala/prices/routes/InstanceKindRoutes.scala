package prices.routes

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import prices.routes.protocol._
import prices.services.InstanceKindService
import prices.services.InstanceKindService.Exception

final case class InstanceKindRoutes[F[_]: Sync](instanceKindService: InstanceKindService[F]) extends Http4sDsl[F] {
  val prefix             = "/instance-kinds"
  private val authHeader = headers.`WWW-Authenticate`(Challenge(scheme = "Bearer", realm = "smartcloud"))

  implicit val instanceKindResponseEncoder: EntityEncoder[F, InstanceKindResponse] = jsonEncoderOf[F, InstanceKindResponse]

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root =>
      for {
        result <- instanceKindService.getAll()
        response <- result match {
                      case Right(data)                       => Ok(data)
                      case Left(Exception.Unauthorized)      => Unauthorized(authHeader)
                      case Left(Exception.APICallFailure(_)) => InternalServerError()
                    }
      } yield response
  }

  def routes: HttpRoutes[F] =
    Router(
      prefix -> get
    )

}
