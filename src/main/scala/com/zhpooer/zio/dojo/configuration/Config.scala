package com.zhpooer.zio.dojo.configuration

case class AppConfig(api: ApiConfig, db: DBConfig)

case class ApiConfig(endpoint: String, port: Int)

case class DBConfig(url: String, driver: String, user: String, password: String)
