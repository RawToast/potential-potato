package prices

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.Uri
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

import prices.client.{ SmartcloudInstanceKindClient, SmartcloudInstancePricingClient }
import prices.config.Config
import prices.routes.{ InstanceKindRoutes, InstancePriceRoutes }
import prices.services.{ SmartcloudInstanceKindService, SmartcloudInstancePriceService }

object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    val baseUri = Uri.fromString(config.smartcloud.baseUri).getOrElse(throw new IllegalArgumentException("Invalid base url"))

    val kindConfig = SmartcloudInstanceKindClient.Config(
      baseUri,
      token = config.smartcloud.token
    )

    val pricingConfig = SmartcloudInstancePricingClient.Config(
      baseUri,
      token = config.smartcloud.token
    )

    val serverResource = for {
      emberClient <- EmberClientBuilder.default[IO].build
      kindClient           = SmartcloudInstanceKindClient.make[IO](emberClient, kindConfig)
      pricingClient        = SmartcloudInstancePricingClient.make[IO](emberClient, pricingConfig)
      instanceKindService  = SmartcloudInstanceKindService.make[IO](kindClient)
      instancePriceService = SmartcloudInstancePriceService.make[IO](pricingClient)
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
