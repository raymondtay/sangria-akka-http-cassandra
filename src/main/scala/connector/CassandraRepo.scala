import com.datastax.driver.core.querybuilder._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder._
import akka.stream.alpakka.cassandra.scaladsl._

class CassandraRepo(implicit actorSystem : ActorSystem, actorMaterializer : ActorMaterializer) {
  import scala.concurrent._, duration._ 
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val session = Cluster.builder.addContactPoint("127.0.0.1").withPort(9042).build.connect()

  def product(id: String) : Option[Product] = {
    val stmt = new SimpleStatement("select * from my_keyspace.product where id = '" + id + "'") // PreparedStatement ?
    val rows = CassandraSource(stmt).runWith(Sink.seq)
    val reifiedRows = Await.result(rows, 3.second)
    reifiedRows.headOption match {
      case  None ⇒ None
      case Some(row) ⇒ Some(Product(row.get("id", classOf[String]), row.get("name", classOf[String]), row.get("description", classOf[String])))
    }
  }

  def products : List[Product] = {
    val stmt = new SimpleStatement("select * from my_keyspace.product limit 100").setFetchSize(50)
    val rows = CassandraSource(stmt).runWith(Sink.seq)
    val reifiedRows = Await.result(rows, 5.second)
    reifiedRows.map(
          row ⇒ 
          Product(row.get("id", classOf[String]), row.get("name", classOf[String]), row.get("description", classOf[String]))
      ).toList
  }
}
