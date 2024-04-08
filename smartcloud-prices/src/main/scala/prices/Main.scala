package prices

import cats.effect.{ IO, IOApp, Ref }

import prices.cache.CachedPrice
import prices.config.Config
import prices.data.InstanceKind

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    for {
      config <- Config.load[IO]
      cache <- Ref.of[IO, Map[InstanceKind, CachedPrice]](Map.empty)
      server <- Server.serve(config, cache).compile.drain
    } yield server

}
