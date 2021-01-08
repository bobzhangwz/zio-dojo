package com.zhpooer.zio.dojo.domain.todo

import zio.logging.Logging
import sttp.tapir.ztapir._
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import io.circe.{Decoder, Encoder, Json}
import zio._
import io.circe.syntax._
import sttp.tapir.server.http4s.ztapir._
import io.circe.generic.semiauto._
import sttp.tapir.generic.auto._
import org.http4s.Uri
import sttp.tapir.Schema
import sttp.tapir.SchemaType.SString
import zio.interop.catz._

sealed trait AppError

case class SystemError(cause: String) extends AppError

object AppError {
  implicit val encoder: Encoder[SystemError] = Encoder.instance {
    case SystemError(cause) => Json.obj("message" := cause)
  }
  implicit val decoder: Decoder[SystemError] = deriveDecoder
}

object TodoApi {
  import AppError._
  implicit val uriEncoder: Encoder[Uri] =
    Encoder.encoderContravariant.contramap(implicitly[Encoder[String]])(_.toString)
  implicit val uriDecoder: Decoder[Uri] =
    Decoder.decodeString.emap(s => Uri.fromString(s).left.map(_.message))

  implicit val schemaForUri: Schema[Uri] = Schema(SString)
  implicit val schemaForThrowable: Schema[Throwable] = Schema(SString)

  implicit val todoEncoder = deriveEncoder[Todo]
  implicit val todoDecoder = deriveDecoder[Todo]

  val getAll: ZEndpoint[Unit, AppError, List[Todo]] =
    endpoint.get
      .in("todos")
      .errorOut(
        oneOf[AppError](
          statusMappingFromMatchType[SystemError](StatusCode.InternalServerError, jsonBody[SystemError])
        )
      )
      .out(jsonBody[List[Todo]])

  lazy val spec = {
    import sttp.tapir.docs.openapi._
    import sttp.tapir.openapi.circe.yaml._
    List(getAll).toOpenAPI("todo", "1.0").toYaml
  }
}

class TodoController[R <: Logging with TodoApplicationService] {
  type CTask[A] = RIO[R, A]
  val getAll = TodoApi.getAll.toRoutes[R]( _ => {
    TodoApplicationService.getAll.mapError(e => SystemError(e.getMessage))
  })
}

