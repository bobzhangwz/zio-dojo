package com.zhpooer.zio.dojo.service

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import zio.Task
import org.http4s.implicits._
import zio.interop.catz._

object HelloService {
  private val dsl = Http4sDsl[Task]
  import dsl._

  val service = HttpRoutes.of[Task] {
    case GET -> Root => Ok("hello world")
  }.orNotFound
}

