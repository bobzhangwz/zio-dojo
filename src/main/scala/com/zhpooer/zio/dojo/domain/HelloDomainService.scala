package com.zhpooer.zio.dojo.domain

import com.zhpooer.zio.dojo.repository
import com.zhpooer.zio.dojo.repository.{Hello, HelloRepository}
import com.zhpooer.zio.dojo.utils.TransactionManager
import zio.{RIO, ZLayer}

object HelloDomainService {
  trait Service {
    def getHello(id: Int): RIO[HelloRepository with TransactionManager, Option[Hello]]
  }

  val live: ZLayer[Any, Nothing, HelloDomainService] = ZLayer.succeed(new HelloDomainServiceLive)
}

class HelloDomainServiceLive extends HelloDomainService.Service {
  override def getHello(id: Int): RIO[HelloRepository with TransactionManager, Option[Hello]] = {
    TransactionManager.runTransaction[HelloRepository with TransactionManager, Option[Hello]](repository.getHello(id))
  }
}
