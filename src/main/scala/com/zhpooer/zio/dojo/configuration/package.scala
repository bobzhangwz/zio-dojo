package com.zhpooer.zio.dojo

package object configuration {
  type Configuration = zio.Has[ApiConfig] with zio.Has[DBConfig]
}
