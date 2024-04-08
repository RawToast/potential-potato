package prices.services

import java.time.ZonedDateTime

import cats.effect.IO
import cats.implicits._
import munit._

import prices.client.{ InstancePricing, InstancePricingClient }
import prices.data.{ InstanceKind, InstancePrice }
import prices.routes.protocol.InstancePriceResponse

class SmartcloudInstancePriceServiceSuite extends FunSuite {
  import InstancePriceService.Exception
  import SmartcloudInstancePriceServiceSuite._
  import cats.effect.unsafe.implicits.global

  test("getInstancePricing returns the price of an instance") {
    val instanceKind = InstanceKind("sc2-micro")
    val stubClient   = makeStubClient()
    val service      = SmartcloudInstancePriceService.make[IO](stubClient)

    val resultIO = service.getInstancePricing(InstanceKind("sc2-micro"))
    val result   = resultIO.unsafeRunSync()

    val expected = InstancePriceResponse(instanceKind, InstancePrice(1.0)).some.asRight[Error]
    assert(result == expected)
  }

  test("getInstancePricing returns UnauthorizedCalled if the client responds with an Authorization error") {
    val stubClient = makeStubClient()
    val service    = SmartcloudInstancePriceService.make[IO](stubClient)

    val resultIO = service.getInstancePricing(instanceKindUnauth)
    val result   = resultIO.unsafeRunSync()

    val expected = Left(Exception.UnauthorizedCall)
    assert(result == expected)
  }

  test("getInstancePricing returns APICallFailure if the client responds with a Server error") {
    val stubClient = makeStubClient()
    val service    = SmartcloudInstancePriceService.make[IO](stubClient)

    val resultIO = service.getInstancePricing(instanceKindFail)
    val result   = resultIO.unsafeRunSync()

    val expected = Left(Exception.APICallFailure("API returned an unexpected error"))
    assert(result == expected)
  }

  test("getInstancePricing returns APICallFailure if the client responds with a RateLimit error") {
    val stubClient = makeStubClient()
    val service    = SmartcloudInstancePriceService.make[IO](stubClient)

    val resultIO = service.getInstancePricing(instanceKindLimit)
    val result   = resultIO.unsafeRunSync()

    val expected = Left(Exception.RateLimitExceeded)
    assert(result == expected)
  }
}

object SmartcloudInstancePriceServiceSuite {
  import InstancePricingClient._

  val timestamp = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Asia/Tokyo]")
  val instances = List(InstanceKind("sc2-micro"), InstanceKind("sc2-small"), InstanceKind("sc2-medium"))

  val instanceKindUnauth = InstanceKind("unauth")
  val instanceKindFail   = InstanceKind("boom")
  val instanceKindLimit  = InstanceKind("spam")
  val instanceKindNone   = InstanceKind("none")

  def makeStubClient() = new InstancePricingClient[IO] {
    def getInstancePricing(instanceKind: InstanceKind): IO[Either[Error, Option[InstancePricing]]] =
      instanceKind match {
        case ik if ik == instanceKindUnauth => Error.Unauthorized.asLeft.pure[IO]
        case ik if ik == instanceKindLimit  => Error.RateLimit.asLeft.pure[IO]
        case ik if ik == instanceKindFail   => Error.ServerError.asLeft.pure[IO]
        case ik if ik == instanceKindNone   => Option.empty.asRight.pure[IO]
        case instanceKind                   => InstancePricing(instanceKind, InstancePrice(1.0), timestamp).some.asRight.pure[IO]
      }
  }
}
