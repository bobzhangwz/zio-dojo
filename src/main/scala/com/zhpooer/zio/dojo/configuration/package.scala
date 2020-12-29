package com.zhpooer.zio.dojo

import zio.ZIO

package object configuration {
  type Configuration = zio.Has[AppConfig]

  def getDBConfig: ZIO[Configuration, Throwable, DBConfig] = ZIO.access(_.get.db)
}
