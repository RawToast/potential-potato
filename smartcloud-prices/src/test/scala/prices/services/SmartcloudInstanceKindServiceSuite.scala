package prices.services

import cats.effect.IO
import munit._

import prices.data.InstanceKind

class SmartcloudInstanceKindServiceSuite extends FunSuite {
  import cats.effect.unsafe.implicits.global

  test("get all returns the stubbed list of instances") {
    val config  = SmartcloudInstanceKindService.Config("http://localhost:1234", "TestToken")
    val service = SmartcloudInstanceKindService.make[IO](config)

    val resultIO = service.getAll()
    val result   = resultIO.unsafeRunSync()

    assert(result == List(InstanceKind("sc2-micro"), InstanceKind("sc2-small"), InstanceKind("sc2-medium")))
  }
}
