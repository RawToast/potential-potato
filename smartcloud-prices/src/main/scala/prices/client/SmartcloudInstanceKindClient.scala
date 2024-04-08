package prices.client

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.UnexpectedStatus
import org.http4s.headers.Authorization

import prices.data._

object SmartcloudInstanceKindClient {

  final case class Config(
      baseUri: Uri,
      token: String
  )

  def make[F[_]: Concurrent](client: Client[F], config: Config): InstanceKindClient[F] = new SmartcloudInstanceKindClient(client, config)

  private final class SmartcloudInstanceKindClient[F[_]: Concurrent](
      client: Client[F],
      config: Config
  ) extends InstanceKindClient[F] {

    import InstanceKindClient.Error
    implicit val instanceKindsDecoder: EntityDecoder[F, List[InstanceKind]] = jsonOf

    val getAllUri  = config.baseUri / "instances"
    val authHeader = Authorization(Credentials.Token(AuthScheme.Bearer, config.token))

    private val getAllRequest: Request[F] = Request(
      Method.GET,
      getAllUri,
      headers = Headers(authHeader)
    )

    override def getAll(): F[Either[Error, List[InstanceKind]]] = {
      client
        .expect[List[InstanceKind]](getAllRequest)
        .map(_.asRight[Error])
        .recover {
          case UnexpectedStatus(Status.Unauthorized, _, _)        => Error.Unauthorized.asLeft
          case UnexpectedStatus(Status.InternalServerError, _, _) => Error.ServerError.asLeft
        }
    }
  }
}
