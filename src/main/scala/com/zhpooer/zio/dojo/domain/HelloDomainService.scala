package com.zhpooer.zio.dojo.domain

import com.zhpooer.zio.dojo.repository
import com.zhpooer.zio.dojo.repository.{Hello, HelloRepository}
import doobie.Transactor
import zio.{Has, RIO, Task}

object HelloDomainService {
  trait Service {
    def getHello(id: Int): RIO[HelloRepository , Option[Hello]]
  }
}

class HelloDomainService(tx: Transactor[Task]) extends HelloDomainService.Service {
  override def getHello(id: Int): RIO[HelloRepository, Option[Hello]] = {
    RIO.accessM[HelloRepository](a =>
      repository.getHello(id).provide( a ++ Has(tx))
    )
  }
}
