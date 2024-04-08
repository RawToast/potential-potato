package prices.services

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe._

import prices.client.InstanceKindClient
import prices.client.InstanceKindClient.Error.ServerError
import prices.client.InstanceKindClient.Error.Unauthorized
import prices.routes.protocol.InstanceKindResponse

object SmartcloudInstanceKindService {
  import InstanceKindService.Exception

  def make[F[_]: Concurrent](client: InstanceKindClient[F]): InstanceKindService[F] = new SmartcloudInstanceKindService(client)

  private final class SmartcloudInstanceKindService[F[_]: Concurrent](
      client: InstanceKindClient[F]
  ) extends InstanceKindService[F] {
    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]

    def mapError(error: InstanceKindClient.Error): Exception = error match {
      case ServerError  => Exception.APICallFailure("API returned an unexpected error")
      case Unauthorized => Exception.Unauthorized
    }
    override def getAll(): F[Either[InstanceKindService.Exception, InstanceKindResponse]] =
      for {
        clientResult <- client.getAll()
        result = clientResult.bimap(mapError, InstanceKindResponse(_))
      } yield result
  }
}
