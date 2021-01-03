package com.zhpooer.zio.dojo.shared.middleware

import cats.data.Kleisli
import org.http4s.{HttpApp, Request}
import zio.{RIO, UIO}
import zio.logging.{ log, LogAnnotation, Logging }

import java.util.UUID

object TracingMiddleware {
  def apply[R <: Logging](service: HttpApp[RIO[R, *]]): HttpApp[RIO[R, *]] =
    Kleisli { req: Request[RIO[R, *]] =>
      for {
        correlationId <- UIO.some(UUID.randomUUID())
        resp          <- log.locally(_.annotate(LogAnnotation.CorrelationId, correlationId)) {
          log.info("starting request") *> service(req)
        }
      } yield resp
    }
}
