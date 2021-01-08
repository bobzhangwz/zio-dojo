package com.zhpooer.zio.dojo.domain.todo

import com.zhpooer.zio.dojo.shared.db.TransactionManager
import zio.macros.accessible
import zio.{Task, ZLayer}

@accessible
object TodoApplicationService {
  trait Service {
    def getAll: Task[List[Todo]]
  }

  val live: ZLayer[TodoRepository with TransactionManager, Nothing, TodoApplicationService] =
    ZLayer.fromServices[TodoRepository.Service, TransactionManager.Service, Service] { (repo, txMrg) =>
      new Service {
        override def getAll: Task[List[Todo]] = txMrg.noTransaction {
          repo.getAll
        }
      }
    }
}
