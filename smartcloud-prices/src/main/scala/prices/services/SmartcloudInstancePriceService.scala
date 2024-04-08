package prices.services

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe._

import prices.client.InstancePricingClient
import prices.client.InstancePricingClient.Error.{ RateLimit, ServerError, Unauthorized }
import prices.data._
import prices.routes.protocol.InstancePriceResponse

object SmartcloudInstancePriceService {
  import InstancePriceService.Exception
  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Concurrent](client: InstancePricingClient[F]): InstancePriceService[F] = new SmartcloudInstancePriceService(client)

  private final class SmartcloudInstancePriceService[F[_]: Concurrent](
      client: InstancePricingClient[F]
  ) extends InstancePriceService[F] {

    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]

    override def getInstancePricing(instanceKind: InstanceKind): F[Either[Exception, Option[InstancePriceResponse]]] =
      for {
        clientResult <- client.getInstancePricing(instanceKind)
        result = clientResult match {
                   case Right(Some(value)) => InstancePriceResponse(value.kind, value.price).some.asRight
                   case Right(None)        => Option.empty[InstancePriceResponse].asRight
                   case Left(value) =>
                     value match {
                       case RateLimit    => Exception.RateLimitExceeded.asLeft
                       case ServerError  => Exception.APICallFailure("API returned an unexpected error").asLeft
                       case Unauthorized => Exception.UnauthorizedCall.asLeft
                     }
                 }
      } yield result
  }
}
