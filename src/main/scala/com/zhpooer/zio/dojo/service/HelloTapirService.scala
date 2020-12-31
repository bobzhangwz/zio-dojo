package com.zhpooer.zio.dojo.service

import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir._
import sttp.tapir.server.http4s.ztapir._
import cats.implicits._
import com.zhpooer.zio.dojo.domain.HelloDomainService
import zio._
import zio.interop.catz._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import sttp.model.StatusCode
import zio.clock.Clock
import zio.logging.{Logging, log}

class HelloTapirService[R <: Logging with Clock with HelloDomainService] {
  type HelloTask[A] = RIO[R, A]

  val helloZioEndpoint: ZEndpoint[String, String, String] =
    endpoint.get.in("hello_zio").in(query[String]("name")).description("please enter your name")
      .errorOut(stringBody)
      .out(stringBody)

  val helloService = helloZioEndpoint.toRoutes[R] {
    case "error" =>
      log.info("say error") *> IO.fail("some thing wrong")
    case name =>
      for {
        _ <- log.info("say hello")
        _ <- HelloDomainService.getHello(1).mapError(_.toString)
      } yield s"hello $name"
  }

  case class Item(id: Int, name: String)
  sealed trait AppError
  case class InvalidValue(msg: String) extends AppError
  case class SystemError(msg: String) extends AppError

  val getItemEndpoint: ZEndpoint[(Int, String), AppError, Item] =
    endpoint.description("used to get Item")
      .get.in("item" / path[Int]("id")).description("id is required")
      .in(query[String]("name")).description("item name")
      .errorOut(
        oneOf[AppError](
          statusMappingFromMatchType(StatusCode.BadRequest, jsonBody[InvalidValue].description("invalide value")),
          statusMappingFromMatchType(StatusCode.NotFound, jsonBody[SystemError])
        )
      )
      .out(jsonBody[Item])

  def getItem(id: Int, name: String): ZIO[R, AppError, Item] = {
    id match {
      case -1 =>
        IO.fail(InvalidValue("id is -1"))
      case 0 => IO.fail(SystemError("id is 0"))
      case _ => UIO(Item(id, name))
    }
  }

  val itemService = getItemEndpoint.toRoutes {
    (getItem _).tupled
  }

  val service = itemService <+> helloService

  import sttp.tapir.docs.openapi._
  import sttp.tapir.openapi.circe.yaml._
  val yaml =
    List(helloZioEndpoint, getItemEndpoint).toOpenAPI("Our pets", "1.0").toYaml

}
