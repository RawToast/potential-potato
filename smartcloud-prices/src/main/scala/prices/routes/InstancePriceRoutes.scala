package prices.routes

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import prices.data.InstanceKind
import prices.routes.protocol._
import prices.services.InstancePriceService
import prices.services.InstancePriceService.Exception

final case class InstancePriceRoutes[F[_]: Sync](instancePriceService: InstancePriceService[F]) extends Http4sDsl[F] {
  object KindQueryParamMatcher extends QueryParamDecoderMatcher[String]("kind")

  implicit val instancePriceResponseEncoder: EntityEncoder[F, InstancePriceResponse] = jsonEncoderOf[F, InstancePriceResponse]
  private val authHeader = headers.`WWW-Authenticate`(Challenge(scheme = "Bearer", realm = "smartcloud"))
  private val infix      = "prices"

  private val getPricing: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root :? KindQueryParamMatcher(kind) =>
      for {
        result <- instancePriceService.getInstancePricing(InstanceKind(kind))
        response <- result match {
                      case Right(Some(data))                 => Ok(data)
                      case Right(None)                       => NotFound()
                      case Left(Exception.UnauthorizedCall)  => Unauthorized(authHeader)
                      case Left(Exception.APICallFailure(_)) => InternalServerError()
                      case Left(Exception.RateLimitExceeded) => TooManyRequests()
                    }
      } yield response
  }

  def routes: HttpRoutes[F] =
    Router(
      infix -> getPricing
    )

}
