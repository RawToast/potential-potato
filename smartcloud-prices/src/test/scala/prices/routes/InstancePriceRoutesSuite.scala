package prices.routes

import cats.Monad
import cats.effect._
import cats.implicits._
import io.circe.Json
import io.circe.literal._
import munit._
import org.http4s.Method.GET
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._

import prices.data._
import prices.routes.protocol.InstancePriceResponse
import prices.services.InstancePriceService
import prices.services.InstancePriceService.Exception

class InstancePriceRoutesSuite extends FunSuite {
  import InstancePriceRoutesSuite._
  import cats.effect.unsafe.implicits.global

  val routes    = InstancePriceRoutes[IO](new StubInstancePriceService())
  val underTest = routes.routes.orNotFound

  test("get price returns the stubbed prices") {
    val request = Request[IO](GET, uri"/prices?kind=sc2-micro")

    val response: IO[Response[IO]] = underTest.run(request)
    val actualResponse             = response.unsafeRunSync()

    assertEquals(actualResponse.status, Ok)

    val responseJson = actualResponse.as[Json].unsafeRunSync()

    assertEquals(
      responseJson,
      json"""{"kind":"sc2-micro", "amount":36.9}"""
    )
  }

  test("returns 404 response when the client requests the price for a kind that does not exist") {
    val request = Request[IO](GET, uri"/prices?kind=none")

    val response: IO[Response[IO]] = underTest.run(request)
    val actualResponse             = response.unsafeRunSync()

    assertEquals(actualResponse.status, NotFound)
  }

  test("returns 401 response when the client returns with authorization failure") {
    val request = Request[IO](GET, uri"/prices?kind=unauth")

    val response: IO[Response[IO]] = underTest.run(request)
    val actualResponse             = response.unsafeRunSync()

    assertEquals(actualResponse.status, Unauthorized)
  }

  test("returns 429 response when the client returns with a RateLimitExceeded failure") {
    val request = Request[IO](GET, uri"/prices?kind=spam")

    val response: IO[Response[IO]] = underTest.run(request)
    val actualResponse             = response.unsafeRunSync()

    assertEquals(actualResponse.status, TooManyRequests)
  }

  test("returns 500 response when the client returns with an API Call failure") {
    val request = Request[IO](GET, uri"/prices?kind=boom")

    val response: IO[Response[IO]] = underTest.run(request)
    val actualResponse             = response.unsafeRunSync()

    assertEquals(actualResponse.status, InternalServerError)
  }
}

object InstancePriceRoutesSuite {
  val instanceKindUnauth = InstanceKind("unauth")
  val instanceKindFail   = InstanceKind("boom")
  val instanceKindLimit  = InstanceKind("spam")
  val instanceKindNone   = InstanceKind("none")

  final class StubInstancePriceService[F[_]: Monad]() extends InstancePriceService[F] {
    val unauthError =
      Either.left[Exception, Option[InstancePriceResponse]](Exception.UnauthorizedCall).pure[F]

    val callFailError: F[Either[Exception, Option[InstancePriceResponse]]] =
      Either.left[Exception, Option[InstancePriceResponse]](Exception.APICallFailure("boom")).pure[F]

    def getInstancePricing(instanceKind: InstanceKind): F[Either[Exception, Option[InstancePriceResponse]]] =
      instanceKind match {
        case ik if ik == instanceKindNone => Option.empty[InstancePriceResponse].asRight[Exception].pure[F]
        case ik if ik == instanceKindFail =>
          Either.left[Exception, Option[InstancePriceResponse]](Exception.APICallFailure("boom")).pure[F]
        case ik if ik == instanceKindUnauth => unauthError
        case ik if ik == instanceKindLimit =>
          Either.left[Exception, Option[InstancePriceResponse]](Exception.RateLimitExceeded).pure[F]
        case ik => InstancePriceResponse(ik, InstancePrice(36.9)).some.asRight[Exception].pure[F]
      }
  }
}
