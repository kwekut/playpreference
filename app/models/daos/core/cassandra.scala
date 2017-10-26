package models.daos.core

import com.datastax.driver.core.{Row, BoundStatement, ResultSet, ResultSetFuture}
import scala.concurrent.{CanAwait, Future, ExecutionContext}
import scala.util.{Success, Try}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.language.implicitConversions

private[core] trait CassandraResultSetOperations {
  private case class ExecutionContextExecutor(executonContext: ExecutionContext) extends java.util.concurrent.Executor {
    def execute(command: Runnable): Unit = { executonContext.execute(command) }
  }

  protected class RichResultSetFuture(resultSetFuture: ResultSetFuture) extends Future[ResultSet] {
    @throws(classOf[InterruptedException])
    @throws(classOf[scala.concurrent.TimeoutException])
    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
      resultSetFuture.get(atMost.toMillis, TimeUnit.MILLISECONDS)
      this
    }

    @throws(classOf[Exception])
    def result(atMost: Duration)(implicit permit: CanAwait): ResultSet = {
      resultSetFuture.get(atMost.toMillis, TimeUnit.MILLISECONDS)
    }

    def onComplete[U](func: (Try[ResultSet]) => U)(implicit executionContext: ExecutionContext): Unit = {
      if (resultSetFuture.isDone) {
        func(Success(resultSetFuture.getUninterruptibly))
      } else {
        resultSetFuture.addListener(new Runnable {
          def run() {
            func(Try(resultSetFuture.get()))
          }
        }, ExecutionContextExecutor(executionContext))
      }
    }

    def isCompleted: Boolean = resultSetFuture.isDone

    def value: Option[Try[ResultSet]] = if (resultSetFuture.isDone) Some(Try(resultSetFuture.get())) else None
  }

  implicit def toFuture(resultSetFuture: ResultSetFuture): Future[ResultSet] = new RichResultSetFuture(resultSetFuture)
}

trait Binder[-A] {

  def bind(value: A, boundStatement: BoundStatement): Unit

}

trait BoundStatementOperations {

  implicit class RichBoundStatement[A : Binder](boundStatement: BoundStatement) {
    val binder = implicitly[Binder[A]]

    def bindFrom(value: A): BoundStatement = {
      binder.bind(value, boundStatement)
      boundStatement
    }
  }

}

object cassandra {

  object resultset extends CassandraResultSetOperations

  object boundstatement extends BoundStatementOperations

}

// //////////////////////////////////////////////////////////////////////////
// //http://www.datastax.com/dev/blog/java-driver-async-queries
//   private def sendQueries(session: Session, query: String, partitionKeys: Array[Any]): List[ResultSetFuture] = {
//     partitionKeys.toList map {partitionKey=> (session.executeAsync(query, partitionKey))}
//   }
//   def queryAll(session: Session, query: String, partitionKeys: AnyRef*): List[ResultSet] = {
//     Future(sendQueries(session, query, partitionKeys)) map {x => x.get()}
//   }

//   def executeFuture(session: Session, query: String, partitionKeys: AnyRef*): List[Future[ResultSet]] = 
//     queryAll(session, query, partitionKeys)

// //executeFuture(session, stmt)


// implicit class RichListenableFuture[T](lf: ListenableFuture[T]) {
//   def asScala: Future[T] = {
//     val p = Promise[T]()
//     Futures.addCallback(lf, new FutureCallback[T] {
//       def onFailure(t: Throwable): Unit = p failure t
//       def onSuccess(result: T): Unit    = p success result
//     })
//     p.future
//   }    
// }
// executor.asyncExecute(query).asScala