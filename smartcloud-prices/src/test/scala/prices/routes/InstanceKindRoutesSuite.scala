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

import prices.data.InstanceKind
import prices.services.InstanceKindService

class InstanceKindRoutesSuite extends FunSuite {
  import cats.effect.unsafe.implicits.global

  final class StubInstanceKindService[F[_]: Monad]() extends InstanceKindService[F] {
    override def getAll(): F[List[InstanceKind]] =
      List("sc2-micro", "sc2-small", "sc2-medium")
        .map(InstanceKind(_))
        .pure[F]
  }
  val routes    = InstanceKindRoutes[IO](new StubInstanceKindService())
  val underTest = routes.routes.orNotFound

  test("get all returns the stubbed list of instances") {
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
}
