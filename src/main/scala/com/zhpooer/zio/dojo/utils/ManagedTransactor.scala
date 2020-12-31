package com.zhpooer.zio.dojo.utils

import cats.effect.Blocker
import com.zhpooer.zio.dojo.configuration
import com.zhpooer.zio.dojo.configuration.{Configuration, DBConfig}
import doobie.free.connection
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Strategy
import doobie.{ConnectionIO, Transactor}
import zio._
import zio.blocking.Blocking
import zio.interop.catz._

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

  val live: ZLayer[Configuration with Blocking, Throwable, TransactionManager] = {
    ZLayer.fromManaged(
      for {
        dbConfig <- configuration.getDBConfig.toManaged_
        managedTx <- mkTransactor(dbConfig)
      } yield new TransactionManagerLive(managedTx)
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

class TransactionManagerLive(transactor: Transactor[Task]) extends TransactionManager.Service {
  val tx: Transactor[Task] = transactor.copy(strategy0 = Strategy.void)
  def runTransaction[A](zio: ZIO[HasConnection, Throwable, A]): Task[A] = {
    val trans = for {
      _ <- connection.setAutoCommit(false).transact(tx)
      result <- zio.onExit {
        case Exit.Failure(_) => connection.rollback.transact(tx).either
        case Exit.Success(_) => connection.commit.transact(tx).eventually
      }
    } yield result
    trans.provide(Has(tx))
  }

  def runTransactionR[R <: Has[_], A](zio: ZIO[R with HasConnection, Throwable, A]): ZIO[R, Throwable, A] = {

    ZIO.accessM[R] { r =>
      val trans: ZIO[R with HasConnection, Throwable, A] = for {
        _ <- connection.setAutoCommit(false).transact(tx)
        result <- zio.onExit {
          case Exit.Failure(_) =>
            connection.rollback.transact(tx).orDie
          case Exit.Success(_) =>
            connection.commit.transact(tx).orDie
        }
      } yield result

      trans.provide(r.add(tx))
    }
  }
}
