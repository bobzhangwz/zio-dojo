package com.zhpooer.zio.dojo.utils

import java.sql.Connection

import cats.effect.{ Blocker, Resource }
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import com.zhpooer.zio.dojo.configuration.DBConfig
import com.zhpooer.zio.dojo.{ configuration, BasicDependency }
import doobie.free.connection
import doobie.implicits._
import doobie.util.transactor.Strategy
import doobie.{ ConnectionIO, KleisliInterpreter, Transactor }
import javax.sql.DataSource
import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import zio.logging.Logger

object DBConnection {

  def exec[A](connIO: => ConnectionIO[A]): RIO[HasConnection, A] =
    for {
      conn   <- RIO.access[HasConnection](_.get)
      result <- conn.rawTrans(implicitly)(connIO)
    } yield result
}

object TransactionManager {

  trait Service {
    def inTransactionR[R <: Has[_], A](zio: ZIO[R with HasConnection, Throwable, A]): ZIO[R, Throwable, A]
    def inTransaction[A](zio: ZIO[HasConnection, Throwable, A]): Task[A]
  }

  def runTransactionR[R <: Has[_], A](
    zio: ZIO[R with HasConnection, Throwable, A]
  ): ZIO[R with TransactionManager, Throwable, A] =
    ZIO.accessM[R with TransactionManager] { mrg: TransactionManager => mrg.get.inTransactionR[R, A](zio) }

  val live: ZLayer[BasicDependency, Throwable, TransactionManager] =
    ZLayer.fromManaged(
      for {
        dbConfig       <- configuration.getDBConfig.toManaged_
        dataSource     <- mkDataSource(dbConfig)
        baseDependency <- ZIO.environment[BasicDependency].toManaged_
      } yield new TransactionManagerLive(baseDependency, dataSource)
    )

  def mkDataSource(
    cfg: DBConfig
  ): ZManaged[Any, Throwable, DataSource] = {
    val hikariConfig = {
      val config = new HikariConfig
      config.setJdbcUrl(cfg.url)
      config.setUsername(cfg.user)
      config.setPassword(cfg.password)
      config.addDataSourceProperty("cachePrepStmts", "true")
      config.addDataSourceProperty("prepStmtCacheSize", "250")
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
      config
    }

    Task(Class.forName(cfg.driver)).toManaged_ *>
      ZManaged.makeEffect(new HikariDataSource(hikariConfig))(_.close())
  }
}

class TransactionManagerLive(baseEnv: BasicDependency, dataSource: DataSource) extends TransactionManager.Service {

  lazy val autoTransactor: URIO[Blocking, Transactor[Task]] =
    ZIO.runtime[Blocking].map { rt =>
      val blockerEC = baseEnv.get[Blocking.Service].blockingExecutor.asEC
      val runtimeEC = rt.platform.executor.asEC
      Transactor
        .fromDataSource[Task](dataSource, runtimeEC, Blocker.liftExecutionContext(blockerEC))
    }

  def manuallyMngTransactor(connection: Connection): RIO[Blocking, Transactor[Task]] =
    ZIO.environment[Blocking].map { blocking =>
      val blockerEC = blocking.get[Blocking.Service].blockingExecutor.asEC
      val blocker   = Blocker.liftExecutionContext(blockerEC)
      val connect   = (c: Connection) => Resource.pure[Task, Connection](c)
      val interp    = KleisliInterpreter[Task](blocker).ConnectionInterpreter

      Transactor(connection, connect, interp, Strategy.void)
    }

  def inDB[A](program: ZIO[HasConnection, Throwable, A]): Task[A] = inDBR[Has[Int], A](program).provide(Has(0))

  def inDBR[R <: Has[_], A](program: ZIO[R with HasConnection, Throwable, A]): ZIO[R, Throwable, A] =
    ZIO.accessM[R] { r =>
      for {
        tx     <- autoTransactor.provide(baseEnv)
        result <- program.provide(r.add(tx))
      } yield result
    }

  def inTransaction[A](program: ZIO[HasConnection, Throwable, A]): Task[A] =
    inTransactionR[Has[Int], A](program).provide(Has(0))

  def inTransactionR[R <: Has[_], A](program: ZIO[R with HasConnection, Throwable, A]): ZIO[R, Throwable, A] =
    ZIO.accessM[R] { r =>
      ZIO.bracket(Task(dataSource.getConnection()))(c => Task(c.close()).orDie) { conn =>
        for {
          tx     <- manuallyMngTransactor(conn).provide(baseEnv)
          _      <- connection.setAutoCommit(false).transact(tx)
          result <- program.provide(r.add(tx)).onExit {
                      case Exit.Failure(e) =>
                        baseEnv.get[Logger[String]].error(e.toString) *>
                          connection.rollback.transact(tx).orDie
                      case Exit.Success(_) => connection.commit.transact(tx).orDie
                    }
        } yield result
      }
    }
}
