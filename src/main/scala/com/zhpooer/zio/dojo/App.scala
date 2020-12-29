package com.zhpooer.zio.dojo

import cats.data.Kleisli
import zio.{ExitCode, RIO, UIO, ZIO}
import zio.console._
import com.zhpooer.zio.dojo.configuration.Configuration
import zio.interop.catz._
import zio.clock.Clock
import fs2.Stream.Compiler._
import com.zhpooer.zio.dojo.service.{HelloService, HelloTapirService}
import org.http4s.implicits._
import cats.implicits._
import com.zhpooer.zio.dojo.domain.HelloDomainService
import com.zhpooer.zio.dojo.repository.HelloRepository
import com.zhpooer.zio.dojo.utils.TransactionManager
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.{HttpApp, HttpRoutes, Request}
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.blocking.Blocking
import zio.logging.slf4j.Slf4jLogger
import zio.logging.{LogAnnotation, Logging, log}

import java.util.UUID

object Application extends zio.App {
  override def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] = sayHello.exitCode

  val sayHello =
    for {
      _ <- putStrLn("What's your name: ")
      name = "zhang bo"
      _ <- putStrLn(s"Hello ${name}, welcome to zio!")
    } yield ()
}

object Main extends App {
  // API_ENDPOINT=localhost bloop run root --main com.zhpooer.zio.dojo.Main
  val runtime = zio.Runtime.default

  type Dependency = Blocking with Clock with Console with
    Configuration with Logging with HelloRepository with TransactionManager with HelloDomainService

  def correlationIdMidware[R <: Logging](service: HttpApp[RIO[R, *]]): HttpApp[RIO[R, *]] =
    Kleisli { req: Request[RIO[R, *]] =>
      for {
        correlationId <- UIO.some(UUID.randomUUID())
        resp <- log.locally(_.annotate(LogAnnotation.CorrelationId, correlationId)) {
          log.info("starting request") *> service(req)
        }
      } yield resp
    }

  val tapirService = new HelloTapirService[Dependency]()
  val program: RIO[Dependency, Unit] = for {
    appConfig <- ZIO.access[Configuration](_.get)
    _ <- putStrLn(appConfig.toString())
//    tapirService <- HelloTapirService.service
    routes: HttpRoutes[RIO[Dependency, *]] = new HelloService[Dependency].service <+>
      new SwaggerHttp4s(tapirService.yaml, "swagger").routes[RIO[Dependency, *]] <+>
        tapirService.service
    _ <- runHttp(routes.orNotFound, appConfig.api.port)
  } yield ()

  runtime.unsafeRun(
    program.provideCustomLayer(
      Blocking.live >+> Configuration.live >+> Slf4jLogger.makeWithAllAnnotationsAsMdc() >+>
        HelloRepository.live >+> TransactionManager.live >+> HelloDomainService.live
    )
  )

  def runHttp[R <: Clock with Logging](
    httpApp: HttpApp[RIO[R, *]],
    port: Int
  ): ZIO[R, Throwable, Unit] = {
    type Task[A] = RIO[R, A]
    ZIO.runtime[R].flatMap { implicit rts =>
      BlazeServerBuilder
        .apply[Task](rts.platform.executor.asEC)
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(CORS(correlationIdMidware(httpApp)))
        .serve
        .compile
        .drain
    }
  }

}
