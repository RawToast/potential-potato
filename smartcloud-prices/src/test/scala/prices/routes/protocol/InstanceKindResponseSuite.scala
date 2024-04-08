package prices.routes.protocol

import io.circe.literal._
import io.circe.syntax._
import munit._

import prices.data.InstanceKind

class InstanceKindResponseSuite extends FunSuite {
  test("encoder creates expected Json output") {
    val response = InstanceKindResponse(List(InstanceKind("sc2-micro"), InstanceKind("sc2-small")))

    val responseJson = response.asJson

    assertEquals(
      responseJson,
      json"""[{"kind":"sc2-micro"}, {"kind":"sc2-small"}]"""
    )
  }
}
