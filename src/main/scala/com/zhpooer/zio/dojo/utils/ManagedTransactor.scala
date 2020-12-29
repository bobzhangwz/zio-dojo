package com.zhpooer.zio.dojo.utils

import cats.effect.Blocker
import com.zhpooer.zio.dojo.configuration
import com.zhpooer.zio.dojo.configuration.{Configuration, DBConfig}
import doobie.{ConnectionIO, Transactor}
import doobie.free.connection
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Strategy
import zio.interop.catz._
import zio._
import zio.blocking.Blocking

object DBConnection {
  def exec[A](connIO: => ConnectionIO[A]): RIO[HasConnection, A] = {
    for {
      conn <- RIO.access[HasConnection](_.get)
      result <- conn.rawTrans(implicitly)(connIO)
    } yield result
  }
}

object TransactionManager {
  trait Service {
    def runTransaction[R <: Has[_], A](zio: ZIO[R with HasConnection, Throwable, A]): ZIO[R, Throwable, A]
  }

  val live: ZLayer[Configuration with Blocking, Throwable, TransactionManager] =
    ZLayer.fromManaged(
      for {
        dbConfig <- configuration.getDBConfig.toManaged_
        managedTx <- mkTransactor(dbConfig)
      } yield new TransactionManagerLive(managedTx)
    )

  def mkTransactor(
    cfg: DBConfig
  ): ZManaged[Blocking, Throwable, Transactor[Task]] =
    ZIO.runtime[Blocking].toManaged_.flatMap { implicit rt =>
      for {
        transactEC <- Managed.succeed(
          rt.environment
            .get[Blocking.Service]
            .blockingExecutor
            .asEC
        )
        connectEC = rt.platform.executor.asEC
        transactor <- HikariTransactor
          .newHikariTransactor[Task](
            cfg.driver,
            cfg.url,
            cfg.user,
            cfg.password,
            connectEC,
            Blocker.liftExecutionContext(transactEC)
          )
          .toManaged
      } yield transactor
    }
}

class TransactionManagerLive(transactor: Transactor[Task]) extends TransactionManager.Service {

  def runTransaction[R <: Has[_], A](zio: ZIO[R with HasConnection, Throwable, A]): ZIO[R, Throwable, A] = {
    val tx = transactor.copy(strategy0 = Strategy.void)
    ZIO.accessM[R] { r =>
      val trans: ZIO[R with HasConnection, Throwable, A] = for {
        _ <- connection.setAutoCommit(false).transact(tx)
        result <- zio
        _ <- connection.commit.transact(tx)
      } yield result

      trans.provide(r.add(tx))
    }
  }
}
