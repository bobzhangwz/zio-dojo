package com.zhpooer.zio

import com.zhpooer.zio.dojo.configuration.Configuration
import com.zhpooer.zio.dojo.domain.HelloDomainService
import com.zhpooer.zio.dojo.domain.todo.{TodoApplicationService, TodoRepository}
import com.zhpooer.zio.dojo.repository.HelloRepository
import com.zhpooer.zio.dojo.shared.db.TransactionManager
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger
import zio._

package object dojo {
  type BaseDeps = Blocking with Clock with Logging with Configuration

  type HelloLayer = TransactionManager with HelloRepository with HelloDomainService
  type TodoLayer = TodoRepository with TodoApplicationService

  type AppEnv = BaseDeps with HelloLayer with TodoLayer

  object live {
    val baseDeps: ZLayer[Blocking with Clock, Throwable, BaseDeps] =
      Blocking.any ++ Clock.any ++ Slf4jLogger.makeWithAllAnnotationsAsMdc() ++ Configuration.live

    val serviceLayer: ZLayer[BaseDeps, Throwable, HelloLayer with TodoLayer] =
       ZLayer.requires[BaseDeps] >>> TransactionManager.live >+>
         HelloRepository.live >+> HelloDomainService.live >+>
         TodoRepository.live >+> TodoApplicationService.live

    val allLayer: ZLayer[Blocking with Clock, Throwable, AppEnv] = baseDeps >+> serviceLayer
  }
}
