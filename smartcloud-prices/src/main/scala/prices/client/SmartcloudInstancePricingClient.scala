package prices.client

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.{ Client, UnexpectedStatus }
import org.http4s.headers.Authorization

import prices.data._

object SmartcloudInstancePricingClient {

  final case class Config(
      baseUri: Uri,
      token: String
  )

  def make[F[_]: Concurrent](client: Client[F], config: Config): InstancePricingClient[F] = new SmartcloudInstancePricingClient(client, config)

  private final class SmartcloudInstancePricingClient[F[_]: Concurrent](
      client: Client[F],
      config: Config
  ) extends InstancePricingClient[F] {

    import InstancePricingClient.Error
    implicit val instancePricingDecoder: EntityDecoder[F, InstancePricing]  = jsonOf
    implicit val instanceKindsDecoder: EntityDecoder[F, List[InstanceKind]] = jsonOf

    val getAllUri  = config.baseUri / "instances"
    val authHeader = Authorization(Credentials.Token(AuthScheme.Bearer, config.token))

    override def getInstancePricing(instanceKind: InstanceKind): F[Either[Error, Option[InstancePricing]]] = {
      val getInstanceUri = config.baseUri / "instances" / instanceKind.getString
      val request: Request[F] = Request(
        Method.GET,
        getInstanceUri,
        headers = Headers(authHeader)
      )
      client
        .expect[InstancePricing](request)
        .map(result => Option(result).asRight[Error])
        .recover {
          case UnexpectedStatus(Status.NotFound, _, _)            => None.asRight[Error]
          case UnexpectedStatus(Status.TooManyRequests, _, _)     => Error.RateLimit.asLeft
          case UnexpectedStatus(Status.InternalServerError, _, _) => Error.ServerError.asLeft
          case UnexpectedStatus(Status.Unauthorized, _, _)        => Error.Unauthorized.asLeft
        }
    }

  }
}
