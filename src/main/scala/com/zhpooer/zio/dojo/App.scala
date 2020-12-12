package com.zhpooer.zio.dojo

import zio.ExitCode
import zio.console._
import zio.ZIO

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
  runtime.unsafeRun(ZIO(println("hello world")))
}

