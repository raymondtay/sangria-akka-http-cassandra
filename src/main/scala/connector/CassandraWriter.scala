import com.datastax.driver.core.querybuilder._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder._
import akka.stream.alpakka.cassandra.scaladsl._

/** 
  * Assumption here is that the table is created in the keyspace `my_keyspace`
  * prior (where the cassandra is assumed to run locally) and the create table expression is like this:
  *
    CREATE TABLE my_keyspace.product (
    id text PRIMARY KEY,
    description text,
    name text)
  */

// Tip: Its rather expensive to create ActorSystems and its Materializer every
// request.
class CassandraWriter(implicit actorSystem : ActorSystem, actorMaterializer : ActorMaterializer) {
  def run(rows: Int = 1000000) = {
    import scala.concurrent._, duration._ 
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val session = Cluster.builder.addContactPoint("127.0.0.1").withPort(9042).build.connect()
    def preparedStatement = session.prepare("insert into my_keyspace.product(id, name, description) values(?,?,?)")
    def stmtBinder = (product: Product, statement: PreparedStatement) ⇒ statement.bind(product.id, product.name, product.description)
    val source = Source(1 to rows).map(id ⇒ Product(id+"", s"product:$id", s"description: $id"))
    val sink = CassandraSink[Product](parallelism = 4, preparedStatement, stmtBinder)
    val result = source.runWith(sink)
    Await.result(result, 10.seconds)
    println("done")
  }
}


