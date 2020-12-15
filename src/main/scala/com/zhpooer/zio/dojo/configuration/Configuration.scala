package com.zhpooer.zio.dojo.configuration

import zio.Task
import zio.ZLayer
import ciris._
import zio.interop.catz._

object Configuration {
  trait Service {
    val load: Task[AppConfig]
  }

  val live: ZLayer[Any, Nothing, Configuration] =
    ZLayer.succeed(new Live(sys.env))
}

class Live(envMap: Map[String, String]) extends Configuration.Service {

  val apiConfig: ConfigValue[ApiConfig] =
    for {
      port <- fromEnv("API_PORT").as[Int].default(8081)
      endpoint <- fromEnv("API_ENDPOINT")
    } yield ApiConfig(endpoint, port)

  val appConfig: ConfigValue[AppConfig] = apiConfig.map(AppConfig)

  override val load: Task[AppConfig] = appConfig.load[Task]

  def fromEnv(name: String): ConfigValue[String] = {
    val key = ConfigKey.env(name)
    envMap.get(name).fold(ConfigValue.missing[String](key))(ConfigValue.loaded[String](key, _))
  }
}
