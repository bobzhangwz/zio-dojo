package com.zhpooer.zio.dojo

import doobie.Transactor
import zio.{Has, Task}

package object utils {
  type HasConnection = Has[Transactor[Task]]
  type TransactionManager = Has[TransactionManager.Service]
}
