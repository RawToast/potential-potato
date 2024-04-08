package prices

import scala.concurrent.duration._

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.Status
import org.http4s.Uri
import org.http4s.client.middleware.{ Retry, RetryPolicy }
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

import prices.cache.{ CachedPrice, SmartCloudPricingCache }
import prices.client.{ SmartcloudInstanceKindClient, SmartcloudInstancePricingClient }
import prices.config.Config
import prices.data.InstanceKind
import prices.routes.{ InstanceKindRoutes, InstancePriceRoutes }
import prices.services.{ SmartcloudInstanceKindService, SmartcloudInstancePriceService }

object Server {

  def serve(config: Config, cacheRef: Ref[IO, Map[InstanceKind, CachedPrice]]): Stream[IO, ExitCode] = {

    val baseUri = Uri.fromString(config.smartcloud.baseUri).getOrElse(throw new IllegalArgumentException("Invalid base url"))

    val kindConfig = SmartcloudInstanceKindClient.Config(
      baseUri,
      token = config.smartcloud.token
    )

    val pricingConfig = SmartcloudInstancePricingClient.Config(
      baseUri,
      token = config.smartcloud.token
    )

    val cacheConfig = SmartCloudPricingCache.Config(config.cache.ttl)

    val retryPolicy = RetryPolicy[IO](
      (_ => Some(1.milli)),
      retriable = (_, response) =>
        response match {
          case Right(response) if response.status == Status.InternalServerError => true
          case Right(_)                                                         => false
          case Left(_)                                                          => true
        }
    )

    val serverResource = for {
      emberClient <- EmberClientBuilder.default[IO].build
      clientWithRetry      = Retry(retryPolicy)(emberClient)
      kindClient           = SmartcloudInstanceKindClient.make[IO](clientWithRetry, kindConfig)
      pricingClient        = SmartcloudInstancePricingClient.make[IO](clientWithRetry, pricingConfig)
      cache                = SmartCloudPricingCache.make[IO](cacheConfig, cacheRef)
      instanceKindService  = SmartcloudInstanceKindService.make[IO](kindClient)
      instancePriceService = SmartcloudInstancePriceService.makeWithCache[IO](pricingClient, cache)
      priceRoutes          = InstancePriceRoutes[IO](instancePriceService).routes
      kindRoutes           = InstanceKindRoutes[IO](instanceKindService).routes
      httpApp              = (priceRoutes <+> kindRoutes).orNotFound
      server <- EmberServerBuilder
                  .default[IO]
                  .withHost(Host.fromString(config.app.host).get)
                  .withPort(Port.fromInt(config.app.port).get)
                  .withHttpApp(Logger.httpApp(true, true)(httpApp))
                  .build
    } yield server

    Stream.eval(serverResource.useForever)
  }

}
