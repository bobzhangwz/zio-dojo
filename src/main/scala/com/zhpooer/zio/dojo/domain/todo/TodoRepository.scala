package com.zhpooer.zio.dojo.domain.todo

import com.zhpooer.zio.dojo.shared.db
import com.zhpooer.zio.dojo.shared.db.HasConnection
import doobie.quill.DoobieContext
import io.getquill.SnakeCase
import org.http4s.Uri
import zio.{RIO, ZLayer}
import zio.macros.accessible

@accessible
object TodoRepository {
  trait Service {
    def getAll: RIO[HasConnection, List[Todo]]
  }

  val live: ZLayer[Any, Nothing, TodoRepository] = ZLayer.succeed(new PostgresTodoRepo)
}

class PostgresTodoRepo extends TodoRepository.Service {
  val ctx = new DoobieContext.Postgres(SnakeCase)
  import ctx._

  implicit val todoSchemaMeta = schemaMeta[Todo]("hello", _.order -> "orders")
  implicit val uriToString = MappedEncoding[Uri, String](_.renderString)
  implicit val strToUri = MappedEncoding[String, Uri](s => Uri.fromString(s).getOrElse(Uri()))

  override def getAll: RIO[HasConnection, List[Todo]] = db.exec { ctx.run(query[Todo]) }
}
