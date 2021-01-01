package com.zhpooer.zio.dojo.utils

import cats.effect.Blocker
import com.zhpooer.zio.dojo.configuration.DBConfig
import com.zhpooer.zio.dojo.{BasicDependency, configuration}
import doobie.free.connection
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Strategy
import doobie.{ConnectionIO, Transactor}
import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import zio.logging.Logger

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
    def runTransactionR[R <: Has[_], A](zio: ZIO[R with HasConnection, Throwable, A]): ZIO[R, Throwable, A]
    def runTransaction[A](zio: ZIO[HasConnection, Throwable, A]): Task[A]
  }

  def runTransactionR[R <: Has[_], A](zio: ZIO[R with HasConnection, Throwable, A]): ZIO[R with TransactionManager, Throwable, A] = {
    ZIO.accessM[R with TransactionManager] { mrg: TransactionManager =>
      mrg.get.runTransactionR[R, A](zio)
    }
  }

  val live: ZLayer[BasicDependency, Throwable, TransactionManager] = {
    ZLayer.fromManaged(
      for {
        dbConfig <- configuration.getDBConfig.toManaged_
        managedTx <- mkTransactor(dbConfig)
        baseDependency <- ZIO.environment[BasicDependency].toManaged_
      } yield new TransactionManagerLive(baseDependency, managedTx)
    )
  }

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

class TransactionManagerLive(baseEnv: BasicDependency, transactor: Transactor[Task]) extends TransactionManager.Service {
  val tx: Transactor[Task] = transactor.copy(strategy0 = Strategy.void)

  def exec[R <: HasConnection, A](block: ZIO[R, Throwable, A]): ZIO[R, Throwable, A] = {
    for {
      _ <- connection.setAutoCommit(false).transact(tx)
      result <- block.onExit {
        case Exit.Failure(e) =>
          baseEnv.get[Logger[String]].error(e.toString) *>
            connection.rollback.transact(tx).orDie
        case Exit.Success(_) => connection.commit.transact(tx).orDie
      }
    } yield result
  }
  def runTransaction[A](zio: ZIO[HasConnection, Throwable, A]): Task[A] = {
    exec(zio).provide(Has(tx))
  }

  def runTransactionR[R <: Has[_], A](zio: ZIO[R with HasConnection, Throwable, A]): ZIO[R, Throwable, A] = {
    ZIO.accessM[R] { r =>
      exec(zio).provide(r.add(tx))
    }
  }
}
