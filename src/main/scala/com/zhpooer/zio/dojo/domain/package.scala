package com.zhpooer.zio.dojo

import zio.Has

package object domain {
  type HelloDomainService = Has[HelloDomainService.Service]
}
