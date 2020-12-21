package com.zhpooer.zio.dojo.service

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import zio._
import zio.interop.catz._
import zio.logging.Logging

class HelloService[R <: Logging] {
  type HelloTask[A] = RIO[R, A]
  private val dsl = Http4sDsl[HelloTask]
  import dsl._

  val service = HttpRoutes.of[HelloTask] {
    case GET -> Root => Ok("hello world")
  }
}
