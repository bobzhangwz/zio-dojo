package com.zhpooer.zio

import com.zhpooer.zio.dojo.configuration.Configuration
import zio.blocking.Blocking
import zio.logging.Logging

package object dojo {
  type BasicDependency = Logging with Configuration with Blocking
}
