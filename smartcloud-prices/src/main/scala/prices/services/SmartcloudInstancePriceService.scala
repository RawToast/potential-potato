package prices.services

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe._

import prices.cache.DataCache
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

  def makeWithCache[F[_]: Concurrent](client: InstancePricingClient[F], cache: DataCache[F, InstanceKind, InstancePrice]): InstancePriceService[F] =
    new SmartcloudInstancePriceServiceWithCache(client, cache)

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

  private class SmartcloudInstancePriceServiceWithCache[F[_]: Concurrent](
      client: InstancePricingClient[F],
      cache: DataCache[F, InstanceKind, InstancePrice]
  ) extends InstancePriceService[F] {

    private val underlyingService = new SmartcloudInstancePriceService(client)

    private def fetchAndUpdateCache(instanceKind: InstanceKind): F[Either[Exception, Option[InstancePriceResponse]]] =
      for {
        clientResult <- underlyingService.getInstancePricing(instanceKind)
        _ <- clientResult match {
               case Right(Some(value)) => cache.store(value.kind, value.amount).map(_ => value.some)
               case _                  => clientResult.pure[F]
             }
      } yield clientResult

    override def getInstancePricing(instanceKind: InstanceKind): F[Either[Exception, Option[InstancePriceResponse]]] =
      for {
        cached <- cache.get(instanceKind)
        result <- cached match {
                    case Some(price) => InstancePriceResponse(instanceKind, price).some.asRight.pure[F]
                    case None        => fetchAndUpdateCache(instanceKind)
                  }
      } yield result
  }
}
