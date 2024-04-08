package prices.client

import cats.effect.IO
import io.circe.literal._
import munit.FunSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.typelevel.ci.CIString

import prices.data.InstanceKind

class SmartcloudInstancePricingClientSuite extends FunSuite {
  import InstancePricingClient.Error
  import SmartcloudInstancePricingClientSuite._
  import cats.effect.unsafe.implicits.global

  test("Returns None when the Instance does not exist") {
    val config = SmartcloudInstancePricingClient.Config(
      baseUri = uri"/",
      token = validToken
    )
    val smartcloudClient = SmartcloudInstancePricingClient.make(client, config)

    val result = smartcloudClient.getInstancePricing(InstanceKind("sc-mystery")).unsafeRunSync()

    assertEquals(result, Right(None))
  }

  test("Returns Unauthorized for getInstance when an invalid token is provided") {
    val config = SmartcloudInstancePricingClient.Config(
      baseUri = uri"/",
      token = "badToken"
    )
    val smartcloudClient = SmartcloudInstancePricingClient.make(client, config)

    val result = smartcloudClient.getInstancePricing(InstanceKind("sc-micro")).unsafeRunSync()

    assertEquals(result, Left(Error.Unauthorized))
  }

  test("Returns ServerError when a 500 error is returned") {
    val config = SmartcloudInstancePricingClient.Config(
      baseUri = uri"/",
      token = validToken
    )
    val smartcloudClient = SmartcloudInstancePricingClient.make(client, config)

    val result = smartcloudClient.getInstancePricing(InstanceKind("sc-boom")).unsafeRunSync()

    assertEquals(result, Left(Error.ServerError))
  }

  test("Returns RateError when a 429 error is returned") {
    val config = SmartcloudInstancePricingClient.Config(
      baseUri = uri"/",
      token = validToken
    )
    val smartcloudClient = SmartcloudInstancePricingClient.make(client, config)

    val result = smartcloudClient.getInstancePricing(InstanceKind("sc-many")).unsafeRunSync()

    assertEquals(result, Left(Error.RateLimit))
  }
}

object SmartcloudInstancePricingClientSuite {
  val dsl = new Http4sDsl[IO] {}
  import dsl._

  val authHeader        = CIString.apply("authorized")
  val validToken        = "validToken123"
  private val fullToken = s"Bearer $validToken"

  private def hasAuthToken(req: Request[IO]) = req.headers.headers.contains(Header.Raw(CIString("authorization"), fullToken))

  val routes = HttpRoutes.of[IO] {
    case req @ GET -> Root / "instances" =>
      if (hasAuthToken(req)) Ok(json"""["sc2-micro","sc2-small","sc2-medium"]""")
      else IO.pure(Response(Status.Unauthorized))
    case req @ GET -> Root / "instances" / value =>
      value match {
        case _ if !hasAuthToken(req) => IO.pure(Response(Status.Unauthorized))
        case "sc2-micro" => Ok(json"""{
          "kind": "sc2-micro",
          "price": 0.934,
          "timestamp": "2024-04-02T12:41:38.090Z"
        }""")
        case "sc-boom" => IO.pure(Response(Status.InternalServerError))
        case "sc-many" => IO.pure(Response(Status.TooManyRequests))
        case _         => IO.pure(Response(Status.NotFound))
      }

  }

  val httpApp: HttpApp[IO] = routes.orNotFound
  val client               = Client.fromHttpApp(httpApp)
}
