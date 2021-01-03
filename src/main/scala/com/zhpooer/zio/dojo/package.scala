package com.zhpooer.zio

import com.zhpooer.zio.dojo.configuration.Configuration
import com.zhpooer.zio.dojo.domain.HelloDomainService
import com.zhpooer.zio.dojo.repository.HelloRepository
import com.zhpooer.zio.dojo.shared.db.TransactionManager
import zio.ZLayer
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger

package object dojo {
  type BaseDeps = Blocking with Clock with Logging with Configuration
  type CoreLayer = TransactionManager with HelloRepository with HelloDomainService

  type AppEnv = BaseDeps with CoreLayer

  object live {
    val baseDeps: ZLayer[Blocking with Clock, Throwable, BaseDeps] =
      Blocking.any ++ Clock.any ++ Slf4jLogger.makeWithAllAnnotationsAsMdc() ++ Configuration.live

    val coreLayer: ZLayer[BaseDeps, Throwable, CoreLayer] =
       ZLayer.requires[BaseDeps] >>> TransactionManager.live >+> HelloRepository.live >+> HelloDomainService.live

    val allLayer: ZLayer[Blocking with Clock, Throwable, AppEnv] = baseDeps >+> coreLayer
  }
}
