package com.zhpooer.zio.dojo

import com.zhpooer.zio.dojo.utils.HasConnection
import zio.{ Has, RIO, ZIO }

package object repository {
  type HelloRepository = Has[HelloRepository.Service]

  def getHello(id: Int): RIO[HelloRepository with HasConnection, Option[Hello]] = ZIO.accessM(_.get.getHello(id))
}
