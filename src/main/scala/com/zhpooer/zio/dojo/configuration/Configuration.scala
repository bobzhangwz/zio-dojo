package com.zhpooer.zio.dojo.configuration

import zio.Task
import zio.ZLayer
import ciris._
import zio.interop.catz._
import cats.implicits._

object Configuration {

  val live: ZLayer[Any, Throwable, Configuration] =
    ZLayer.fromEffect(new ConfigLoader(sys.env).load)
}

class ConfigLoader(envMap: Map[String, String]) {

  val apiConfig: ConfigValue[ApiConfig] =
    for {
      port <- fromEnv("API_PORT").as[Int].default(8081)
      endpoint <- fromEnv("API_ENDPOINT")
    } yield ApiConfig(endpoint, port)

  val dbConfig: ConfigValue[DBConfig] = {
    for {
      url <- fromEnv("DB_URL").default("")
      driver <- fromEnv("DB_URL").default("")
      user <- fromEnv("DB_URL").default("")
      password <- fromEnv("DB_URL").default("")
    } yield DBConfig(url, driver, user, password)
  }
  val appConfig: ConfigValue[AppConfig] =
    (apiConfig, dbConfig).mapN(AppConfig.apply)

  val load: Task[AppConfig] = appConfig.load[Task]

  def fromEnv(name: String): ConfigValue[String] = {
    val key = ConfigKey.env(name)
    envMap.get(name).fold(ConfigValue.missing[String](key))(ConfigValue.loaded[String](key, _))
  }
}
