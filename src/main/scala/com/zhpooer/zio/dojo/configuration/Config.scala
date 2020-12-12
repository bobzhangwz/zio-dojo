package com.zhpooer.zio.dojo.configuration

case class AppConfig(api: ApiConfig)

case class ApiConfig(endpoint: String, port: Int)

