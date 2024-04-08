package prices.cache

import prices.data.InstancePrice

case class CachedPrice(
    price: InstancePrice,
    timestamp: Long
) {
  def isLive(time: Long, ttl: Long): Boolean = time - timestamp < ttl
}

trait DataCache[F[_], K, V] {
  def get(k: K): F[Option[V]]

  def store(key: K, value: V): F[Unit]
}
