package com.zhpooer.zio.dojo.configuration

import pureconfig.ConfigSource
import zio.Task
import zio.ZLayer
import pureconfig.generic.auto._

object Configuration {
  trait Service {
    val load: Task[Config]
  }
  val live: ZLayer[Any, Nothing, Configuration] = {
    ZLayer.succeed(new Live {})
  }
}

trait Live extends Configuration.Service {
  val load: Task[Config] =
    Task.effect(ConfigSource.default.loadOrThrow[Config])
}
