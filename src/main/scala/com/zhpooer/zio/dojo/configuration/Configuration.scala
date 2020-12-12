package com.zhpooer.zio.dojo.configuration

import zio.Task
import zio.ZLayer
import ciris._
import zio.interop.catz._

object Configuration {
  trait Service {
    val load: Task[AppConfig]
  }
  val live: ZLayer[Any, Nothing, Configuration] = {
    ZLayer.succeed(new Live {})
  }
}

trait Live extends Configuration.Service {


  val apiConfig: ConfigValue[ApiConfig] =
    for {
      port <- env("API_PORT").as[Int].default(8081)
      endpoint <- env("API_ENDPOINT")
    } yield ApiConfig(endpoint, port)

  val appConfig: ConfigValue[AppConfig] = apiConfig.map(AppConfig)

  override val load: Task[AppConfig] = appConfig.load[Task]
}
