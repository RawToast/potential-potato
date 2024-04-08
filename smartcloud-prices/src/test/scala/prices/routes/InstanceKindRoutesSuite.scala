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
import prices.routes.protocol.InstanceKindResponse
import prices.services.InstanceKindService
import prices.services.InstanceKindService.Exception

class InstanceKindRoutesTest extends FunSuite {
  import InstanceKindRoutesTest._
  import cats.effect.unsafe.implicits.global

  test("get all returns the stubbed list of instances") {
    val routes    = InstanceKindRoutes[IO](new StubInstanceKindService())
    val underTest = routes.routes.orNotFound

    val request = Request[IO](GET, uri"/instance-kinds")

    val response: IO[Response[IO]] = underTest.run(request)
    val actualResponse             = response.unsafeRunSync()

    assertEquals(actualResponse.status, Ok)

    val responseJson = actualResponse.as[Json].unsafeRunSync()

    assertEquals(
      responseJson,
      json"""[{"kind":"sc2-micro"},{"kind":"sc2-small"},{"kind":"sc2-medium"}]"""
    )
  }

  test("returns 401 response when the service returns Unauthorized") {
    val request   = Request[IO](GET, uri"/instance-kinds")
    val routes    = InstanceKindRoutes[IO](new UnauthorizedInstanceKindService())
    val underTest = routes.routes.orNotFound

    val response: IO[Response[IO]] = underTest.run(request)
    val actualResponse             = response.unsafeRunSync()

    assertEquals(actualResponse.status, Unauthorized)
  }

  test("returns 500 response when the service returns an APICallFailure") {
    val request   = Request[IO](GET, uri"/instance-kinds")
    val routes    = InstanceKindRoutes[IO](new ErrorProneInstanceKindService())
    val underTest = routes.routes.orNotFound

    val response: IO[Response[IO]] = underTest.run(request)
    val actualResponse             = response.unsafeRunSync()

    assertEquals(actualResponse.status, InternalServerError)
  }
}

object InstanceKindRoutesTest {
  final class StubInstanceKindService[F[_]: Monad]() extends InstanceKindService[F] {
    def getAll(): F[Either[InstanceKindService.Exception, InstanceKindResponse]] =
      List("sc2-micro", "sc2-small", "sc2-medium")
        .map(InstanceKind(_))
        .asRight[InstanceKindService.Exception]
        .map(InstanceKindResponse(_))
        .pure[F]
  }

  final class ErrorProneInstanceKindService[F[_]: Monad]() extends InstanceKindService[F] {
    def getAll(): F[Either[Exception, InstanceKindResponse]] = {
      val error: Either[Exception, InstanceKindResponse] = Left(Exception.APICallFailure("boom"))
      error.pure[F]
    }
  }

  final class UnauthorizedInstanceKindService[F[_]: Monad]() extends InstanceKindService[F] {
    def getAll(): F[Either[Exception, InstanceKindResponse]] = {
      val error: Either[Exception, InstanceKindResponse] = Left(Exception.Unauthorized)
      error.pure[F]
    }
  }
}
