package prices.services

import java.time.ZonedDateTime

import cats.effect.IO
import cats.implicits._
import munit._

import prices.client.InstanceKindClient
import prices.data.InstanceKind
import prices.routes.protocol.InstanceKindResponse

class SmartcloudInstanceKindServiceSuite extends FunSuite {
  import InstanceKindService.Exception
  import SmartcloudInstanceKindServiceSuite._
  import cats.effect.unsafe.implicits.global

  test("getAll returns the stubbed list of instances") {
    val stubClient = makeStubClient(IO.pure(Right(instances)))
    val service    = SmartcloudInstanceKindService.make[IO](stubClient)

    val resultIO = service.getAll()
    val result   = resultIO.unsafeRunSync()

    val expected = InstanceKindResponse(instances).asRight[Error]
    assert(result == expected)
  }

  test("getAll returns APICallFailure if the client responds with a Server error") {
    val stubClient = makeStubClient(IO.pure(Left(InstanceKindClient.Error.ServerError)))
    val service    = SmartcloudInstanceKindService.make[IO](stubClient)

    val resultIO = service.getAll()
    val result   = resultIO.unsafeRunSync()

    val expected = Left(Exception.APICallFailure("API returned an unexpected error"))
    assert(result == expected)
  }

  test("getAll returns Unauthorized if the client responds with an Authorization error") {
    val stubClient = makeStubClient(IO.pure(Left(InstanceKindClient.Error.Unauthorized)))
    val service    = SmartcloudInstanceKindService.make[IO](stubClient)

    val resultIO = service.getAll()
    val result   = resultIO.unsafeRunSync()

    val expected = Left(Exception.Unauthorized)
    assert(result == expected)
  }
}

object SmartcloudInstanceKindServiceSuite {
  import InstanceKindClient._

  val timestamp        = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Asia/Tokyo]")
  val instances        = List(InstanceKind("sc2-micro"), InstanceKind("sc2-small"), InstanceKind("sc2-medium"))
  val instanceKindFail = InstanceKind("boom")

  def makeStubClient(getAllResponse: IO[Either[Error, List[InstanceKind]]]) = new InstanceKindClient[IO] {
    def getAll(): IO[Either[Error, List[InstanceKind]]] = getAllResponse.map(_.map(_.toList))
  }
}
