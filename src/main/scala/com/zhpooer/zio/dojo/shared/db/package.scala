package com.zhpooer.zio.dojo.shared

import doobie.{ConnectionIO, Transactor}
import zio.{Has, RIO, Task}
import zio.interop.catz._

package object db {
  type HasConnection      = Has[Transactor[Task]]
  type TransactionManager = Has[TransactionManager.Service]
  def exec[A](connIO: => ConnectionIO[A]): RIO[HasConnection, A] =
    for {
      conn   <- RIO.access[HasConnection](_.get)
      result <- conn.trans(implicitly)(connIO)
    } yield result
}
