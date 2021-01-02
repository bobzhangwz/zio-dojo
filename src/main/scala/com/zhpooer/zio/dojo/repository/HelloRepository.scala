package com.zhpooer.zio.dojo.repository

import com.zhpooer.zio.dojo.utils.{ DBConnection, HasConnection }
import doobie.quill.DoobieContext
import io.getquill.SnakeCase
import zio.{ RIO, ZLayer }

case class Hello(id: Int, name: String)

object HelloRepository {

  trait Service {
    def getHello(id: Int): RIO[HasConnection, Option[Hello]]
  }

  val live: ZLayer[Any, Nothing, HelloRepository] = ZLayer.succeed(new HelloRepositoryLive)
}

class HelloRepositoryLive extends HelloRepository.Service {

  val ctx                      = new DoobieContext.Postgres(SnakeCase)
  import ctx._
  implicit val helloSchemaMeta = schemaMeta[Hello]("hello")

  override def getHello(id: Int): RIO[HasConnection, Option[Hello]] =
    DBConnection.exec {
      ctx
        .run(
          query[Hello].filter(_.id == lift(id))
        )
        .map(_.headOption)
    }
}
