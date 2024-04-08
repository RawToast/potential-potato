package prices.client

import scala.concurrent.duration.FiniteDuration

import cats.effect.unsafe.implicits.global
import cats.effect.{ IO, Ref, Temporal }
import munit.FunSuite

import prices.cache.CachedPrice
import prices.cache.SmartCloudPricingCache
import prices.data.InstanceKind
import prices.data.InstancePrice

class SmartcloudInstancePricingCacheSuitetest extends FunSuite {

  test("Returns none if no cached data is available") {

    val config = SmartCloudPricingCache.Config(
      ttl = 5000L
    )
    val cacheRef = Ref.unsafe[IO, Map[InstanceKind, CachedPrice]](Map.empty)
    val cache    = SmartCloudPricingCache.make[IO](config, cacheRef)

    val resultIO = cache.get(InstanceKind("sc-micro"))
    val result   = resultIO.unsafeRunSync()

    assertEquals(result, None)
  }

  test("Returns cached data if it exists and is still live") {

    val config = SmartCloudPricingCache.Config(
      ttl = 5000L
    )
    val cacheRef = Ref.unsafe[IO, Map[InstanceKind, CachedPrice]](Map.empty)
    val cache    = SmartCloudPricingCache.make[IO](config, cacheRef)
    val price    = InstancePrice(1.337)

    val result = for {
      _ <- cache.store(InstanceKind("sc-micro"), price)
      data <- cache.get(InstanceKind("sc-micro"))
    } yield data

    assertEquals(result.unsafeRunSync(), Some(price))
  }

  test("Returns none if data has expired") {
    val config = SmartCloudPricingCache.Config(
      ttl = 1L
    )
    val cacheRef = Ref.unsafe[IO, Map[InstanceKind, CachedPrice]](Map.empty)
    val cache    = SmartCloudPricingCache.make[IO](config, cacheRef)
    val price    = InstancePrice(1.337)

    val result = for {
      _ <- cache.store(InstanceKind("sc-micro"), price)
      _ <- Temporal[IO].sleep(FiniteDuration(5, "ms"))
      data <- cache.get(InstanceKind("sc-micro"))
    } yield data

    assertEquals(result.unsafeRunSync(), None)
  }
}
