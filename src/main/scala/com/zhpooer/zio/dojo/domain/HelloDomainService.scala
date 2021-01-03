package com.zhpooer.zio.dojo.domain

import com.zhpooer.zio.dojo.repository.{Hello, HelloRepository}
import com.zhpooer.zio.dojo.shared.db.TransactionManager
import zio.macros.accessible
import zio.{Task, ZLayer}

@accessible
object HelloDomainService {

  trait Service {
    def getHello(id: Int): Task[Option[Hello]]
  }

  val live: ZLayer[HelloRepository with TransactionManager, Nothing, HelloDomainService] =
    ZLayer.fromServices[HelloRepository.Service, TransactionManager.Service, Service] { (repository, txMrg) =>
      new Service {
        override def getHello(id: Int): Task[Option[Hello]] = txMrg.transaction(
          repository.getHello(id)
        )
      }
    }
}
