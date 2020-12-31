package com.zhpooer.zio.dojo.domain

import com.zhpooer.zio.dojo.repository.{Hello, HelloRepository}
import com.zhpooer.zio.dojo.utils.TransactionManager
import zio.{RIO, Task, ZIO, ZLayer}

object HelloDomainService {
  trait Service {
    def getHello(id: Int): Task[Option[Hello]]
  }

  def getHello(id: Int): RIO[HelloDomainService, Option[Hello]] = {
    ZIO.accessM(_.get.getHello(id))
  }

  val live: ZLayer[HelloRepository with TransactionManager, Nothing, HelloDomainService] = {
    ZLayer.fromServices[HelloRepository.Service, TransactionManager.Service, Service]((repository, txMrg) => {
      new Service {
        override def getHello(id: Int): Task[Option[Hello]] =
          txMrg.runTransaction(repository.getHello(id))
      }
    })
  }
}

