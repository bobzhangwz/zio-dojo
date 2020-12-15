package com.zhpooer.zio.dojo

import zio.ExitCode
import zio.console._
import zio.ZIO
import com.zhpooer.zio.dojo.configuration.Configuration
import zio.RIO
import org.http4s.server.blaze.BlazeServerBuilder
import zio.interop.catz._
import zio.Task
import zio.clock.Clock
import zio.interop.catz.implicits._
import com.zhpooer.zio.dojo.service.HelloService

object Application extends zio.App {

  override def run(args: List[String]): zio.URIO[zio.ZEnv,ExitCode] = sayHello.exitCode

  val sayHello =
    for {
      _ <- putStrLn("What's your name: ")
      name = "zhang bo"
      _ <- putStrLn(s"Hello ${name}, welcome to zio!")
    } yield ()
}

object Main extends App {
  val runtime = zio.Runtime.default

  ZIO.runtime[Clock].flatMap { implicit rte: zio.Runtime[Clock] =>
     BlazeServerBuilder.apply[Task](rte.platform.executor.asEC)
       .bindHttp(8080, "localhost")
       .withHttpApp(HelloService.service)
       .serve
       .compile
       .drain
  }

  val program: RIO[Console with Configuration, Unit] = for {
    appConfig <- ZIO.accessM[Configuration](_.get.load)
    _ <- putStrLn(appConfig.toString())
  } yield ()

  runtime.unsafeRun(
    program.provideCustomLayer(Configuration.live)
  )
}

