package prices.routes.protocol

import io.circe.literal._
import io.circe.syntax._
import munit._

import prices.data.{ InstanceKind, InstancePrice }

class InstancePriceResponseSuite extends FunSuite {
  test("encoder creates expected Json output") {
    val response = InstancePriceResponse(
      InstanceKind("sc2-micro"),
      InstancePrice(0.1337)
    )

    val responseJson = response.asJson

    assertEquals(
      responseJson,
      json"""{"kind":"sc2-micro", "amount":0.1337}"""
    )
  }
}
