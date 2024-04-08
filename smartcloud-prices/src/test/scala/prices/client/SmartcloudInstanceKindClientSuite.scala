package prices.client

import cats.effect.IO
import io.circe.literal._
import munit.FunSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._

import prices.data.InstanceKind

class SmartcloudInstanceKindClientSuite extends FunSuite {
  val dsl = new Http4sDsl[IO] {}

  import InstanceKindClient.Error
  import cats.effect.unsafe.implicits.global
  import dsl._

  test("returns with instance kinds when calling getAll") {
    val routes = HttpRoutes.of[IO] {
      case GET -> Root / "instances" => Ok(json"""["sc2-micro","sc2-small","sc2-medium"]""")
    }

    val httpApp: HttpApp[IO] = routes.orNotFound
    val client               = Client.fromHttpApp(httpApp)

    val config = SmartcloudInstanceKindClient.Config(
      baseUri = uri"/",
      token = "testing123"
    )
    val smartcloudClient = SmartcloudInstanceKindClient.make(client, config)

    val resultIO = smartcloudClient.getAll()
    val result   = resultIO.unsafeRunSync()

    assertEquals(result, Right(List(InstanceKind("sc2-micro"), InstanceKind("sc2-small"), InstanceKind("sc2-medium"))))
  }

  test("returns ServerError for getAll when an unexpected error is returned") {
    val routes = HttpRoutes.of[IO] {
      case GET -> Root / "instances" => InternalServerError()
    }

    val httpApp: HttpApp[IO] = routes.orNotFound
    val client               = Client.fromHttpApp(httpApp)

    val config = SmartcloudInstanceKindClient.Config(
      baseUri = uri"/",
      token = "testing123"
    )
    val smartcloudClient = SmartcloudInstanceKindClient.make(client, config)

    val result = smartcloudClient.getAll().unsafeRunSync()

    assertEquals(result, Left(Error.ServerError))
  }

  test("returns unauthorised error for getAll when the token is invalid") {
    val routes = HttpRoutes.of[IO] {
      case GET -> Root / "instances" => IO.pure(Response(Status.Unauthorized))
    }

    val httpApp: HttpApp[IO] = routes.orNotFound
    val client               = Client.fromHttpApp(httpApp)

    val config = SmartcloudInstanceKindClient.Config(
      baseUri = uri"/",
      token = "testing123"
    )
    val smartcloudClient = SmartcloudInstanceKindClient.make(client, config)

    val result = smartcloudClient.getAll().unsafeRunSync()

    assertEquals(result, Left(Error.Unauthorized))
  }
}
