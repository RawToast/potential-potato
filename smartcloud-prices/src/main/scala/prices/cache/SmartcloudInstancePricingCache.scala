package prices.cache

import scala.collection.immutable.Map

import cats.effect._
import cats.implicits._

import prices.data.InstanceKind
import prices.data.InstancePrice

object SmartCloudPricingCache {
  final case class Config(
      ttl: Long
  )

  def make[F[_]: Concurrent: Clock](config: Config, cacheRef: Ref[F, Map[InstanceKind, CachedPrice]]): DataCache[F, InstanceKind, InstancePrice] =
    new SmartCloudPricingCache[F](config, cacheRef)

  final class SmartCloudPricingCache[F[_]: Concurrent: Clock](
      config: Config,
      cacheRef: Ref[F, Map[InstanceKind, CachedPrice]]
  ) extends DataCache[F, InstanceKind, InstancePrice] {
    private val ttl = config.ttl

    override def get(k: InstanceKind): F[Option[InstancePrice]] =
      for {
        realTime <- Clock[F].realTime
        time = realTime.toMillis
        cache <- cacheRef.get
        price = cache.get(k).mapFilter {
                  case cp @ CachedPrice(price, _) if cp.isLive(time, ttl) => price.some
                  case _                                                  => none
                }
      } yield price

    override def store(key: InstanceKind, instancePrice: InstancePrice): F[Unit] = {
      for {
        realTime <- Clock[F].realTime
        time = realTime.toMillis
        _ <- cacheRef.update(_ + (key -> CachedPrice(instancePrice, time)))
      } yield ()
    }
  }
}
