package com.zhpooer.zio.dojo

import cats.implicits._
import com.zhpooer.zio.dojo.configuration.Configuration
import com.zhpooer.zio.dojo.live.allLayer
import com.zhpooer.zio.dojo.service.{HelloService, HelloTapirService}
import com.zhpooer.zio.dojo.shared.middleware.TracingMiddleware
import fs2.Stream.Compiler._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.{HttpApp, HttpRoutes}
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.clock.Clock
import zio.interop.catz._
import zio.logging.{Logging, log}

object Main extends zio.App {
  // API_ENDPOINT=localhost bloop run root --main com.zhpooer.zio.dojo.Main
  override def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] = {

    val tapirService                   = new HelloTapirService[AppEnv]()
    val program: RIO[AppEnv, Unit] = for {
      appConfig                             <- ZIO.access[Configuration](_.get)
      _                                     <- log.info(appConfig.toString())
      //    tapirService <- HelloTapirService.service
      routes: HttpRoutes[RIO[AppEnv, *]] =
        new HelloService[AppEnv].service <+>
          new SwaggerHttp4s(tapirService.yaml, "swagger").routes[RIO[AppEnv, *]] <+>
          tapirService.service
      _                                     <- runHttp(routes.orNotFound, appConfig.api.port)
    } yield ()

    program.provideSomeLayer[ZEnv](allLayer).exitCode
  }

  def runHttp[R <: Clock with Logging](
    httpApp: HttpApp[RIO[R, *]],
    port: Int
  ): ZIO[R, Throwable, Unit] = {
    type Task[A] = RIO[R, A]
    ZIO.runtime[R].flatMap { implicit rts =>
      BlazeServerBuilder
        .apply[Task](rts.platform.executor.asEC)
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(CORS(TracingMiddleware(httpApp)))
        .serve
        .compile
        .drain
    }
  }
}
